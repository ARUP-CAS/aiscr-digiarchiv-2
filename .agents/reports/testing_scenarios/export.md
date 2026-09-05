# Export — testing scenario

**Key:** export (feature)
**Scope:** the export feature — the paginated export listing and table downloads (CSV, XLSX, XML, JSON) on the export and map-export surfaces, record URL columns, map export columns, server-side row limits, and element-level field protection in downloads. Driven by ARUP-CAS/aiscr-digiarchiv-2#148 (milestone v4.1.0).
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — Angular frontend with the API under `/api/`. API hosts referenced by URL columns: `https://api-test.aiscr.cz/id/` and `https://amcr-test.aiscr.cz/id/` (per-environment, from the client config `choiceApi`).
- Production: `https://digiarchiv.aiscr.cz/` — **not compared** in verifications of unreleased milestone work; the comparison is meaningful only once the feature is deployed there.
- Export pages (client-rendered): `/export?entity=<entity>&sort=<field> <dir>&page=<n>&lang=cs` and `/export-mapa?...` (map export).
- Export API: `ExportServlet` at `/api/exp` — parameters: `entity`, `format` (`csv`|`xlsx`|`xml`|`json`), `rows` (server-capped at 1000), `page` (zero-based), `sort`, `lang`, `mapa=true` (map export), `geometrie` (`GeoJSON`|`GML`|anything else → WKT), plus the search filter set (`q`, `f_*` fields, `loc_rpt`).
- Map display search API: `SearchServlet` at `/api/search/query` — serves the map including heatmap facets (`facet.heatmap` on `loc_rpt_<pristupnost>`). With `mapa=true` the row count is pinned to `mapOptions.docsForMarker` (marker display, by design).
- Client config: `/api/config` (`ConfigServlet`) — `exportFields` per entity, `choiceApi` URL columns, `mapOptions`, `sorts`, `serverUrl`, `exportRowsLimit`.

### Architecture and implementation facts

- `ExportServlet.processRequest` dispatches on `format`; each entity's `EntitySearcher.export()` runs the normal search query (`SolrSearcher.addCommonParams`) and then `SolrSearcher.addExportParams`, which overrides the field list (`handle` + `choiceApi` concat URLs + `exportFields` + `pristupnost`), forces the sort tiebreaker `ident_cely asc`, and caps rows at `min(rows, 1000)`.
- Column model asymmetry: the CSV/XLSX **header** comes from `SolrSearcher.getExportField(entity, "label")` — `handle` + `choiceApi` labels + `exportFields` labels, **not** hidden-aware and **not** map-aware. The **data** comes from `SolrSearcher.getExportFieldsExt(entity, isMap)`, which skips `hidden: true` entries and, for `mapa=true`, replaces the URL columns with the PIAN columns.
- XLSX is written with Apache POI (XSSF), all cells as strings; CSV quoting handles embedded commas, quotes, and newlines.
- Fields marked `translated: true` in `exportFields` pass through `I18n.translate((String) val)` — a JSONArray value (multivalued Solr field) crashes with `ClassCastException`.
- Element-level protection: per-entity `filter()` strips chranene_udaje-derived fields (katastr, lokalizace, …) from docs whose `pristupnost` exceeds the session's access level; an anonymous session runs at level A. Translated `pristupnost` values: A→anonym, B→badatel, C→archeolog, D→archivář.
- Map export (`mapa=true`) data columns: `pian_ident_cely`, `pian_presnost`, `pian_typ`, `pian_zm10`, `pian_wgs84` (definition point, `lat : lng`), `geometrie` (WKT by default; GeoJSON or GML per the `geometrie` parameter) + the entity's `exportFields`. URL columns are intentionally absent in map mode.
- The map search display still pins `rows` to `mapOptions.docsForMarker` in `addCommonParams` (the `Math.max(docsForMarker, rows)` variant is commented out); the export path overrides rows downstream in `addExportParams`, so the export row count is respected independently of that pin.

### Feature or entity model

- Exportable entities (client config `entities`): dokument, projekt, akce, lokalita, samostatny_nalez, knihovna_3d, komponenta.
- akce and lokalita `exportFields` begin with a `hidden: true` `az_dj_pian` entry; komponenta carries two `hidden: true` `katastr` entries (per-origin katastr variants). Hidden entries are excluded from export data but still appear in the CSV/XLSX header (defect export-D01).
- samostatny_nalez export includes `obdobi` (from `samostatny_nalez_obdobi`, translated) plus nález fields (druh_nalezu, kategorie_nalezu, specifikace, pocet).
- komponenta export covers the field list requested in #148: ident_cely, katastr, dalsi_katastry, obdobi, presna_datace (komponenta_presna_datace), jistota (komponenta_jistota), areal (f_areal), aktivita (komponenta_aktivita) + the URL columns. The optional aggregation of nálezy per point was not implemented (explicitly waivable in the issue discussion).

