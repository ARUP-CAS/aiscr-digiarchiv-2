# File distributions (ATRIUM) — testing scenario

**Key:** file-distributions (feature)
**Scope:** alternative file distributions and paradata — indexing, reader UI download, File API serving, facets, paradata; issue [ARUP-CAS/aiscr-digiarchiv-2#693](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/693) (upstream: aiscr-webamcr#3527) drove it.
**Principle:** concrete record/file ids are deliberately not embedded — record states drift, and the maintainers create purpose-built test data (linked in the issue thread). Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- **Documented File API** — `/id/{ident_cely}/file/{uuid}[/{dist}][/thumb][/thumb-large][/thumb/page/N]` (`HandleServlet`): the public, documented surface (<https://arup-cas.github.io/aiscr-api-home/file-api/>) with the full access rules (record-level + element-level per entity), small thumbnails always public, rate limiting (429 — see the limiter facts below), and 404 for unknown files. Distributions and paradata are served on this surface: any non-reserved suffix resolves as a distribution path, `orig` explicitly included, and the bare `paradata` suffix serves the paradata of `orig`.
- **Reader image surface** — `/api/img/{thumb|medium|full}?id={soubor_id}&dist={path}` (`ImageServlet`): the file-viewer's internal, **undocumented** surface (the `id` parameter is the `soubor[].id` field from the indexed `soubor` JSON). Small and large thumbnails are served without a permission check; `full` is gated by `ImageAccess` — a display-oriented model — rate-limited by the same `AppState` limiter as the File API, and returns 404 ("Distribuce not found") for a `dist` not present in the indexed `distribuce`. Do not "fix" the reader model when testing #693 — the two surfaces are intended to differ; the issue explicitly left `/api/img/*` unchanged.

### Architecture and implementation facts

- **Distribution serving on the File API** (`HandleServlet.getFile`): the requested path is matched against the indexed `soubor_filepath` values — the file's own path, `path + "/orig"`, and `path + "/" + <dist>` for every live distribution (see the indexing facts below). A miss yields 404. On a hit, the response's `Content-Type` and `Content-Disposition` come from the matching `distribuce` entry (filename, mimetype).
- **Paradata branch:** the id's `paradata` segment is stripped (`id.replaceAll("/paradata", "")`) before both the Solr lookup and the `distribuce` match, so `{file_id}/paradata/{dist}` resolves `distri` from the requested distribution and serves the Fedora path `{file path}/paradata/{dist}`; the bare `{file_id}/paradata` matches the file's own path, leaves `distri = "orig"`, and serves `{file path}/paradata/orig` — equivalent to `paradata/orig`. Paradata is always served as `text/plain`.
- **Rate limiter** (`AppState.canGetFileInterval`): keyed by IP only (the id parameter only exempts `thumb`). Two branches: a concurrent request gets 429 "Downloading file still in progress", and a request inside the `requestInterval` window after the previous **successful** download gets 429 "Try in N seconds." — the interval remainder is computed in milliseconds, so sub-second windows block. The `Retry-After` header and the message seconds are still truncated (`retryTime/1000`), so a sub-second remainder reports `Retry-After: 0`. With `requestInterval` = 500 ms the enforced window is 500 ms (the documented "≥ 1 s between requests" is the API doc's contract; the enforced value follows the deployment's config). The reader's `full` action applies the same limiter. Applies identically to original files, distributions, and paradata.
- **Indexing** (`Soubor.fillSolrFields`): `soubor_filepath` gets the file path, `path + "/orig"`, and `path + "/" + <dist>` per live distribution; `soubor_distri` gets `orig` plus the live distribution paths (DIST01 insert, DIST10 remove, in historie order); the `distribuce` array in the `soubor` JSON carries `{path, filename, size, mimetype}` per distribution (filename/size/mimetype from Fedora `fcr:metadata` ebucore/premis), with `orig` always first.

### Feature or entity model

- **Reader UI:** the file-viewer has a file select and a distribution `mat-select` (`selectedDist`: path/filename/size/mimetype) with `orig` as the default, download through `/api/img/full?id=<soubor_id>&dist=<path>`.
- **Facet:** `soubor_distri` on dokument, knihovna_3d, and samostatny_nalez.
- **Paradata:** `{file_id}/paradata/{dist}` and the bare `{file_id}/paradata` on the File API; permissions always as the parent; served as `text/plain`.

### Discovery recipes

1. **Find files with distributions:** the `soubor_distri` facet on `entity=dokument` search (anonymous sees the values; the non-`orig` values identify records with real distributions); or OAI `GetRecord` for a dokument → `soubor/historie` `DIST01` entries (the note is the distribution path); or the record's `soubor` JSON `distribuce` array via the search API.
2. **Test distribution serving:** `…/id/<ident_cely>/file/<uuid>/<dist>` — check status, `Content-Type`, `Content-Disposition` against the `distribuce` entry; a nonexistent distribution must 404; compare the same file's `orig`.
3. **Test paradata:** `{file_id}/paradata/orig`, `{file_id}/paradata/{dist}`, and the bare `{file_id}/paradata` — compare response bodies (hashes) across the forms; permissions follow the parent. Pace the requests (one per command): the interval limiter blocks rapid sequential requests, and a 429 body read as a file body produces false byte-identity (see the 2026-09-10 correction).
4. **Permission comparisons:** pick files from records of different restriction (per the [`permissions`](permissions.md) scenario's discovery recipes) and compare `/id/…/file/…` codes for `orig`, a distribution, and paradata — they must match; `thumb` stays public. The credentialed positive case needs a maintainer.
5. **Rate limiting:** fire concurrent requests (parallel) — the second gets 429; rapid sequential requests within `requestInterval` also get 429 (both branches now block; see the limiter facts).
6. **Facet coverage:** query `entity=dokument`, `entity=knihovna_3d`, `entity=samostatny_nalez` with `rows=0` and check `facet_fields.soubor_distri`.

### Verification commands

```bash
# distribution download via the documented File API
curl -s -D - -o /dev/null "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"
# nonexistent distribution (expect 404)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/atr/nonexistent"
# paradata forms — compare bodies, not just headers; pace the requests or the limiter answers
curl -s -o pd_orig "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/paradata/orig"
curl -s -o pd_dist "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/paradata/<dist>"
# facet coverage
curl -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=knihovna_3d&rows=0"   # check facet_fields.soubor_distri
```

## Current verification (2026-09-10, `digiarchiv-test`, build `v4.0.3-205-g714e9423-dirty` of 2026-09-09 per the same-day `version-footer` verification, anonymous session)

Regression pass over the 2026-09-06 verification after the D06/D07 fix wave. Candidates resolved by the discovery recipes: the `soubor_distri` facet on `entity=dokument` yields exactly one record with non-`orig` values — [C-TX-192700656](https://digiarchiv-test.aiscr.cz/id/C-TX-192700656) (its distribution-bearing file `CTX192700656.pdf` now carries real ALTO content in `atr/alto-xml` and a `distribution_2.txt` in `atr/stats-csv`, plus a second file `CTX192700656B.pdf` with no distributions), and the issue-thread example [C-TX-202600010](https://digiarchiv-test.aiscr.cz/id/C-TX-202600010) (restricted, no distributions).

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Distribution download via File API `/id/…/file/{uuid}/{dist}` | works — 200 with `Content-Type`/`Content-Disposition` from the `distribuce` entry: `atr/alto-xml` → `application/xml` with filename `CTX192700656.alto`, byte-exact size (965187 B); `atr/stats-csv` → `text/plain; charset=UTF-8` with filename `distribution_2.txt`, Content-Length 3 |
| Explicit `/orig` suffix on the File API | works — 200, `application/pdf`, `CTX192700656.pdf` |
| Nonexistent distribution | **404** (empty body) — `atr/nonexistent` |
| Paradata `paradata/orig` | works — 200, `text/plain; charset=UTF-8`, body `para orig` |
| Paradata `paradata/{dist}` | works — `paradata/atr/alto-xml` serves `para alto`, `paradata/atr/stats-csv` serves `para csv` (SHA-256 distinct per form; see file-distributions-D06 resolved) |
| Bare `{file_id}/paradata` | works — 200, `text/plain; charset=UTF-8`, body byte-identical to `paradata/orig` (same SHA-256; see file-distributions-D07 resolved) |
| Rate limiting on distribution downloads | both branches block: rapid sequential requests within the 500 ms `requestInterval` get 429 ("Try in 0 seconds.", `Retry-After: 0`); the concurrent branch is verified by code and the 2026-09-06 run (this session's three parallel-overlap attempts did not overlap — a probe-environment limitation, not evidence of change) |
| Facet `soubor_distri` | present on dokument (`orig` 195586, `atr/alto-xml` 1, `atr/stats-csv` 1), knihovna_3d (`orig` 696), and samostatny_nalez (`orig` 3411) |
| Access rules on distributions/paradata | restricted record (anonymous): `orig` 403, `paradata/orig` 403, bare `paradata` 403 — same rule as the parent; `thumb` 200 (public exception preserved). Credentialed positive case: maintainer-verified 2026-09-06; the D06/D07 fixes do not touch the permission model |
| Reserved suffixes | `thumb`/`thumb-large` → 200 `image/png` (public); `thumb/page/1` → 404 on the probed PDFs — see observations |
| Reader surface `/api/img/full` | nonexistent `dist` → **404** "Distribuce not found" (previously 200 + the original's headers + a Fedora error body) |
| Reader UI (file select, dist select, `orig` default, size display) | maintainer-assisted — browser-only; verified by the maintainer in the issue thread (2026-08-13), not re-verified anonymously |

### Defect walk (from the 2026-09-06 verification)

- file-distributions-D01 — **fixed** (2026-09-06): distributions and paradata are served on the documented File API under its access rules and rate limiter.
- file-distributions-D02 — **fixed** (2026-09-06): the File API rate limiter covers distribution downloads.
- file-distributions-D03 — **fixed** (2026-09-06): a nonexistent distribution returns 404 on the File API. The reader surface's equivalent 200-behaviour is also resolved this run (404, verified above).
- file-distributions-D04 — **fixed** (2026-09-06): paradata is served as `text/plain` with no original-file headers.
- file-distributions-D05 — **fixed** (2026-09-06): `soubor_distri` facet present for dokument, knihovna_3d, and samostatny_nalez.
- file-distributions-D06 — **fixed** (this run): `/id/{ident_cely}/file/{file_id}/paradata/{dist}` serves the paradata of the requested distribution. Evidence (2026-09-10): the three paradata forms return byte-distinct bodies (distinct SHA-256: `para orig` / `para alto` / `para csv`), matching the distinct-content sources re-uploaded by the maintainer on 2026-09-06; the implementation strips the `paradata` segment before the `distribuce` match, so `distri` resolves per form.
- file-distributions-D07 — **fixed** (this run): the bare `/id/{ident_cely}/file/{file_id}/paradata` serves the paradata of `orig`. Evidence (2026-09-10): byte-identical body and SHA-256 with `paradata/orig`; the implementation resolves the bare form to `distri = "orig"`.

### Known defects

- None identified. No admissible findings remain open on this scenario.

### Observations (not #693 acceptance defects)

- `thumb/page/N` returns 404 on the probed test PDFs while the reader's page source (`/api/img/medium?page=N`) serves — page thumbnails appear not to be generated on the test deployment. Unchanged from the 2026-09-06 run; not a #693 regression per code reading.
- The interval-branch 429 reports `Retry-After: 0` for sub-second remainders (the header and message seconds are truncated), so a client honouring the header retries immediately and hits 429 again; the enforced window equals `requestInterval` (500 ms in the deployment config), not the documented "≥ 1 s". The 2026-09-06 note that the interval branch never blocked is resolved — it now blocks; this remainder is the residual truncation.
- Production comparison not performed — the feature is part of the unreleased milestone v4.1.0.

### Corrections made during this run

- The first rapid batch of paradata requests returned byte-identical bodies for the three distribution forms — initially indistinguishable from file-distributions-D06 still present. The header check revealed these were 429 rate-limiter bodies ("Try in 0 seconds."), not file content: the interval limiter now blocks rapid sequential requests. Re-probed individually with spacing, all three forms returned distinct bodies. Recorded so future runs pace their paradata probes (the discovery recipe now says so).

### Maintainer-assisted checks

- Credentialed positive case (authorized user, restricted record): completed 2026-09-06 — 200 on `orig` and `paradata/orig`. Not re-performed this run; the D06/D07 fixes do not touch the permission model, and the changed paradata paths were verified anonymously on the public distribution-bearing record.
- Reader UI items (file/dist select, `orig` default, size display, file switching) — browser-only; maintainer-verified 2026-08-13; not re-verified this run.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-29 | `digiarchiv-test` (partial implementation of #693, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
| 2026-09-06 | `digiarchiv-test` (dev build of the #693 fix wave, milestone v4.1.0) | Regression pass: D01–D05 resolved; D06 (paradata/{dist} serves orig's paradata) and D07 (bare /paradata 404) minted; access-rule equality and facet coverage verified anonymously; D06 behaviorally confirmed with maintainer-re-uploaded distinct paradata sources; credentialed positive case maintainer-verified. |
| 2026-09-10 | `digiarchiv-test` (build `v4.0.3-205-g714e9423-dirty` of 2026-09-09) | Regression pass: D06 and D07 resolved (per-form paradata bodies distinct; bare form identical to `paradata/orig`); the interval rate-limiter branch now blocks (durable limiter facts rewritten) and the reader `full` surface now 404s unknown distributions; no open defects remain on this scenario. |
