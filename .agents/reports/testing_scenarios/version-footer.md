# Footer version display — testing scenario

**Key:** version-footer (feature)
**Scope:** the deployed-version display in the application footer — the version literal with its Changelog link, the build-injected short commit hash in brackets, the build chain that injects it, and the release workflow's interaction with the footer markup. Driven by ARUP-CAS/aiscr-digiarchiv-2#1113.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — the footer is **client-rendered**: the served `index.html` is the CSR shell (the Maven build renames `index.csr.html` to `index.html`), so the footer markup is absent from the server HTML and present only in the compiled main bundle.
- Production: `https://digiarchiv.aiscr.cz/` — not compared in verifications of unreleased milestone work; the comparison becomes meaningful once the feature is released there.
- The main bundle name is content-hashed (`main-<hash>.js`) and changes per build: resolve the current name from the homepage `<script>` tags each run; never embed it in a durable expectation.
- A rollout in flight can briefly serve a homepage whose referenced bundle returns 404 (observed 2026-09-10 between two builds minutes apart): re-read the homepage and retry the bundle fetch before concluding anything is broken.

### Architecture and implementation facts

- Injection chain: `web/src/main/ng/package.json` `build` = `node git-version.js && ng build`. `git-version.js` runs `gitDescribeSync()` **first** (so the `dirty` flag reflects the tree before the file is rewritten), stamps `date`, and writes `git-version.json` in the ng root. `src/version-info.ts` `require`s it, falling back to `{ hash: 'dev', date: Date.now() }` when the file is absent (dev tree without a prior build). `FooterComponent.ngOnInit` assigns `clientInfo = versionInfo`.
- Footer markup (`web/src/main/ng/src/app/components/footer/footer.component.html`): the version literal `v4.1.0` (hand-maintained, links to the wiki Changelog anchor `#v410`) followed by a non-breaking space and the bracketed build hash, guarded by `@if (clientInfo)`. Inside the guard, the `g` prefix from git-describe is stripped (`@let commitHash = clientInfo.hash.startsWith('g') ? clientInfo.hash.slice(1) : clientInfo.hash`); a `dev` fallback renders as a plain span `(dev)`; every other value renders as a link to `https://github.com/ARUP-CAS/aiscr-digiarchiv-2/commit/<commitHash>`.
- git-describe hash format: the `hash` field carries the `g` prefix (`g3c4ee627`), which is not a resolvable commit ref on GitHub — the footer strips it before displaying and linking; the commit short hash is the value without the `g`.
- `web/src/main/ng/git-version.json` is a build artifact and is **untracked**: the repository root `.gitignore` carries `web/src/main/ng/git-version.json`, so a rebuild no longer dirties the tree and no stale copy is committed (fixed via commits `0992c92` and `54ab8b2` after version-footer-D03).
- Maven wiring: `web/pom.xml` runs `npm run build` at `generate-sources` via exec-maven-plugin (skippable with `-DskipNg=true`), so a standard WAR build regenerates the hash. A direct `ng build`, `npm run watch`, or `-DskipNg` build does **not** regenerate it and ships whatever `git-version.json` is present.
- Release workflow interaction (`.github/workflows/new-version.yml`): the release job rewrites exactly one `wiki/Changelog#` anchor and the visible `vX.Y.Z` in the footer (plus `CITATION.cff`), then commits and tags. The hash link is an `[href]` binding, not a literal `wiki/Changelog#` anchor, so the workflow's `CHANGELOG_A` regex still finds exactly one match and the release automation is unaffected; the version literal remains maintained by the release workflow, not by `git-version`.
- The deployed index page is produced from `index.csr.html`; SSR is a separate surface and does not render the footer.
- Test deployments are routinely built from a local working tree (maintainer-confirmed 2026-09-10), so an embedded `dirty:true` is the expected build identity for the test instance, not an anomaly.

### Feature or entity model

- The displayed version has two independent sources: the **literal** (`v4.1.0`, release-workflow-maintained) and the **build identity** (`git-version.json`: tag, distance, hash, dirty, date — build-time-maintained). They can disagree by design during milestone development (a v4.1.0-dev build still describes from tag `v4.0.3`).

### Discovery recipes

1. Embedded version info of the deployed build: fetch the homepage, extract the `main-*.js` script src, fetch the bundle, and locate the `gitDescribe`-shaped object (search the body for `raw:"v` or `hash:"g`). Its `dirty`/`raw`/`hash`/`distance`/`tag`/`semverString`/`date` fields identify the exact build event.
2. Compiled footer template: in the same bundle, search `wiki/Changelog#` (version-link attributes), `clientInfo.hash` (the bracketed interpolation and the `startsWith("g")` strip), and `commit/` (the link-target construction) to confirm the rendered structure and both hrefs without a browser.
3. Build vs. history comparison: compare the embedded `raw` (tag-distance-hash-dirty) against the repository's dev commit list. The hash names the build's base commit; `dirty:true` means the build also carried uncommitted changes (expected local-build practice for this instance).
4. Repository-side state: read `web/src/main/ng/git-version.json` in the working tree (untracked, regenerated by the last local build) and the commit history for the implementation commits.

### Verification commands

