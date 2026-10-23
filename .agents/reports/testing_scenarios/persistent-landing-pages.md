# Persistent landing pages — testing scenario

**Key:** persistent-landing-pages (feature)
**Scope:** the record landing page `/id/<ident_cely>` as a persistent (citable) URL — how its HTML shell is produced, cached, and kept consistent with the deployed frontend build across deployments. Driven by [ARUP-CAS/aiscr-digiarchiv-2#915](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/915). Out of scope: `/id/<ident>/file/*` (File API) and `/map/<ident>` (which bypasses the page cache by construction).
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | Build observed on 2026-10-23 | Landing-page cache |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | frontend bundle `v4.1.0-7-geafb47ea` | Bypassed: the prior permissions verification records `isTestEnv=true`, which skips both cache read and cache write. This run could not re-derive the flag (no route exposes it). |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | frontend bundle `v4.0.3-237-g96deec70-dirty` | Active: a one-day anonymous cache of the rendered shell. Backend build identity is not exposed by any route. |

- The shell is rendered by the Angular SSR process (`web/src/main/ng/src/server.ts`, `http://localhost:4000`), which `HandleServlet` calls server-side. The browser and server build output is copied into `web/src/main/webapp` by `web/pom.xml` (`copy-frontend-resources`, `copy-server-resources`) and shipped inside the WAR.
- Hashed assets change per build and are not accumulated: each instance serves only its own build's `main-<hash>.js`, and the other environment's bundle returns 404 (verified 2026-10-23). A shell that references a previous build's bundle therefore cannot bootstrap, even though the HTML itself is served with HTTP 200.

### Architecture and implementation facts

- Route: `/id/<ident>` → `HandleServlet.processRequest`. `checkId` queries the `entities` Solr core and gates 404 (no document), 410 (`is_deleted`), 401 (not `searchable` for anonymous), 403 (per-model `filterOAI`); only a 200 reaches the shell path. The permission model itself is owned by the [`permissions`](permissions.md) scenario.
- Shell path: the cache file lives under `InitServlet.CONFIG_DIR` at `.amcr/cache/<three reversed two-character segments of the ident>/<ident>`. It is served when it exists, was modified after `InitServlet.STARTED_AT`, and is younger than one day; otherwise the servlet fetches `http://localhost:4000/id/<ident>` and writes the response body to the cache when the caller is not logged in and the instance is not `isTestEnv`.
- `STARTED_AT` is a static set when the servlet class loads, i.e. it marks the Java application's start. It is not derived from the deployed frontend build.
- The #915 guard (commit `377abd50`) added the `fileTime.isAfter(InitServlet.STARTED_AT)` condition. Consequence: any cache entry older than the current Java process is ignored and overwritten, so a deployment that restarts the Java application invalidates the previous shells at once.
- The guard is scoped to process start, not to the asset build. A deployment that replaces the frontend/SSR payload without restarting the Java application leaves pre-deployment shells valid until their one-day age expires (persistent-landing-pages-D01).
- The cache write does not inspect the SSR response: whatever body arrives is written, including an error page or an empty body, and served for up to one day (pre-existing observation O1).
- A transport exception while fetching the SSR leaves the response as HTTP 200 with an empty body, because the writer is already open and the exception is only logged (pre-existing observation O2).
- The cache entry is keyed by ident only and carries no build marker, so nothing checks whether the assets referenced by a cached shell still exist.
- `/map/<ident>` forwards to `/map2` before the cache branch is reached, so the page cache never applies to map routes.

### Feature or entity model

- The landing page's observable outcomes are the gate's statuses (404/410/401/403) or a 200 shell. The shell is record-independent: every record returns the same HTML and the record data is fetched by the SPA from `/api/search/handle`.
- The cache affects only the 200 shell, and only anonymous requests write entries.
- Failure signature for #915: a shell cached under build A is served after a deployment to build B; it references `main-<A>.js`, which no longer exists (404), so the SPA never bootstraps and the page stays blank, while a search URL such as `/results?q=<ident>` still works because it serves the current shell. Because the shell is shared, a stale entry can affect any record cached before the deployment, not only the record that first triggered it.
- A shell that is byte-identical across records is expected, not evidence of a defect.

### Discovery recipes

1. **Fresh candidates across record types:** `GET /api/search/query?entity=<entity>&q=*:*&sort=datestamp desc&rows=3&fl=ident_cely` for `dokument`, `projekt`, `samostatny_nalez`, `akce`, `lokalita`, `adb`, `pian`, `let`; open `/id/<ident_cely>` for each.
2. **Shell-versus-build comparison:** fetch `/`, collect the `main-*`, `styles-*`, `scripts-*`, `chunk-*` names it references, then fetch each `/id/<ident_cely>` and extract the same names. Every 200 shell must reference only the homepage set. A shell naming any other bundle is stale.
3. **Asset resolution:** request every asset path extracted in step 2. Any non-200 means that shell cannot bootstrap.
4. **Build-turnover control:** request the other environment's current `main-<hash>.js` on this environment; 404 confirms that assets are replaced rather than accumulated, which is why a stale shell blanks the page.
5. **Build identity:** fetch `/`, resolve `main-<hash>.js`, download it, and read the embedded `raw:"v…"` git-describe object. The frontend bundle does not identify the backend build.
6. **Guard inspection:** read the cache path in `HandleServlet` and `STARTED_AT` in `InitServlet` in the checkout, and check whether the deployed tree contains the fix commit with `git merge-base --is-ancestor <fix-commit> <release-tag>`.

### Verification commands

```bash
# landing page status and shell (swap the host for the other environment)
curl.exe -s -o NUL -w "%{http_code}\n" "https://digiarchiv.aiscr.cz/id/<IDENT>"
curl.exe -s "https://digiarchiv.aiscr.cz/id/<IDENT>"
# current build's asset set (resolve main-<hash> from the homepage)
curl.exe -s "https://digiarchiv.aiscr.cz/"
curl.exe -s -o NUL -w "%{http_code}\n" "https://digiarchiv.aiscr.cz/main-<HASH>.js"
# record data behind the shell
curl.exe -s "https://digiarchiv.aiscr.cz/api/search/handle?id=<IDENT>"
# candidate discovery
curl.exe -s "https://digiarchiv.aiscr.cz/api/search/query?entity=<ENTITY>&q=*%3A*&sort=datestamp%20desc&rows=3&fl=ident_cely"
```

Rate-limit the sweep to roughly one request per second; no credentials are needed for the shell and the anonymous cache path.

## Current verification (2026-10-23, production `digiarchiv.aiscr.cz` bundle `v4.0.3-237-g96deec70-dirty`; test `digiarchiv-test.aiscr.cz` bundle `v4.1.0-7-geafb47ea`)

**Scope and limitations.** Production was probed read-only and anonymously with explicit operator approval; the sweep used about one request per second and stored no personal data. The cache path is disabled on the test instance, so no cache behaviour can be observed there. No deployment took place during the run, so the post-deployment state was not observed and the guard was assessed from source. The backend build is not exposed, so its identity is not inferred from the frontend bundle.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| The record cited in the driving issue loads | ✓ HTTP 200, 43,731-byte shell; `/api/search/handle` returns the record's dokument data |
| Landing pages sampled across eight record types, production | ✓ 19 of 19 responses with a shell referenced only the current bundle; the other 5 sampled pages returned 403 (permission gate, not cache) |
| Landing pages sampled, test | ✓ 21 of 21 responses with a shell referenced only the current bundle; 3 returned 403 |
| Referenced assets resolve | ✓ all 8 production assets and all 3 test assets returned 200 |
| Shell is record-independent | ✓ two different records returned byte-identical HTML (SHA-256 `C1CA5CF7…`) |
| Assets are replaced between builds | ✓ each environment's bundle returns 404 on the other environment |
| #915 guard present in source | ✓ cache valid only if newer than `STARTED_AT` and younger than one day; present on `origin/dev`, absent from release tag `v4.1.0` |
| Cache invalidated by a real deployment | — not examined: no deployment in this run |
| Production backend carries the guard | — unverified: no route exposes the backend build; the frontend bundle predates the fix |
| `/map/<ident>` cache behaviour | — not examined: the route bypasses the cache branch by construction |

### Known defects

- **persistent-landing-pages-D01 — Medium (open).** Cache validity is keyed to the Java application's start, not to the build that produced the shell, so permanence depends on an invariant the application neither checks nor records. A deployment that replaces the frontend/SSR payload without restarting the Java application leaves pre-deployment shells valid for up to one day; those shells reference bundles that now return 404, and the page stays blank. Evidence: the `fileTime.isAfter(InitServlet.STARTED_AT)` condition in `HandleServlet` (with `STARTED_AT` set at class load), the WAR packaging in `web/pom.xml`, and the 404 cross-build control above. Fix direction: carry the build identity into the cache decision (key the entry by the current `main-<hash>`, store the referenced bundle beside the shell, or re-check the referenced asset before serving), or purge `.amcr/cache` as part of every deployment; validating the SSR status before writing would close the adjacent pre-existing gap as well.

### Corrections made during this run

- The first sweep classification counted 5 production and 3 test pages as "shell does not match the current bundle". Re-reading showed that all of them were 403 gate refusals with no shell and no assets; the stale-shell count is zero. The matrix records the corrected result.
- The working assumption that the deployed `main-<hash>.js` identifies the deployed backend was wrong: the bundle is a frontend artefact only, and no route exposes the backend build. The production backend question is therefore recorded as unverified rather than as a defect.

### Pre-existing observations

These predate the #915 change and are recorded without verdict-bearing severity; they are not attributed to the fix.

- **O1 — unvalidated caching.** The cache write uses the SSR response body without checking `hresponse.statusCode()` or the body content, so a 5xx or error page served once after an application start is cached and served for up to one day. Introduced with the cache in `5840ea72` (#487).
- **O2 — empty 200 on transport failure.** If the SSR request throws, `HandleServlet` logs the exception and returns HTTP 200 with an empty body (the writer is already open), which reads as a blank page until the next reload. Same provenance.
- **O3 — gate refusals in the sample.** The eight 403 responses come from the permission gate refusing anonymously invisible projekt/ADB records; the [`permissions`](permissions.md) scenario documents that behaviour, and the record-level state of those records was not re-examined here.

### Unverified

- Whether the production WAR carries commit `377abd50`. The frontend bundle is built from `96deec70` (2026-09-29), which predates the fix, and the backend build is not exposed.
- Behaviour across an actual deployment, which is the acceptance check the guard is meant to satisfy.
- The `isTestEnv` flag on the test instance: inherited from the prior permissions verification, not re-derived in this run.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-10-23 | production `v4.0.3-237-g96deec70-dirty`; test `v4.1.0-7-geafb47ea` | First verification of #915: the reported symptom does not reproduce and every sampled shell matches the current build; the #915 guard is present on `origin/dev` but not in `v4.1.0`; D01 records that the guard is keyed to a Java restart rather than to the build identity; two pre-existing cache robustness gaps are recorded as observations. |
