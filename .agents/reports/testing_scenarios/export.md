# Export — testing scenario

**Key:** export (feature)
**Scope:** the export feature — the paginated export listing and table downloads (CSV, XLSX, XML, JSON) on the export and map-export surfaces, record URL columns, map export columns, server-side row limits, and element-level field protection in downloads. Driven by ARUP-CAS/aiscr-digiarchiv-2#148 (milestone v4.1.0).
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — Angular frontend with the API under `/api/`. API hosts referenced by URL columns: `https://api-test.aiscr.cz/id/` and `https://amcr-test.aiscr.cz/id/` (per-environment, from the client config `choiceApi`).
- Production: `https://digiarchiv.aiscr.cz/` — **not compared** in verifications of unreleased milestone work; the comparison is meaningful only once the feature is deployed there.
- Build identification for the verification log: the footer displays the version and the Angular main bundle (`main-*.js`, referenced from `/`) carries `hash:"g<short-sha>"` and a `v<describe>` version string (e.g. `v4.0.3-211-g467f7386`); record both when naming what was verified.
- Export pages (client-rendered): `/export?entity=<entity>&sort=<field> <dir>&page=<n>&lang=cs` and `/export-mapa?...` (map export). The pages' **listing** queries go to `SearchServlet` actions `/api/search/export` (non-map) and `/api/search/export_mapa` (map) — both return Solr-shaped responses with `response.numFound`, which the page paginators count from. The **download** links go to `ExportServlet` at `/api/exp` with the current query params plus `format`.
- Export API: `ExportServlet` at `/api/exp` — parameters: `entity`, `format` (`csv`|`xlsx`|`xml`|`json`, absent → raw JSON), `rows` (server-capped at 1000), `page` (zero-based), `sort`, `lang`, `mapa=true` (map export), `geometrie` (`GeoJSON`|`GML`|anything else → WKT), plus the search filter set (`q`, `f_*` fields, `loc_rpt`).
- Map display search API: `SearchServlet` at `/api/search/query` — serves the map including heatmap facets (`facet.heatmap` on `loc_rpt_<pristupnost>`).
- Client config: `/api/config` (`ConfigServlet`) — `exportFields` per entity, `choiceApi` URL columns, `mapOptions` (incl. `docsForMarker`), `sorts`, `serverUrl`, `exportRowsLimit` (100 on test).

### Architecture and implementation facts

- `ExportServlet.processRequest` dispatches on `format` (null-guarded to the raw-JSON default) and `geometrie` (null-guarded to the WKT default); each entity's `EntitySearcher.export()` runs the normal search query (`SolrSearcher.addCommonParams`) and then `SolrSearcher.addExportParams`, which overrides the field list (`handle` + `choiceApi` concat URLs + `exportFields` + `pristupnost`), forces the sort tiebreaker `ident_cely asc`, and caps rows at `min(rows, 1000)`.
- Column model: the CSV/XLSX **header** comes from `SolrSearcher.getExportField(entity, "label", isMap)` — `handle` + `choiceApi` labels + `exportFields` labels, skipping `hidden: true` entries, and for `mapa=true` replacing the URL columns with the PIAN columns. The **data** comes from `SolrSearcher.getExportFieldsExt(entity, isMap)`, with the same hidden- and map-awareness, so header and data agree.
- `exportFields` entries with `byPath: true` (e.g. knihovna_3d `f_obdobi`, `f_areal` — `dokument_cast:[json]` sources) are resolved by `SolrSearcher.processExportDocs`/`readValuesByPath` into single string values before translation; multivalued by-path results are joined with `, `.
- Multivalue rendering per format (behaviour verified on the deployed build `v4.0.3-211-g467f7386`): **CSV** writes array-derived fields wrapped in double quotes — valid CSV field quoting, so a parser recovers the bare joined value (`Semily`, `Frolík, Jan`); **XLSX** cells carry the same joined values wrapped in literal double quotes (`"Karlovy Vary"`), visible to the user (residual cosmetic defect export-D06); **XML** renders bare joined values; **JSON** keeps native arrays. Scalar fields are CSV-quoted only when they contain commas.
- Map export (`mapa=true`) data columns: `pian_ident_cely`, `pian_presnost`, `pian_typ`, `pian_zm10`, `pian_wgs84` (definition point, `lat : lng`), `geometrie` (WKT by default; GeoJSON or GML per the `geometrie` parameter) + the entity's `exportFields`. URL columns are intentionally absent in map mode. `processMap` explodes each document into one row per linked PIAN; knihovna_3d and samostatny_nalez carry their own geometry (`hasPian=false`); the deployed build resolves PIANs for dokument too (via its linked records), so the dokument map export returns rows with the PIAN columns populated.
- Map listing stride: `SolrSearcher.addCommonParams` computes `start = page × rows` also when `mapa=true`, and `addExportParams` honours the requested rows (≤1000) — the map listing endpoint and the `/api/exp` map download therefore both serve the window implied by the paginator's page and rows choice.
- Map-export page load cost: the default entity branch issues one `/api/search/geometrie` request **per PIAN row** after the listing response (a HAR recording of an initial `rows=100` akce load showed ~240 geometry requests), each carrying a literal `format=undefined` query value — tolerated by the server's `switch` default (WKT), but the page's request count scales with the row×PIAN product.
- `export-mapa.component` (Angular): the default entity branch iterates `doc.pian` per document and resolves geometry per PIAN via `/api/search/geometrie`; the whole table **and paginator** render inside `@if (docs().length > 0)` — an entity whose export rows are empty would render a completely empty page (observed for dokument while export-D08 was open).
- XLSX is written with Apache POI (XSSF), all cells as strings; CSV quoting handles embedded commas, quotes, and newlines.
- Fields marked `translated: true` in `exportFields` pass through `I18n.translate((String) val)`; with `byPath` extraction the values are single strings, so the historical JSONArray ClassCastException (export-D05) is gone.
- Element-level protection: per-entity `filter()` strips chranene_udaje-derived fields (katastr, lokalizace, …) from docs whose `pristupnost` exceeds the session's access level; an anonymous session runs at level A (okres is not protected and stays populated). Translated `pristupnost` values: A→anonym, B→badatel, C→archeolog, D→archivář.