Identifier-free, re-runnable against the test instance (rate-limit gently; no credentials needed — the footer is anonymous):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/"
# extract the main-<hash>.js src from the <script> tags, then:
curl.exe -s -o <scratch>/main.js "https://digiarchiv-test.aiscr.cz/main-<hash>.js"
# search the body for: wiki/Changelog#   clientInfo.hash   commit/   raw:"v...-g...-dirty
# when the bundle 404s, re-read the homepage — a rollout may be in flight
```

## Current verification (2026-09-10, digiarchiv-test.aiscr.cz, main bundle main-O3EFC64P.js, embedded build v4.0.3-210-g3c4ee627-dirty of 2026-09-10T13:23:47Z, anonymous session + maintainer-confirmed browser check)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Footer version literal `Verze v4.1.0` with Changelog wiki anchor link | verified (compiled template attrs; maintainer-confirmed browser check 2026-09-10) |
| Bracketed short commit hash rendered after the version | verified — bundle embeds `hash:"g3c4ee627"`; the footer strips the `g` prefix and renders `(3c4ee627)` (compiled template + maintainer-confirmed browser check) |
| The bracketed hash is a live link to the commit | verified — compiled href is `https://github.com/ARUP-CAS/aiscr-digiarchiv-2/commit/` + hash; the displayed short hash resolves as a commit ref (HTTP 200) and the maintainer confirmed the rendered click-through |
| Hash injected automatically at build | verified — `npm run build` chain (`node git-version.js && ng build`) plus `web/pom.xml` exec wiring; the deployed bundle embeds a build identity stamped after the fix commits landed (fresh injection, not a stale committed artifact) |
| Release workflow compatibility (`new-version.yml` footer rewrite) | verified — `CHANGELOG_A` still finds exactly one `wiki/Changelog` anchor; the hash link is an `[href]` binding outside the match |
| Hash identifies the deployed code | **changed** — the embedded hash names dev HEAD `3c4ee62` (the build's base commit), and the build is again `dirty:true`; the maintainer confirmed dirty builds are the expected local-build practice for test deployments, so the hash identifies the base commit of an accepted practice rather than misidentifying the deployment (version-footer-D01) |
| Production footer | not examined — unreleased milestone work; the production comparison is not meaningful per corpus convention |

### Known defects

- version-footer-D01 (changed): **The deployed test build was produced from a dirty working tree and the footer does not surface the dirty state.** Originally verified 2026-09-10 (main-CPJF64VQ.js): the hash then named the pre-implementation commit `714e942` while the deployed bundle contained the #1113 feature, so the displayed hash misidentified the deployed code. This run (main-O3EFC64P.js): the embedded hash names dev HEAD `3c4ee62` — the build's base commit — and the maintainer confirmed that test deployments are routinely built from a local working tree, making `dirty:true` the expected build identity for the test instance; the unrendered dirty state is accepted practice, not an accidental misidentification. No open action item remains within the #1113 scope; rendering `versionInfo.dirty` stays an optional future refinement.
- version-footer-D02 (fixed): **The hash link targeted the repository root, and the displayed value was not a resolvable commit ref.** Fixed in commit `54ab8b2`: the footer strips the `g` prefix, binds the href to `https://github.com/ARUP-CAS/aiscr-digiarchiv-2/commit/<hash>`, and renders `dev` as a plain span. Verified deployed — compiled template in main-O3EFC64P.js, the displayed short hash resolves (HTTP 200), maintainer-confirmed rendered click-through (2026-09-10).
- version-footer-D03 (fixed): **`web/src/main/ng/git-version.json` was a generated build artifact committed to the repository.** Fixed in commits `0992c92` (root `.gitignore` entry) and `54ab8b2` (file deleted from tracking). Verified in the current tree: the file is absent and the ignore entry is present (2026-09-10).

### Corrections made during this run

- The first bundle fetch this run (homepage-referenced `main-X6MYJ3G3.js`) returned 404 and initially read as a broken deployment. Corrected on re-probe minutes later: the homepage then referenced `main-O3EFC64P.js` — a rollout was in flight between two builds; the recipe's re-read-and-retry resolves it and the behaviour is now recorded under Environments.
- The prior verification's D01 reading ("the displayed hash misidentifies the deployed code") was corrected by this run's evidence: the hash now names dev HEAD rather than predating the implementation commits, and the maintainer confirmed dirty builds are the expected local-build practice for the test instance.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-10 | digiarchiv-test.aiscr.cz (main bundle main-CPJF64VQ.js; embedded build v4.0.3-205-g714e9423-dirty, 2026-09-09) | First verification of #1113: the footer hash feature verified as deployed and build-injected; three defects recorded (version-footer-D01 dirty-build misidentification, D02 link target and `g` prefix, D03 committed build artifact). |
| 2026-09-10 | digiarchiv-test.aiscr.cz (main bundle main-O3EFC64P.js; embedded build v4.0.3-210-g3c4ee627-dirty, 2026-09-10T13:23:47Z) | Regression pass after fixes `0992c92`/`54ab8b2`: D02 fixed (commit link, `g` strip — deployed and click-confirmed), D03 fixed (artifact untracked and ignored), D01 changed (hash at dev HEAD; dirty accepted as expected local-build practice). Mid-rollout bundle 404 observed and documented. |
