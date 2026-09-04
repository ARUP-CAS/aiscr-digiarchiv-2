# File distributions (ATRIUM) — testing scenario

**Key:** file-distributions (feature)
**Scope:** alternative file distributions and paradata — indexing, reader UI download, File API serving, facets, paradata; issue [ARUP-CAS/aiscr-digiarchiv-2#693](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/693) (upstream: aiscr-webamcr#3527) drove it.
**Principle:** concrete record/file ids are deliberately not embedded — record states drift, and the maintainers create purpose-built test data (linked in the issue thread). Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- **Documented File API** — `/id/{ident_cely}/file/{uuid}[/thumb][/thumb/page/N]` (`HandleServlet`): the public, documented surface (<https://arup-cas.github.io/aiscr-api-home/file-api/>) with the full access rules (record-level + element-level per entity), small thumbnails always public, rate limiting (429, ≥ 1 s between requests), and 404 for unknown files.
- **Reader image surface** — `/api/img/{thumb|medium|full}?id={soubor_id}&dist={path}` (`ImageServlet`): the file-viewer's internal, **undocumented** surface. Small and large thumbnails are served without a permission check (the reader's thumbnail source, per the docs' "all small thumbnails available without authentication"); `full` is gated by `ImageAccess` — a display-oriented model (pristupnost comparison only, no `stav` rule, samostatny_nalez unrestricted by the explicit decision in issue #85, no projekt rules). Do not "fix" the reader model when testing #693 — the two surfaces are intended to differ.

### Architecture and implementation facts

- Distribution downloads are currently implemented **on the reader surface** (`/api/img/full?dist=…`), not the File API — the root cause of most gaps in the current verification.

### Feature or entity model

- **Indexing:** the record's `soubor` JSON carries a `distribuce` array — `{path, filename, size, mimetype}` per distribution (from `ebucore` `fcr:metadata`), with `orig` always first. The OAI `soubor/historie` shows the `DIST01` entries (insert; `DIST11` update, `DIST10` delete — the liveness rule per the issue: a `DIST01` counts unless a `DIST10` with a later date and the same note exists).
- **Reader UI:** the file-viewer has a distribution `mat-select` (`selectedDist`: path/filename/size/mimetype) with download through `/api/img/full?id=<soubor_id>&dist=<path>` and headers taken from the `distribuce` entry.
- **Facet:** `soubor_distri` on the dokument entity.
- **Paradata:** reachable only as `dist=paradata/…` on the reader surface; not on the File API path.

### Discovery recipes

1. **Find files with distributions:** OAI `GetRecord` for a dokument → inspect `soubor/historie` for `DIST01` entries (the note is the distribution path, e.g. `atr/alto-xml`); or the `soubor_distri` facet on `entity=dokument` search (anonymous sees the values); or the record's `soubor` JSON `distribuce` array via `/api/search/handle`.
2. **Test distribution serving:** `…/api/img/full?id={soubor_id}&dist={path}` (URL-encode the slash as `%2F`) — check status, `Content-Type`, `Content-Disposition` against the `distribuce` entry; compare the File API path `/id/{ident}/file/{uuid}/{path}` for the 404 gap.
3. **Test paradata:** `dist=paradata/orig` and `dist=paradata/{path}` — verify the body differs from the original and check the headers (known gap, file-distributions-D04); the bare `{file_id}/paradata` form on the File API.
4. **Permission comparisons:** pick files from records of different restriction (pristupnost/stav per the [`permissions`](permissions.md) scenario's discovery recipes) and compare `/id/…/file/…` codes with `/api/img/full` codes — remember the two surfaces are intended to differ; the #693 requirement is that the **File API** serves distributions under its rules.
5. **Rate limiting:** fire rapid repeated requests on both surfaces (File API → 429 expected; img/full → 200s expose the gap).
6. **Facet coverage:** query `entity=knihovna_3d` and `entity=samostatny_nalez` with `rows=0` and check `facet_fields` for `soubor_distri`.

### Verification commands

```bash
# distribution download via the reader surface (dist path slash encoded)
curl -s -D - -o /dev/null "https://digiarchiv-test.aiscr.cz/api/img/full?id=<soubor_id>&dist=atr%2Falto-xml"
# the documented File API path (currently 404 for distributions)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/id/<ident_cely>/file/<uuid>/atr/alto-xml"
# paradata
curl -s -D - -o /dev/null "https://digiarchiv-test.aiscr.cz/api/img/full?id=<soubor_id>&dist=paradata%2Forig"
# facet coverage
curl -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=knihovna_3d&rows=0"   # check facet_fields.soubor_distri
```

## Current verification (2026-08-29, `digiarchiv-test`, partial implementation of #693, milestone v4.1.0)

Supplements the maintainer feedback already posted in the issue thread (2026-08-13, reader UI items — not repeated here).

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Distribution indexing (incl. ebucore filename/mimetype) | works — correct `Content-Type`/`Content-Disposition` for real distributions (e.g. `application/xml` with the distribution's own filename) |
| `orig` as default in the select | data-level works — `orig` is always `distribuce[0]`; the reader's state bug on file switching is the already-reported UI item |
| Distribution download via reader (`/api/img/full?dist=…`) | works for existing distributions |
| Paradata content via `dist=paradata/…` | served (content differs from the original) but with the **original's headers** |
| File API path `/id/…/file/{uuid}/{dist}` | **404** — distributions and paradata not served on the documented surface |
| Access rules on distribution downloads | reader model only (pristupnost comparison; tested restricted records returned 401) — the File API rules do not apply |
| Rate limiting on distribution downloads | **none** — rapid repeated requests all 200 |
| Nonexistent distribution (`dist=atr/foo`) | **200** with the original's headers and a Fedora error text as body — no 404 |
| Facet `soubor_distri` | dokument only — **absent for knihovna_3d and samostatny_nalez** (issue requires all three) |
| Reserved suffixes (`thumb`) on the File API | still work |

### Known defects

- file-distributions-D01: distributions/paradata not served through the documented File API (`/id/…/file/{file_id}/{dist}` → 404; bare `{file_id}/paradata` does not map to `paradata/orig`) — the "any suffix = distribution" requirement is unimplemented; downloads work only via the internal reader endpoint, so the File API contract (access rules, 429, error semantics) does not cover them.
- file-distributions-D02: no rate limit on distribution downloads (consequence of file-distributions-D01).
- file-distributions-D03: nonexistent distribution returns 200 + original's headers + Fedora error body instead of 404.
- file-distributions-D04: paradata served with the original's `Content-Type`/`Content-Disposition` (the `paradata/*` path never matches a `distribuce` entry).
- file-distributions-D05: `soubor_distri` facet missing for Knihovna 3D and PAS.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-29 | `digiarchiv-test` (partial implementation of #693, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
