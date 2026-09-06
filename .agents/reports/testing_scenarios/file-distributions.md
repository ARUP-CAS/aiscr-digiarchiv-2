# File distributions (ATRIUM) — testing scenario

**Key:** file-distributions (feature)
**Scope:** alternative file distributions and paradata — indexing, reader UI download, File API serving, facets, paradata; issue [ARUP-CAS/aiscr-digiarchiv-2#693](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/693) (upstream: aiscr-webamcr#3527) drove it.
**Principle:** concrete record/file ids are deliberately not embedded — record states drift, and the maintainers create purpose-built test data (linked in the issue thread). Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- **Documented File API** — `/id/{ident_cely}/file/{uuid}[/{dist}][/thumb][/thumb/page/N]` (`HandleServlet`): the public, documented surface (<https://arup-cas.github.io/aiscr-api-home/file-api/>) with the full access rules (record-level + element-level per entity), small thumbnails always public, rate limiting (429 — see the limiter facts below; only concurrent requests are effectively blocked), and 404 for unknown files. Distributions and paradata are served on this surface: any non-reserved suffix resolves as a distribution path, `orig` explicitly included.
- **Reader image surface** — `/api/img/{thumb|medium|full}?id={soubor_id}&dist={path}` (`ImageServlet`): the file-viewer's internal, **undocumented** surface. Small and large thumbnails are served without a permission check; `full` is gated by `ImageAccess` — a display-oriented model. Do not "fix" the reader model when testing #693 — the two surfaces are intended to differ; the issue explicitly left `/api/img/*` unchanged.

### Architecture and implementation facts

- **Distribution serving on the File API** (`HandleServlet.getFile`): the requested path is matched against the indexed `soubor_filepath` values — the file's own path, `path + "/orig"`, and `path + "/" + <dist>` for every live distribution (see the indexing facts below). A miss yields 404. On a hit, the response's `Content-Type` and `Content-Disposition` come from the matching `distribuce` entry (filename, mimetype).
- **Paradata branch:** for ids containing `paradata`, the served Fedora path is `{file path}/paradata/{distri}` with `Content-Type: text/plain`; `distri` is resolved by the same `distribuce` match — which cannot succeed for an id containing `paradata/` (the match compares the un-stripped id), so `distri` stays `orig`. The bare `{file_id}/paradata` form matches no indexed `soubor_filepath`.
- **Rate limiter** (`AppState`): keyed by IP only (the id parameter only exempts `thumb`). Two branches: an in-progress check (a concurrent request gets 429 "Downloading file still in progress") and an interval check whose remaining time is truncated by `Duration.toSeconds()` — with `requestInterval` = 500 ms the interval branch can never block, so only concurrent requests are limited. Applies identically to original files and distributions.
- **Indexing** (`Soubor.fillSolrFields`): `soubor_filepath` gets the file path, `path + "/orig"`, and `path + "/" + <dist>` per live distribution; `soubor_distri` gets `orig` plus the live distribution paths (DIST01 insert, DIST10 remove, in historie order); the `distribuce` array in the `soubor` JSON carries `{path, filename, size, mimetype}` per distribution (filename/size/mimetype from Fedora `fcr:metadata` ebucore/premis), with `orig` always first.

### Feature or entity model

- **Reader UI:** the file-viewer has a file select and a distribution `mat-select` (`selectedDist`: path/filename/size/mimetype) with `orig` as the default, download through `/api/img/full?id=<soubor_id>&dist=<path>`.
- **Facet:** `soubor_distri` on dokument, knihovna_3d, and samostatny_nalez.
- **Paradata:** `{file_id}/paradata/{dist}` on the File API; permissions always as the parent; served as `text/plain`.

### Discovery recipes

1. **Find files with distributions:** the `soubor_distri` facet on `entity=dokument` search (anonymous sees the values; the non-`orig` values identify records with real distributions); or OAI `GetRecord` for a dokument → `soubor/historie` `DIST01` entries (the note is the distribution path); or the record's `soubor` JSON `distribuce` array via the search API.
2. **Test distribution serving:** `…/id/<ident_cely>/file/<uuid>/<dist>` — check status, `Content-Type`, `Content-Disposition` against the `distribuce` entry; a nonexistent distribution must 404; compare the same file's `orig`.
3. **Test paradata:** `{file_id}/paradata/orig`, `{file_id}/paradata/{dist}`, and the bare `{file_id}/paradata` — compare response bodies (hashes) across the forms; permissions follow the parent.
4. **Permission comparisons:** pick files from records of different restriction (per the [`permissions`](permissions.md) scenario's discovery recipes) and compare `/id/…/file/…` codes for `orig`, a distribution, and paradata — they must match; `thumb` stays public. The credentialed positive case needs a maintainer.
5. **Rate limiting:** fire concurrent requests (parallel) — the second gets 429; rapid sequential requests pass, because the interval branch truncates sub-second remainders (see the limiter facts).
6. **Facet coverage:** query `entity=dokument`, `entity=knihovna_3d`, `entity=samostatny_nalez` with `rows=0` and check `facet_fields.soubor_distri`.

### Verification commands

```bash
# distribution download via the documented File API
curl -s -D - -o /dev/null "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"
# nonexistent distribution (expect 404)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/atr/nonexistent"
# paradata forms — compare bodies, not just headers
curl -s -o pd_orig "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/paradata/orig"
curl -s -o pd_dist "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/paradata/<dist>"
# facet coverage
curl -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=knihovna_3d&rows=0"   # check facet_fields.soubor_distri
```

## Current verification (2026-09-06, `digiarchiv-test`, dev build of the #693 fix wave, milestone v4.1.0)

Regression pass over the 2026-08-29 verification after the fix wave for #693.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Distribution download via File API `/id/…/file/{uuid}/{dist}` | works — 200 with `Content-Type`/`Content-Disposition` from the `distribuce` entry (verified `application/xml` with the distribution's own filename and `text/plain; charset=UTF-8` with its own filename, byte-exact sizes) |
| Explicit `/orig` suffix on the File API | works — 200 with the original's headers |
| Nonexistent distribution | **404** (empty body) — fixed |
| Rate limiting on distribution downloads | same limiter as the original: concurrent requests → 429 (`Retry-After: 500`); rapid sequential requests pass exactly as for the original (see the limiter facts — the interval branch is inert) |
| Paradata `paradata/orig` | works — 200, `text/plain; charset=UTF-8` |
| Paradata `paradata/{dist}` | **serves the paradata of `orig`** — byte-identical bodies (same SHA-256) across all three paradata forms, even after each Fedora paradata source was re-uploaded with distinct content (every form then served the orig source's new bytes); see file-distributions-D06 |
| Bare `{file_id}/paradata` | **404** — the issue requires paradata for `orig`; see file-distributions-D07 |
| Facet `soubor_distri` | present on dokument (`orig`, `atr/alto-xml`, `atr/stats-csv`), knihovna_3d (`orig`), and samostatny_nalez (`orig`) — fixed |
| Access rules on distributions/paradata | restricted record (anonymous): `orig` 403, `paradata/orig` 403 — same rule as the parent; `thumb` 200 (public exception preserved). Credentialed positive case verified: an authorized user gets 200 on `orig` and `paradata/orig` of the restricted record |
| Reserved suffixes | `thumb`/`thumb-large` → 200 `image/png` (public); `thumb/page/1` → 404 on the probed PDFs — see observations |
| Reader UI (file select, dist select, `orig` default, size display) | maintainer-assisted — browser-only; verified by the maintainer in the issue thread (2026-08-13), not re-verified anonymously |

### Defect walk (from the 2026-08-29 verification)

- file-distributions-D01 — **fixed**: distributions and paradata are served on the documented File API under its access rules and rate limiter (verified above).
- file-distributions-D02 — **fixed**: the File API rate limiter covers distribution downloads (429 on concurrent requests, identical behaviour to the original).
- file-distributions-D03 — **fixed** on the File API: a nonexistent distribution returns 404. The reader surface `/api/img/full?dist=…` still returns 200 + the original's headers + a Fedora error body for a nonexistent dist — unchanged by design (the reader model was explicitly left alone); see observations.
- file-distributions-D04 — **fixed**: paradata is served as `text/plain` with no original-file headers. Body correctness is now defect file-distributions-D06.
- file-distributions-D05 — **fixed**: `soubor_distri` facet present for knihovna_3d and samostatny_nalez (and dokument).

### Known defects

- file-distributions-D06: `/id/{ident_cely}/file/{file_id}/paradata/{dist}` serves the paradata of `orig` instead of the requested distribution's paradata. Evidence (2026-09-06, `digiarchiv-test`): all three paradata forms of the probed file returned byte-identical bodies (same SHA-256); implementation reading confirms the `distribuce` match compares the un-stripped id (which contains `paradata/`), so `distri` stays `orig` and the served Fedora path is always `paradata/orig`. Behaviorally sealed in the same run: after the maintainer re-uploaded each of the three Fedora paradata sources with distinct content, every form served the orig source's bytes (probed twice, identical SHA-256) — the distribution sources' own content is never served.
- file-distributions-D07: bare `/id/{ident_cely}/file/{file_id}/paradata` returns 404; the issue requires it to serve the paradata for `orig`. Evidence: 404 with empty body on the probed file; `soubor_filepath` carries no `…/paradata` entry, so the Solr lookup misses.

### Observations (not #693 acceptance defects)

- `thumb/page/N` returns 404 on the probed test PDFs while the reader's page source (`/api/img/medium?page=N`) serves — page thumbnails appear not to be generated on the test deployment. Cause not verified; not a #693 regression per code reading (the `getPdfPage` path predates it). Maintainer to confirm.
- The File API rate limiter's interval branch cannot block with `requestInterval` = 500 ms (`Duration.toSeconds()` truncation) — the documented "≥ 1 s between requests" is not enforced for sequential requests on any file, original or distribution. Pre-existing, out of #693 scope.
- The reader surface still serves a nonexistent `dist` as 200 + the original's headers + a Fedora error body (the pre-fix D03 behaviour) — the UI's download select uses this surface, so a stale distribution entry would download a corrupt file. Out of #693's acceptance by the maintainer's explicit decision; candidate for a separate issue.

### Corrections made during this run

- The identical paradata bodies were first read as possibly-equal source files (the test data seeds trivial content); the implementation reading resolved the ambiguity — the API provably requests `paradata/orig` for every `paradata/{dist}` id, so the identity is misrouting, not coincidence. Sealed behaviorally in the same run when the distinct-content re-upload of all three sources left every form serving the orig source's bytes.

### Maintainer-assisted checks

- Credentialed positive case (authorized user, restricted record): completed 2026-09-06 — 200 on `orig` and `paradata/orig`.
- Fedora-test paradata source re-upload with distinct content (D06 discrimination): completed 2026-09-06 — outcome folded into file-distributions-D06 above.
- Reader UI items (file/dist select, `orig` default, size display, file switching) — browser-only; maintainer-verified 2026-08-13.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-29 | `digiarchiv-test` (partial implementation of #693, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
| 2026-09-06 | `digiarchiv-test` (dev build of the #693 fix wave, milestone v4.1.0) | Regression pass: D01–D05 resolved; D06 (paradata/{dist} serves orig's paradata) and D07 (bare /paradata 404) minted; access-rule equality and facet coverage verified anonymously; D06 behaviorally confirmed with maintainer-re-uploaded distinct paradata sources; credentialed positive case maintainer-verified. |