### Discovery recipes

1. Fresh export candidates (any entity): `GET /api/exp?entity=<entity>&format=csv&rows=<N>&lang=cs&sort=datestamp+desc` — the first N rows of the listing.
2. Restricted records for protection checks: in any CSV export, scan the `pristupnost` column for values other than `anonym` (badatel / archeolog / archivář); those rows must show empty `katastr` and `lokalizace` when fetched by an anonymous session.
3. Map export candidates: `GET /api/exp?entity=akce&mapa=true&geometrie=WKT&format=csv&rows=<N>&sort=ident_cely+asc` — every data row carries the PIAN columns.
4. Heatmap regression check: `GET /api/search/query?entity=akce&mapa=true&onlyFacets=true&loc_rpt=<lat1>,<lng1>,<lat2>,<lng2>` over a known-density bounding box — the response must contain `facet_counts.facet_heatmaps.loc_rpt.counts_ints2D`.
5. knihovna_3d export probe: `GET /api/exp?entity=knihovna_3d&format=json&rows=1` with any sort — exercises the translated multivalued fields.
6. XLSX structure inspection: download `GET /api/exp?entity=<entity>&format=xlsx&rows=<N>&lang=cs&sort=ident_cely+asc` and unpack the workbook — an XLSX is a ZIP. Read `xl/worksheets/sheet1.xml` for cell placement (the `r="D2"` references make header/data misalignment directly visible) and `xl/sharedStrings.xml` for the shared-string values.

### Verification commands