### Feature or entity model

- Exportable entities (client config `entities`): dokument, projekt, akce, lokalita, samostatny_nalez, knihovna_3d, komponenta.
- akce and lokalita `exportFields` begin with a `hidden: true` `az_dj_pian` entry; komponenta carries two `hidden: true` `katastr` entries (per-origin katastr variants). Hidden entries are excluded from export data and headers alike.
- samostatny_nalez export includes `obdobi` (from `samostatny_nalez_obdobi`, translated) plus nález fields (druh_nalezu, kategorie_nalezu, specifikace, pocet).
- komponenta export covers the field list requested in #148: ident_cely, katastr, dalsi_katastry, obdobi, presna_datace (komponenta_presna_datace), jistota (komponenta_jistota), areal (f_areal), aktivita (komponenta_aktivita) + the URL columns. The optional aggregation of nálezy per point was not implemented (explicitly waivable in the issue discussion).

### Discovery recipes

1. Fresh export candidates (any entity): `GET /api/exp?entity=<entity>&format=csv&rows=<N>&lang=cs&sort=datestamp+desc` — the first N rows of the listing.
2. Restricted records for protection checks: in any CSV export, scan the `pristupnost` column for values other than `anonym` (badatel / archeolog / archivář); those rows must show empty `katastr` and `lokalizace` when fetched by an anonymous session.
3. Map export candidates: `GET /api/exp?entity=akce&mapa=true&geometrie=WKT&format=csv&rows=<N>&sort=ident_cely+asc` — every data row carries the PIAN columns.
4. Heatmap regression check: `GET /api/search/query?entity=akce&mapa=true&onlyFacets=true&loc_rpt=<lat1>,<lng1>,<lat2>,<lng2>` over a known-density bounding box — the response must contain `facet_counts.facet_heatmaps.loc_rpt.counts_ints2D`.
5. knihovna_3d export probe: `GET /api/exp?entity=knihovna_3d&format=json&rows=1` with any sort — exercises the translated byPath fields.
6. XLSX structure inspection: download `GET /api/exp?entity=<entity>&format=xlsx&rows=<N>&lang=cs&sort=ident_cely+asc` and unpack the workbook — an XLSX is a ZIP. Read `xl/worksheets/sheet1.xml` for cell placement (the `r="D2"` references make header/data misalignment directly visible) and `xl/sharedStrings.xml` for the shared-string values (multivalue rendering defects are visible here as quoted or bracketed strings). This needs a local archive tool; where the session's command floor refuses one, an Excel check by the maintainer covers the user-visible aspect.
7. Map listing stride comparison: request the same `page=1` twice on `/api/search/export_mapa` with different `rows` values (`rows=10`, `rows=500`); if both responses start at the same record, the page stride ignores the requested rows — with `start = page × rows` they must start at record #11 and #501 respectively.
8. Empty-map-export candidates: `GET /api/exp?entity=<entity>&mapa=true&geometrie=WKT&format=json&rows=3` — an entity whose rows resolve no PIAN returns `[]` and its map-export page renders empty.

