# Persistent landing pages — testing scenario

**Key:** persistent-landing-pages (feature)
**Scope:** the record landing page `/id/<ident_cely>` as a persistent (citable) URL — how its HTML shell is produced, cached, and kept consistent with the deployed frontend build across deployments. Driven by [ARUP-CAS/aiscr-digiarchiv-2#915](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/915). Out of scope: `/id/<ident>/file/*` (File API) and `/map/<ident>` (which bypasses the page cache by construction).
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | Build observed on 2026-10-25 | Landing-page cache |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | frontend bundle `v4.1.1-0-g167952b9` (`main-5NJI5AS4.js`) | Bypassed: `isTestEnv=true` skips both cache read and cache write. Read from `HandleServlet`; no route exposes the flag or the backend build. |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | frontend bundle `v4.1.1-0-g167952b9` (`main-5NJI5AS4.js`); static assets `Last-Modified 2026-10-25 14:31:59Z` | Active: a one-day anonymous cache of the rendered shell, served only when the entry is newer than the current Java application start. Backend build identity is not exposed by any route. |

- The shell is rendered by the Angular SSR process (`web/src/main/ng/src/server.ts`, `http://localhost:4000`), which `HandleServlet` calls server-side. The browser and server build output is copied into `web/src/main/webapp` by `web/pom.xml` (`copy-frontend-resources`, `copy-server-resources`) and shipped inside the WAR.
- Hashed assets change per build and are not accumulated: each instance serves only its own build's `main-<hash>.js`. While the two environments run the same release they serve the same hashes, and the cross-build control below cannot be exercised.
- The `Last-Modified` header of the current `main-<hash>.js` is the only deployment-time signal reachable without server access: it moves when the frontend payload is deployed, so it dates the deployment that a post-deployment verification follows.

### Architecture and implementation facts

- Route: `/id/<ident>` → `HandleServlet.processRequest`. `checkId` queries the `entities` Solr core and gates 404 (no document), 410 (`is_deleted`), 401 (not `searchable` for anonymous), 403 (per-model `filterOAI`); only a 200 reaches the shell path. The permission model itself is owned by the [`permissions`](permissions.md) scenario.
- Shell path: the cache file lives under `InitServlet.CONFIG_DIR` at `.amcr/cache/<three reversed two-character segments of the ident>/<ident>`. It is served when it exists, was modified after `InitServlet.STARTED_AT`, and is younger than one day; otherwise the servlet fetches `http://localhost:4000/id/<ident>` and writes the response body to the cache when the caller is not logged in and the instance is not `isTestEnv`.
- `STARTED_AT` is a static set when the servlet class loads, i.e. it marks the Java application's start. It is not derived from the deployed frontend build.
- The #915 guard (commit `377abd50`) added the `fileTime.isAfter(InitServlet.STARTED_AT)` condition. Consequence: any cache entry older than the current Java process is ignored and overwritten, so a deployment that restarts the Java application invalidates the previous shells at once.
- The guard is scoped to process start, not to the asset build. A deployment that replaces the frontend/SSR payload without restarting the Java application leaves pre-deployment shells valid until their one-day age expires (persistent-landing-pages-D01). The maintainers accepted restart-keying as sufficient on 2026-10-23, because every production deployment replaces the WAR and restarts Tomcat.
- The fix ships in tag `v4.1.1` (`377abd50` is an ancestor of `v4.1.1`); `main` carries one commit on top (`v4.1.1-1-gfa827bc6`).
- The cache write does not inspect the SSR response: whatever body arrives is written, including an error page or an empty body, and served for up to one day (pre-existing observation O1).
- A transport exception while fetching the SSR leaves the response as HTTP 200 with an empty body, because the writer is already open and the exception is only logged (pre-existing observation O2).
- The cache entry is keyed by ident only and carries no build marker, so nothing checks whether the assets referenced by a cached shell still exist.
- `/map/<ident>` forwards to `/map2` before the cache branch is reached, so the page cache never applies to map routes.

### Feature or entity model

- The landing page's observable outcomes are the gate's statuses (404/410/401/403) or a 200 shell. The shell is record-independent: every accepted record returns the same HTML, and the record data is fetched by the SPA from `/api/search/handle`.
- The cache affects only the 200 shell, and only anonymous requests write entries.
- Failure signature for #915: a shell cached under build A is served after a deployment to build B; it references `main-<A>.js`, which no longer exists, so the SPA never bootstraps and the page stays blank, while a search URL such as `/results?q=<ident>` still works because it serves the current shell. Because the shell is shared, a stale entry can affect any record cached before the deployment, not only the record that first triggered it.
- A shell that is byte-identical across records is expected, not evidence of a defect. Its exact byte count is build-specific.

### Discovery recipes

1. **Fresh candidates across record types:** `GET /api/search/query?entity=<entity>&q=*:*&sort=datestamp desc&rows=3&fl=ident_cely` for `dokument`, `projekt`, `samostatny_nalez`, `akce`, `lokalita`, `adb`, `pian`, `let`; open `/id/<ident_cely>` for each.
2. **Shell-versus-build comparison:** fetch `/`, collect the `main-*`, `styles-*`, `scripts-*`, `chunk-*` names it references, then fetch each `/id/<ident_cely>` and extract the same names. Every 200 shell must reference only the homepage set. A shell naming any other bundle is stale.
3. **Asset resolution:** request every asset path extracted in step 2. Any non-200 means that shell cannot bootstrap.
4. **Build-turnover control:** request the other environment's current `main-<hash>.js` on this environment; 404 confirms that assets are replaced rather than accumulated. The control is unavailable while both environments run the same release, because the hashes coincide.
5. **Build identity:** fetch `/`, resolve `main-<hash>.js`, download it, and read the embedded `raw:"v…"` git-describe object. The frontend bundle does not identify the backend build; the WAR packaging is what ties them together.
6. **Deployment time:** request `main-<hash>.js` with headers and read `Last-Modified`; it dates the deployment that placed the current frontend payload.
7. **Guard inspection:** read the cache path in `HandleServlet` and `STARTED_AT` in `InitServlet` in the checkout, and check whether the deployed tree contains the fix commit with `git merge-base --is-ancestor <fix-commit> <release-tag>`.

### Verification commands

```bash
# landing page status and shell (swap the host for the other environment)
curl.exe -s -o NUL -w "%{http_code}\n" "https://digiarchiv.aiscr.cz/id/<IDENT>"
curl.exe -s "https://digiarchiv.aiscr.cz/id/<IDENT>"
# current build's asset set (resolve main-<hash> from the homepage)
curl.exe -s "https://digiarchiv.aiscr.cz/"
curl.exe -s -D - -o NUL "https://digiarchiv.aiscr.cz/main-<HASH>.js"
# record data behind the shell
curl.exe -s "https://digiarchiv.aiscr.cz/api/search/handle?id=<IDENT>"
# candidate discovery
curl.exe -s "https://digiarchiv.aiscr.cz/api/search/query?entity=<ENTITY>&q=*%3A*&sort=datestamp%20desc&rows=3&fl=ident_cely"
```

Rate-limit the sweep to roughly one request per second; no credentials are needed for the shell and the anonymous cache path.

## Current verification (2026-10-25, production `digiarchiv.aiscr.cz` and test `digiarchiv-test.aiscr.cz`, both build `v4.1.1-0-g167952b9`)

**Scope and limitations.** Production was probed read-only and anonymously with explicit operator approval; the sweep used about one request per second and stored no personal data. The operator confirmed that the production deployment ran on 2026-10-25 at about 14:32Z and restarted the Java application; whether `.amcr/cache` was additionally purged is not known to the operator. The cache directory is not reachable from outside the server, so cache-entry state is not directly observable. The test instance runs the same build and disables the page cache (`isTestEnv=true`), so it supplies no cache evidence. The backend build is not exposed by any route.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| The record cited in the driving issue loads | ✓ HTTP 200, 43,674-byte shell; `/api/search/handle` returns the record's dokument data |
| Landing pages sampled across eight record types, production | ✓ 19 of 19 responses with a shell referenced only the current asset set; the other 5 sampled pages returned 403 (permission gate body confirmed, not cache) |
| Landing pages sampled, test | ✓ both sampled pages returned the current shell; the cache branch does not run there |
| Referenced assets resolve | ✓ all 8 current production assets returned 200 |
| Shell is record-independent | ✓ 19 different records returned byte-identical HTML (SHA-256 `753386979E87662FFF545D5DE1CD42259CE5A7F395C198D4CBDC72A489074724`) |
| Deployed production build identity | ✓ frontend bundle `v4.1.1-0-g167952b9` (`main-5NJI5AS4.js`), static assets `Last-Modified 2026-10-25 14:31:59Z` |
| #915 guard present in the deployed release | ✓ `377abd50` is an ancestor of tag `v4.1.1`; the `fileTime.isAfter(InitServlet.STARTED_AT)` condition is present in `HandleServlet` |
| Cache invalidated after a real deployment | ✓ 4.4 h after the confirmed restart, every sampled shell carries the post-deployment build; no stale shell was served (attribution caveat under Unverified) |
| persistent-landing-pages-D01 | — still present as a design property (validity keyed to the Java start, not the build); the maintainers accepted restart-keying on 2026-10-23, and this deployment's restart produced correct behaviour |
| Test instance cache behaviour | — not observable: `isTestEnv=true` bypasses cache read and write |
| `/map/<ident>` cache behaviour | — not examined: the route bypasses the cache branch by construction |

### Known defects

No open defects. `persistent-landing-pages-D01` remains a design property with its accepted disposition, recorded in the matrix above and in the durable half.

### Corrections made during this run

- The previous verification recorded that each environment's bundle returns 404 on the other. On 2026-10-25 both environments serve the same `v4.1.1` bundle, so that control cannot be exercised; the durable half is amended rather than the old statement being retained.
- The previous run's 43,731-byte shell is 43,674 bytes under `v4.1.1`. The byte count moves with the build and is not a stable assertion; the shell-identity result is.

### Pre-existing observations

These predate the #915 change and are recorded without verdict-bearing severity; they are not attributed to the fix.

- **O1 — unvalidated caching.** The cache write uses the SSR response body without checking `hresponse.statusCode()` or the body content, so a 5xx or error page served once after an application start is cached and served for up to one day. Introduced with the cache in `5840ea72` (#487). Not re-examined on 2026-10-25.
- **O2 — empty 200 on transport failure.** If the SSR request throws, `HandleServlet` logs the exception and returns HTTP 200 with an empty body (the writer is already open), which reads as a blank page until the next reload. Same provenance. Not re-examined on 2026-10-25.
- **O3 — gate refusals in the sample.** Re-confirmed: 5 of the 24 sampled records (2 `projekt`, 3 `adb`) returned the 403 permission page, whose body states the missing permission. The [`permissions`](permissions.md) scenario documents that behaviour; the record-level state of those records was not re-examined here.

### Unverified

- Whether the clean post-deployment result is attributable specifically to the `STARTED_AT` guard, to a manual `.amcr/cache` purge, or to natural expiry for some entries. The restart itself is confirmed, but the cache state is not exposed and the operator does not know whether the directory was purged.
- Whether the production WAR carries commit `377abd50`. No route exposes the backend build; the provenance is inferred from the frontend bundle `v4.1.1-0-g167952b9`, which `web/pom.xml` packages into the same WAR.
- The landing pages behind the 5 permission refusals were not opened under an authorized account.
- The `isTestEnv` flag on the test instance: read from `HandleServlet`, not re-derived from a route in this run.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-10-23 | production `v4.0.3-237-g96deec70-dirty`; test `v4.1.0-7-geafb47ea` | First verification of #915: the reported symptom does not reproduce and every sampled shell matches the current build; the #915 guard is present on `origin/dev` but not in `v4.1.0`; D01 records that the guard is keyed to a Java restart rather than to the build identity; two pre-existing cache robustness gaps are recorded as observations. |
| 2026-10-25 | production and test `v4.1.1-0-g167952b9` | Post-deployment regression pass: the production deployment restarted the Java application; all 19 sampled shells carry the new build and all assets resolve; D01's restart-keyed design was accepted by the maintainers, so no open defect remains; the cross-build control is unavailable while both environments run the same release. |