Identifier-free, re-runnable against the test instance (rate-limit gently; anonymous session level A):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&format=csv&rows=3&lang=cs&sort=datestamp+desc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&format=json&rows=2&page=1&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&format=json&rows=20000&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=akce&mapa=true&geometrie=WKT&format=csv&rows=2&lang=cs&sort=ident_cely+asc"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&mapa=true&onlyFacets=true&loc_rpt=50.16475722972232,15.005021179456852,50.19753009357244,15.084183013384107"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/exp?entity=knihovna_3d&format=json&rows=1&lang=cs&sort=ident_cely+asc"
```

## Current verification (2026-09-05, digiarchiv-test.aiscr.cz, dev-branch v4.1.0 milestone build, anonymous session)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Export pagination — `page`/`rows` respected, pages disjoint and ordered | verified (page=0 vs page=1, `ident_cely asc`) |
| Server-side row cap | verified — `rows=20000` returns exactly 1000 documents |
| CSV download (akce, dokument, projekt, samostatny_nalez, komponenta) | verified, with the header shift of export-D01 |
| XML and JSON downloads translated per `exportFields`, mutually consistent | verified |
| XLSX download | verified — HTTP 200, `application/xlsx`; POI XSSF workbook unpacked and read at cell level (`xl/worksheets/sheet1.xml` + `xl/sharedStrings.xml`); the export-D01 header shift confirmed in the cell layout |
| URL columns (`handle`, `api_prefix`, `show_in_AMCR`) in non-map downloads, per-environment URLs | verified (CSV, XML, JSON) |
| Map export — PIAN columns (`pian_ident_cely`, `pian_presnost`, `pian_typ`, `pian_zm10`, `pian_wgs84`) + `geometrie` in download data | verified (CSV, JSON, XLSX at cell level); header mismatch is export-D02 |
| Map display heatmap (regression from the issue's last comment: "Solr nevraci heatmap") | verified fixed — `facet_heatmaps.loc_rpt.counts_ints2D` returns a 7×8 grid over the issue's Prague bounding box (331 akce) |
| Element-level protection in anonymous downloads (katastr/lokalizace empty for badatel/archeolog/archivář records) | verified (akce and samostatny_nalez CSV rows) |
| Empty-page fix for dokument and projekt | verified — non-empty exports |
| Empty-page fix for knihovna_3d | **failed** — export returns HTTP 500 for every format (export-D05) |
| samostatny_nalez `obdobi` populated (earlier missing-data defect) | verified |
| komponenta export field list per #148 | verified present; the optional nálezy aggregation was not implemented (waived) |
| UI paginator on `/export` (10/100/1000, controls above and below, lister matching the standard result list) | verified (maintainer-assisted browser check, 2026-09-05) |
| UI paginator on `/export-mapa` | **failed** — paginator shows 0 pages at 10 and 100 rows alike; 1000 rows per page fails with a browser `TypeError: Failed to fetch` (export-D07) |
| Credentialed (badatel) export behaviour — login, download buttons, CSV downloads | verified (maintainer-assisted browser check, 2026-09-05) |
| Credentialed element-level protection — restricted fields populated at the session's own access level | verified — a badatel-level akce record (recipe 2 candidate) shows katastr/lokalizace to a logged-in badatel while staying empty for anonymous |

### Known defects

- export-D01: **CSV/XLSX header includes hidden `exportFields` entries, shifting every data column.** The header builder `SolrSearcher.getExportField` does not skip `hidden: true` entries while the data builder `getExportFieldsExt` does: akce and lokalita headers carry an extra `az_dj_pian` column (18 header columns vs 17 data values), komponenta two extra `katastr` columns (14 vs 12). In a spreadsheet every column from `ident_cely` onward sits one (resp. two) header to the left of its value. JSON/XML are unaffected (self-describing keys). Verified on 2026-09-05 against the deployed config, live CSV output, and the XLSX cell layout: header row spans A1:R1 (18 cells), data rows stop at column Q (17 values), so the `ident_cely` value lands under the `az_dj_pian` header and the `pristupnost` value under `akce_je_nz`.
- export-D02: **Map export CSV/XLSX uses the non-map header while data rows carry the map columns.** `getExportField` has no map awareness, so a `mapa=true` CSV ships the header `handle,api_prefix,show_in_AMCR,az_dj_pian,ident_cely,...` while its data rows start `pian_ident_cely,pian_presnost,pian_typ,pian_zm10,pian_wgs84,geometrie,...` — a complete header/data mismatch in map downloads. JSON/XML map downloads are self-consistent. Verified on 2026-09-05, including the XLSX cell layout: the map workbook header is the identical non-map 18-column list while its data rows carry 20 map columns (6 PIAN columns + `geometrie` + the entity's exportFields).
- export-D03: **`/api/exp` without a `format` parameter returns HTTP 500** (`NullPointerException` in the `switch (format)` on a null string) instead of the raw-JSON default case. Verified twice on 2026-09-05.
- export-D04: **`/api/exp?mapa=true` without a `geometrie` parameter returns HTTP 500** (`NullPointerException` in `processMap`'s `switch` on the null `geometrie`) instead of the WKT default case. Verified on 2026-09-05.
- export-D05: **knihovna_3d export is broken in all formats** — HTTP 500 `ClassCastException: JSONArray cannot be cast to String`. The entity's `f_obdobi` and `f_areal` are marked `translated: true` and are always JSONArray values (multivalued Solr fields), so `I18n.translate((String) val)` fails for every result set (696 records). Also latent for any other entity with a translated multivalued export field. Verified on 2026-09-05 across CSV/JSON, two sorts. From the export page the download fails silently — the UI does not surface the server 500 (maintainer-assisted browser check).
- export-D06 (cosmetic): **multivalued export fields render as raw JSON arrays** — komponenta `okres` shows `["Semily"]`, dokument `dokument_autor` shows `["Hlubek, Lukáš"]`. Verified on 2026-09-05.
- export-D07: **Map export page (`/export-mapa`) UI is broken** — the paginator shows 0 pages at every row count (10 and 100 alike), and selecting 1000 rows per page fails with a browser `TypeError: Failed to fetch`. The backend is healthy at exactly those parameters: `/api/exp?entity=akce&mapa=true&geometrie=WKT&format=json&rows=1000` returns HTTP 200 with 978 KB in ~2 s, and `/api/search/query?entity=akce&mapa=true&rows=1000` returns HTTP 200 with 2.5 MB in ~2.6 s including `response.numFound` (171870). The `/api/exp` JSON response (map and non-map alike) is a bare array with no total count, so a paginator reading a total from it has nothing to count; the 1000-row browser failure is client-side or intermediate-layer and needs browser devtools to pin down. Verified on 2026-09-05 (maintainer-assisted browser checks plus the server-side probes above).

### Corrections made during this run

- The CSV header/data misalignment was first read as a possible escaping regression (the issue's original XLSX complaint); after fetching the deployed client config the cause was corrected to hidden-entry inclusion in the header builder (export-D01). The CSV quoting itself is correct.
- The map `rows` behaviour was first suspected to still be pinned to `docsForMarker` (the `Math.max` variant remains commented out in `addCommonParams`); corrected after empirical probing — the export path overrides rows in `addExportParams`, and `rows=2` map exports returned 2 rows. The pin affects only the map search display, by design.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-05 | digiarchiv-test.aiscr.cz (dev branch, v4.1.0 milestone build) | First verification: regression pass over #148 fixes (URL columns, map columns, rows handling, XML consistency, rows cap, heatmap) all confirmed fixed except as noted; six defects recorded (export-D01 … export-D06); D01/D02 confirmed at XLSX cell level by unpacking the workbooks. |
| 2026-09-05 | digiarchiv-test.aiscr.cz (same build) | Maintainer-assisted checks completed: `/export` paginator verified; `/export-mapa` paginator defective (export-D07 minted, backend probes attached); credentialed (badatel) export behaviour verified. Adjacent out-of-scope observation: an authenticated session below a record's access level gets HTTP 500 on the `/id/` record page instead of a clean denial (anonymous gets 403) — a record-page defect, not export; reported in the issue comment. |
