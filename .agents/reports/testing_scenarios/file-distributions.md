# File distributions (ATRIUM) — testing scenario

**Key:** file-distributions (feature)
**Scope:** alternative file distributions and paradata — indexing, reader UI download, File API serving, facets, paradata, and the File API rate limiter's `Retry-After` contract; issues [ARUP-CAS/aiscr-digiarchiv-2#693](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/693) (upstream: aiscr-webamcr#3527) and [ARUP-CAS/aiscr-digiarchiv-2#1117](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/1117) drove it.
**Principle:** concrete record/file ids are deliberately not embedded — record states drift, and the maintainers create purpose-built test data (linked in the issue thread). Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- **Documented File API** — `/id/{ident_cely}/file/{uuid}[/{dist}][/thumb][/thumb-large][/thumb/page/N]` (`HandleServlet`): the public, documented surface (<https://arup-cas.github.io/aiscr-api-home/file-api/>) with the full access rules (record-level + element-level per entity), small thumbnails always public, rate limiting (429 — see the limiter facts below), and 404 for unknown files. Distributions and paradata are served on this surface: any non-reserved suffix resolves as a distribution path, `orig` explicitly included, and the bare `paradata` suffix serves the paradata of `orig`.
- **Reader image surface** — `/api/img/{thumb|medium|full}?id={soubor_id}&dist={path}` (`ImageServlet`): the file-viewer's internal, **undocumented** surface (the `id` parameter is the `soubor[].id` field from the indexed `soubor` JSON). Small and large thumbnails are served without a permission check; `full` is gated by `ImageAccess` — a display-oriented model — rate-limited by the same `AppState` limiter as the File API, and returns 404 ("Distribuce not found") for a `dist` not present in the indexed `distribuce`. Do not "fix" the reader model when testing #693 — the two surfaces are intended to differ; the issue explicitly left `/api/img/*` unchanged.

### Architecture and implementation facts

- **Distribution serving on the File API** (`HandleServlet.getFile`): the requested path is matched against the indexed `soubor_filepath` values — the file's own path, `path + "/orig"`, and `path + "/" + <dist>` for every live distribution (see the indexing facts below). A miss yields 404. On a hit, the response's `Content-Type` and `Content-Disposition` come from the matching `distribuce` entry (filename, mimetype).
- **Paradata branch:** the id's `paradata` segment is stripped (`id.replaceAll("/paradata", "")`) before both the Solr lookup and the `distribuce` match, so `{file_id}/paradata/{dist}` resolves `distri` from the requested distribution and serves the Fedora path `{file path}/paradata/{dist}`; the bare `{file_id}/paradata` matches the file's own path, leaves `distri = "orig"`, and serves `{file path}/paradata/orig` — equivalent to `paradata/orig`. Paradata is always served as `text/plain`.
- **Rate limiter** (`AppState.canGetFileInterval`): keyed by IP only (the id parameter only exempts `thumb`). Two branches: a concurrent request gets 429 "Downloading file still in progress", and a request inside the `requestInterval` window after the previous **successful** download gets 429 "Try in N seconds." — the interval remainder is computed in milliseconds, so sub-second windows block. Since the #1117 fix (commit `d944b598`) both branches convert milliseconds to seconds with `Math.ceil(ms*.001)`, so the value is never `0` and the in-progress branch no longer writes the raw millisecond value (`500` became `1`); `Math.ceil` returns a `double`, however, so the header and the message render as a **decimal string** (`Retry-After: 1.0`, "Try in 1.0 seconds.") — file-distributions-D08, because RFC 9110 `delay-seconds` is `1*DIGIT`. With `requestInterval` = 500 ms the enforced window is 500 ms (the documented "≥ 1 s between requests" is the API doc's contract; the enforced value follows the deployment's config) and both branches report `1.0`. The reader's `full` action applies the same limiter but **only reads** its state — it never calls `writeGetFileStarted`/`writeGetFileFinished`, so its interval branch fires only when a File API download succeeded within the window, and its in-progress branch returns 429 **without** a `Retry-After` header by design (`web/docs/image-servlet.md`). Applies identically to original files, distributions, and paradata.
- **Indexing** (`Soubor.fillSolrFields`): `soubor_filepath` gets the file path, `path + "/orig"`, and `path + "/" + <dist>` per live distribution; `soubor_distri` gets `orig` plus the live distribution paths (DIST01 insert, DIST10 remove, in historie order); the `distribuce` array in the `soubor` JSON carries `{path, filename, size, mimetype}` per distribution (filename/size/mimetype from Fedora `fcr:metadata` ebucore/premis), with `orig` always first.

### Feature or entity model

- **Reader UI:** the file-viewer has a file select and a distribution `mat-select` (`selectedDist`: path/filename/size/mimetype) with `orig` as the default, download through `/api/img/full?id=<soubor_id>&dist=<path>`.
- **Facet:** `soubor_distri` on dokument, knihovna_3d, and samostatny_nalez.
- **Paradata:** `{file_id}/paradata/{dist}` and the bare `{file_id}/paradata` on the File API; permissions always as the parent; served as `text/plain`.

### Discovery recipes

1. **Find files with distributions:** the `soubor_distri` facet on `entity=dokument` search (anonymous sees the values; the non-`orig` values identify records with real distributions); or OAI `GetRecord` for a dokument → `soubor/historie` `DIST01` entries (the note is the distribution path); or the record's `soubor` JSON `distribuce` array via the search API.
2. **Find a large public file** (to hold the in-progress window open): search `entity=knihovna_3d` and read `soubor[].size_mb` per record, picking a `pristupnost: A`/`stav: 3` record — anonymous may download it and the multi-megabyte fetch keeps the server-side download in progress for seconds.
3. **Test distribution serving:** `…/id/<ident_cely>/file/<uuid>/<dist>` — check status, `Content-Type`, `Content-Disposition` against the `distribuce` entry; a nonexistent distribution must 404; compare the same file's `orig`.
4. **Test paradata:** `{file_id}/paradata/orig`, `{file_id}/paradata/{dist}`, and the bare `{file_id}/paradata` — compare response bodies (hashes) across the forms; permissions follow the parent. Pace the requests (one per command): the interval limiter blocks rapid sequential requests, and a 429 body read as a file body produces false byte-identity (see the 2026-09-10 correction).
5. **Trigger the interval branch:** fire a second request within the 500 ms window after a successful download — a shell compound of two `curl` invocations lands the second inside the window after the first process spawn; a burst of three reliably yields the 429 (the first spawn is slower than the following ones). Expected post-#1117: 429, `Retry-After` ≥ 1 in seconds, message seconds equal to the header.
6. **Trigger the in-progress branch:** run one command with `curl --parallel --parallel-immediate <large public file URL> <any other file URL>` — the URLs must be **distinct** (identical URLs serialize in one connection); the large file's Fedora fetch holds the in-progress marker (per IP, shared with every file request), so the other request gets 429. A rate-limited client read (`--limit-rate`) does **not** extend the window — responses are buffered upstream, so the server-side download finishes on its own. For `/img/full`'s interval branch, a File API success must precede it within the window (the reader only reads limiter state).
7. **Permission comparisons:** pick files from records of different restriction (per the [`permissions`](permissions.md) scenario's discovery recipes) and compare `/id/…/file/…` codes for `orig`, a distribution, and paradata — they must match; `thumb` stays public. The credentialed positive case needs a maintainer.
8. **Facet coverage:** query `entity=dokument`, `entity=knihovna_3d`, `entity=samostatny_nalez` with `rows=0` and check `facet_fields.soubor_distri`.

### Verification commands

```bash
# distribution download via the documented File API (headers to stdout, body discarded)
curl.exe -s -D - -o NUL "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"
# nonexistent distribution (expect 404)
curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/atr/nonexistent"
# paradata forms — compare bodies, not just headers; pace the requests or the limiter answers
curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/paradata/orig"
curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/paradata/<dist>"
# interval branch: burst of three, the limiter answers the later ones
curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"; curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"; curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"
# in-progress branch: distinct URLs, large file first
curl.exe -s -D - -o NUL --parallel --parallel-immediate "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<large uuid>" "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"
# /img/full interval branch: a File API success within the window precedes the reader request
curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/<dist>"; curl.exe -s -D - "https://digiarchiv-test.aiscr.cz/api/img/full?id=<soubor_id>&dist=<dist>"
# facet coverage
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=knihovna_3d&rows=0"   # check facet_fields.soubor_distri
```

## Current verification (2026-09-23, `digiarchiv-test`, build `v4.0.3-230-g1669e29c-dirty` of 2026-09-23 — base commit `1669e29c` (15:36 +0200) contains the #1117 fix `d944b598` (13:55 +0200); dirty per the local-build practice, anonymous session)

Acceptance pass for #1117 (the `Retry-After` unit fix). Pre-fix values were live-observed the same morning by the `permissions` run (interval branch "Try in 0 seconds." with `Retry-After: 0`; concurrent branch `Retry-After: 500`). Candidates resolved by the discovery recipes: recipe 1 (`soubor_distri` facet on `entity=dokument`) yields exactly one record with non-`orig` values — [C-TX-192700656](https://digiarchiv-test.aiscr.cz/id/C-TX-192700656) (distribution-bearing file `CTX192700656.pdf`, uuid `f70b5648-be52-451d-81cc-16803031144c`, soubor id `soub-649332`; distributions `orig`, `atr/alto-xml` 965187 B `application/xml`, `atr/stats-csv` 3 B `text/plain`) — and recipe 2 (`entity=knihovna_3d`, `size_mb`) yields [C-3D-202500002](https://digiarchiv-test.aiscr.cz/id/C-3D-202500002) (pristupnost A, stav 3) with a 4.25 MB public file `C3D202500002.jpg` (uuid `b1ebe502-951a-4cc4-8d0a-8c455fe71f75`), used to hold the in-progress window open.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Interval branch, File API | 429 with `Retry-After: 1.0` and body "Try in 1.0 seconds." — the value is now in **seconds** and never `0` (sub-second remainders report 1), and the message seconds equal the header; the header renders as a decimal string, which is not a valid RFC 9110 `delay-seconds` integer (file-distributions-D08) |
| In-progress branch, File API | 429 with `Retry-After: 1.0` and body "Downloading file still in progress. Try later." — the raw millisecond value (`500`) is gone; same decimal-rendering defect (D08) |
| Interval branch, reader `/api/img/full` | 429 with `Retry-After: 1.0` and body "Try in 1.0 seconds." — fired by a File API success within the window (the reader only reads limiter state); same D08 rendering |
| In-progress branch, reader `/api/img/full` | 429 "Downloading file still in progress. Try later." **without** a `Retry-After` header — unchanged, documented design (`web/docs/image-servlet.md`), not part of #1117's three fix sites |
| Distribution download via File API `/id/…/file/{uuid}/{dist}` | works — 200 with `Content-Type`/`Content-Disposition` from the `distribuce` entry: `atr/stats-csv` → `text/plain; charset=UTF-8`, `distribution_2.txt`, 3 B; `atr/alto-xml` → `application/xml`, `CTX192700656.alto`; `C3D202500002.jpg` → `image/jpeg`, served chunked |
| Reader `/api/img/full` serving | works — 200 for `id=soub-649332&dist=atr/stats-csv` with `Content-Type: text/plain; charset=UTF-8`, `Content-Disposition: filename=distribution_2.txt`, body identical to the File API form |
| Nonexistent distribution | **404** (empty body) — `atr/nonexistent` |
| Paradata `paradata/orig` | works — 200, `text/plain; charset=UTF-8`, body `para orig` |
| Paradata `paradata/{dist}` | works — `paradata/atr/alto-xml` serves `para alto` (distinct from `para orig`) |
| Bare `{file_id}/paradata` | works — 200, `text/plain; charset=UTF-8`, body byte-identical to `paradata/orig` |
| Facet `soubor_distri` | present on dokument (`orig` 195587, `atr/alto-xml` 1, `atr/stats-csv` 1) |
| Production comparison | not examined — the fix is part of the unreleased milestone v4.1.0, so the production comparison is not meaningful per corpus convention |

### Defect walk (from the 2026-09-10 verification)

- file-distributions-D01 — **still fixed** (this run): distributions are served on the documented File API under its access rules and rate limiter (three distributions verified 200 with correct type/disposition; the 429s below fired on distribution downloads).
- file-distributions-D02 — **still fixed** (this run): the File API rate limiter covers distribution downloads (both 429 branches observed on distribution paths).
- file-distributions-D03 — **still fixed** (this run): a nonexistent distribution returns 404 on the File API.
- file-distributions-D04 — **still fixed** (this run): paradata is served as `text/plain` with no original-file headers.
- file-distributions-D05 — **still fixed** (this run): `soubor_distri` facet present for dokument (knihovna_3d and samostatny_nalez not re-queried this run; unchanged since 2026-09-10).
- file-distributions-D06 — **still fixed** (this run): `paradata/{dist}` serves the paradata of the requested distribution (`para alto` vs `para orig`, distinct).
- file-distributions-D07 — **still fixed** (this run): the bare `paradata` serves the paradata of `orig` (body identical to `paradata/orig`).

### Known defects

- file-distributions-D08 (Medium, open): **`Retry-After` renders as a decimal string, not an integer.** All three #1117 fix sites compute the value with `Math.ceil(ms*.001)`, which returns a Java `double`, and concatenate it directly: the header and the message read `1.0` instead of `1`. RFC 9110 §10.2.3 defines `delay-seconds = 1*DIGIT`, so `1.0` is not a valid value — a spec-conformant client must treat the header as malformed and ignore it, which restores the immediate-retry loop the issue reported for those clients (lenient integer parsers read it as 1). Introduced by the fix commit `d944b598`; the issue's proposed `String.valueOf(Math.max(1, (ms + 999) / 1000))` renders an integer. Verified live on all three sites (both File API branches and `/api/img/full`'s interval branch) on the 2026-09-23 build.

### Observations (not #1117 acceptance defects)

- The `permissions` scenario's durable limiter note records the pre-#1117 values (`Retry-After: 0` interval, `500` concurrent) with their live-verified dates; its next run should repoint it at this scenario's limiter facts rather than restating them.
- The enforced window equals `requestInterval` (500 ms in the deployment config), not the documented "≥ 1 s" — unchanged, pre-existing.
- The deployed build is `dirty` (local-build practice, maintainer-confirmed 2026-09-10); its base commit `1669e29c` contains the fix, and the observed header values match the fixed code exactly.
- The subject working tree carries uncommitted line-ending churn on the limiter servlets (working tree matches the committed fix content).

### Corrections made during this run

- None — no earlier conclusion of this scenario moved; the new D08 finding is recorded directly.

### Maintainer-assisted checks

- None — the #1117 acceptance surface is fully anonymous-probeable; no check required a role, credentials, or a browser.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-29 | `digiarchiv-test` (partial implementation of #693, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
| 2026-09-06 | `digiarchiv-test` (dev build of the #693 fix wave, milestone v4.1.0) | Regression pass: D01–D05 resolved; D06 (paradata/{dist} serves orig's paradata) and D07 (bare /paradata 404) minted; access-rule equality and facet coverage verified anonymously; D06 behaviorally confirmed with maintainer-re-uploaded distinct paradata sources; credentialed positive case maintainer-verified. |
| 2026-09-10 | `digiarchiv-test` (build `v4.0.3-205-g714e9423-dirty` of 2026-09-09) | Regression pass: D06 and D07 resolved (per-form paradata bodies distinct; bare form identical to `paradata/orig`); the interval rate-limiter branch now blocks (durable limiter facts rewritten) and the reader `full` surface now 404s unknown distributions; no open defects remain on this scenario. |
| 2026-09-23 | `digiarchiv-test` (build `v4.0.3-230-g1669e29c-dirty`, base commit of 15:36 +0200, contains fix `d944b598` of 13:55 +0200) | #1117 acceptance pass: both File API limiter branches and the reader's interval branch now report `Retry-After` in seconds with a ceiling (never `0`, no raw `500`); both branches live-verified with a working concurrency recipe; D01–D07 hold; **D08 minted** (decimal rendering `1.0`, invalid `delay-seconds`); durable limiter facts and recipes rewritten. |