### Verification commands

Identifier-free, re-runnable against the test instance (rate-limit gently; anonymous session level A):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&format=csv&rows=3&lang=cs&sort=datestamp+desc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&format=json&rows=2&page=1&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&format=json&rows=20000&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&mapa=true&geometrie=WKT&format=csv&rows=2&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&mapa=true&onlyFacets=true&loc_rpt=50.16475722972232,15.005021179456852,50.19753009357244,15.084183013384107"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=knihovna_3d&format=json&rows=1&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/export_mapa?entity=akce&mapa=true&isExport=true&noFacets=true&noStats=true&rows=10&page=1&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/export?entity=akce&isExport=true&noFacets=true&noStats=true&rows=2&page=1&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=dokument&mapa=true&geometrie=WKT&format=json&rows=3&lang=cs&sort=ident_cely+asc"
```

## Current verification (2026-09-10, digiarchiv-test.aiscr.cz, build `v4.0.3-211-g467f7386`, anonymous session plus maintainer-assisted browser and Excel checks)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Export pagination (non-map) — `page`/`rows` respected, pages disjoint and ordered (`/api/exp` and `/api/search/export`) | verified (page=0 vs page=1, `ident_cely asc`; `/api/search/export` page=1, rows=2 → `start: 2`) |
| Server-side row cap | verified — `rows=20000` returns exactly 1000 documents |
| CSV header/data alignment — akce 17/17, komponenta 12/12, dokument 15/15, samostatny_nalez 19/19, knihovna_3d 14/14 (hidden entries excluded) | verified — export-D01 stays fixed |
| XLSX header/data alignment | verified (maintainer-assisted Excel check — no misalignment reported) — export-D01 XLSX aspect stays fixed |
| Map export columns (`pian_ident_cely`, `pian_presnost`, `pian_typ`, `pian_zm10`, `pian_wgs84`, `geometrie`) in download data and header, aligned | verified (CSV, 20 columns; header and data agree) — export-D02 stays fixed |
| `/api/exp` without `format` | verified — raw JSON, HTTP 200 — export-D03 stays fixed |
| `/api/exp?mapa=true` without `geometrie` | verified — WKT default, HTTP 200 — export-D04 stays fixed |
| knihovna_3d export, translated `f_obdobi`/`f_areal` (`byPath` extraction) | verified (CSV, JSON, XML) — export-D05 stays fixed |
| Map export pagination stride | verified — `/api/search/export_mapa` `rows=10&page=1` → `start: 10` (page=0 → `start: 0`); `rows=500&page=1` starts at the 501st record (correct); `/api/exp?mapa=true` download `rows=2&page=1` starts at the 3rd record; UI page 2 with 10 rows/page shows records 11–20 (maintainer-assisted browser check) — **export-D07 fixed** |
| dokument map export | verified — `/api/exp?entity=dokument&mapa=true` returns rows with populated `pian_ident_cely`/`pian_wgs84`/`geometrie`; `/export-mapa?entity=dokument` renders table and paginator (maintainer-assisted browser check) — **export-D08 fixed** |
| XLSX multivalue rendering | changed — raw JSON arrays are gone from cells; joined values now appear wrapped in literal double quotes (okres `"Karlovy Vary"`, `dokument_autor` likewise — maintainer-assisted Excel check) — residual cosmetic defect, see export-D06 |
| CSV multivalue rendering | verified — array-derived fields quoted (valid CSV; values parse bare: komponenta `okres` `Semily`, dokument `dokument_autor` `Frolík, Jan`) |
| XML and JSON downloads translated per `exportFields`, mutually consistent | verified (knihovna_3d XML/JSON; `dokument_autor` bare in XML, native array in JSON) |
| URL columns (`handle`, `api_prefix`, `show_in_AMCR`) in non-map downloads, per-environment URLs | verified (CSV, XML, JSON) |
| Map display heatmap (regression from the issue: "Solr nevraci heatmap") | verified — `facet_heatmaps.loc_rpt.counts_ints2D` present over the issue's Prague bounding box |
| Element-level protection in anonymous downloads (katastr/lokalizace empty for badatel/archeolog/archivář records) | verified (akce badatel-level row; samostatny_nalez archivář rows) |
| samostatny_nalez `obdobi` populated (earlier missing-data defect) | verified |
| komponenta export field list per #148 | verified present; the optional nálezy aggregation was not implemented (waived) |
| XLSX opened and read in Excel (cosmetic display) | verified (maintainer-assisted check, 2026-09-10) — workbooks open cleanly, header aligned; the export-D06 quoting is visible in cells |
| XLSX cell-level unpacking (ZIP inspection) | not performed — blocked by the session's command floor; the maintainer's Excel check covers the user-visible aspect |

### Known defects

- export-D06 (changed, still open — cosmetic): **XLSX cells render joined multivalued fields wrapped in literal double quotes** — komponenta `okres` shows `"Karlovy Vary"` and `dokument_autor` shows quoted values in the workbook, instead of the bare joined values the other formats carry. The original raw-array form (`["Semily"]`) is gone from all formats. CSV renders the same fields with valid CSV quoting (parsers recover the bare value), XML renders bare joined values, JSON keeps native arrays. Verified on 2026-09-10 by the maintainer in Excel (komponenta workbook; `dokument_autor` likewise).

### Pre-existing observations

- HandleServlet record-page denial (out of scope for the export feature, first observed 2026-09-05): an authenticated session below a record's access level got HTTP 500 on the `/id/` record page while anonymous users got a clean 403-style denial. **Verified fixed on 2026-09-10** — maintainer-assisted check with a badatel-level account: clean denial / proper page. Retained here for provenance; the record-page behaviour is owned outside this scenario.

### Corrections made during this run

- None. (The session's first reading of the quoted komponenta `okres` value in raw CSV text was ambiguous between valid CSV field quoting and a return of raw-array rendering; the maintainer's Excel check resolved it as literal quotes around joined values — recorded under export-D06 — before any artefact was written.)

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-05 | digiarchiv-test.aiscr.cz (dev branch, v4.1.0 milestone build) | First verification: regression pass over #148 fixes (URL columns, map columns, rows handling, XML consistency, rows cap, heatmap) all confirmed fixed except as noted; six defects recorded (export-D01 … export-D06); D01/D02 confirmed at XLSX cell level by unpacking the workbooks. |
| 2026-09-05 | digiarchiv-test.aiscr.cz (same build) | Maintainer-assisted checks completed: `/export` paginator verified; `/export-mapa` paginator defective (export-D07 minted, backend probes attached); credentialed (badatel) export behaviour verified. Adjacent out-of-scope observation: an authenticated session below a record's access level gets HTTP 500 on the `/id/` record page instead of a clean denial (anonymous gets 403) — a record-page defect, not export; reported in the issue comment. |
| 2026-09-10 | digiarchiv-test.aiscr.cz (v4.1.0 build, frontend hash g714e9423) | Regression pass after fix work: export-D01–D05 verified fixed (CSV/XLSX cell level, four formats, source mechanisms confirmed); export-D06 changed (CSV half fixed, XLSX half open); export-D07 changed (page count fixed via `/api/search/export_mapa`, stride defect recorded in reduced form); export-D08 minted (dokument map export empty); HandleServlet out-of-scope observation verified fixed (maintainer-assisted, badatel). Maintainer-assisted checks completed in-session: `/export-mapa` paginator mechanics (navigate + refetch; HAR analysis), XLSX display in Excel, credentialed (badatel) export and element-level protection re-verified. |
| 2026-09-10 | digiarchiv-test.aiscr.cz (build `v4.0.3-211-g467f7386`) | Second regression pass the same day, after further fix work: export-D07 verified fixed (listing `start = page × rows`, download window follows requested rows, UI page 2 continues from record 11); export-D08 verified fixed (dokument map export returns rows with PIAN columns; page renders); export-D06 changed again (raw arrays gone; joined values wrapped in literal quotes in XLSX cells — cosmetic residual, stays open); full matrix re-verified. XLSX cell-level unpacking was blocked by the session's command floor, so XLSX was verified through the maintainer's Excel check instead. |
