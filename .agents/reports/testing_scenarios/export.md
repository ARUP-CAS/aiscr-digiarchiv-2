# Export — testing scenario

**Key:** export (feature)
**Scope:** the export feature — the paginated export listing and table downloads (CSV, XLSX, XML, JSON) on the export and map-export surfaces, record URL columns, map export columns, server-side row limits, and element-level field protection in downloads. Driven by ARUP-CAS/aiscr-digiarchiv-2#148 (milestone v4.1.0).
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — Angular frontend with the API under `/api/`. API hosts referenced by URL columns: `https://api-test.aiscr.cz/id/` and `https://amcr-test.aiscr.cz/id/` (per-environment, from the client config `choiceApi`).
- Production: `https://digiarchiv.aiscr.cz/` — **not compared** in verifications of unreleased milestone work; the comparison is meaningful only once the feature is deployed there.
- Build identification for the verification log: the footer displays the version and the Angular main bundle (`main-*.js`, referenced from `/`) carries `hash:"g<short-sha>"` and a `v<describe>` version string (e.g. `v4.0.3-215-g20163f8`); record both when naming what was verified. Where the bundle is a single minified line, `Select-String -Quiet` with narrowed patterns identifies the version without dumping the line.
- Export pages (client-rendered): `/export?entity=<entity>&sort=<field> <dir>&page=<n>&lang=cs` and `/export-mapa?...` (map export). The pages' **listing** queries go to `SearchServlet` actions `/api/search/export` (non-map) and `/api/search/export_mapa` (map) — both return Solr-shaped responses with `response.numFound`, which the page paginators count from. The **download** links go to `ExportServlet` at `/api/exp` with the current query params plus `format`.
- Export API: `ExportServlet` at `/api/exp` — parameters: `entity`, `format` (`csv`|`xlsx`|`xml`|`json`, absent → raw JSON), `rows` (server-capped at 1000), `page` (zero-based), `sort`, `lang`, `mapa=true` (map export), `geometrie` (`GeoJSON`|`GML`|anything else → WKT), plus the search filter set (`q`, `f_*` fields, `loc_rpt`).
- Map display search API: `SearchServlet` at `/api/search/query` — serves the map including heatmap facets (`facet.heatmap` on `loc_rpt_<pristupnost>`).
- Client config: `/api/config` (`ConfigServlet`) — `exportFields` per entity, `choiceApi` URL columns, `mapOptions` (incl. `docsForMarker`), `sorts`, `serverUrl`, `exportRowsLimit` (100 on test).
- Login for credentialed probes: `GET /user/login?user=<email>&pwd=<password>` (`LoginServlet`) returns the user JSON and sets a `JSESSIONID` session cookie; the session rides subsequent API requests via the cookie or Tomcat `;jsessionid=<id>` URL rewriting, so command-line probes can verify credentialed behaviour. Maintainer-supplied test accounts exist at the three non-anonymous levels: badatel (B), archeolog (C), archivář (D) — credentials are maintained by the maintainer and are deliberately not recorded here.

### Architecture and implementation facts

- `ExportServlet.processRequest` dispatches on `format` (null-guarded to the raw-JSON default) and `geometrie` (null-guarded to the WKT default); each entity's `EntitySearcher.export()` runs the normal search query (`SolrSearcher.addCommonParams`) and then `SolrSearcher.addExportParams`, which overrides the field list (`handle` + `choiceApi` concat URLs + `exportFields` + `pristupnost`), forces the sort tiebreaker `ident_cely asc`, and caps rows at `min(rows, 1000)`.
- Column model: the CSV/XLSX **header** comes from `SolrSearcher.getExportField(entity, "label", isMap)` — `handle` + `choiceApi` labels + `exportFields` labels, skipping `hidden: true` entries, and for `mapa=true` replacing the URL columns with the PIAN columns. The **data** comes from `SolrSearcher.getExportFieldsExt(entity, isMap)`, with the same hidden- and map-awareness, so header and data agree.
- `exportFields` entries with `byPath: true` (e.g. knihovna_3d `f_obdobi`, `f_areal` — `dokument_cast:[json]` sources) are resolved by `SolrSearcher.processExportDocs`/`readValuesByPath` into single string values before translation; multivalued by-path results are joined with `, `.
- Multivalue rendering per format (behaviour verified on the deployed build `v4.0.3-215-g20163f8`): array-derived fields are joined with `"; "`. **CSV** wraps the joined value in double quotes when it contains the delimiter (comma), a newline, or a leading quote — valid CSV field quoting, so a parser recovers the bare joined value (`"Běhoun, Petr; Těsnohlídek, Jakub"` parses to `Běhoun, Petr; Těsnohlídek, Jakub`); **XLSX** cells carry bare joined values with no quote wrapping; **XML** renders one repeated element per array value (`<dokument_autor>...</dokument_autor><dokument_autor>...</dokument_autor>`); **JSON** keeps native arrays. Scalar fields are CSV-quoted only when they contain commas.
- Map export (`mapa=true`) data columns: `pian_ident_cely`, `pian_presnost`, `pian_typ`, `pian_zm10`, `pian_wgs84` (definition point, `lat : lng`), `geometrie` (WKT by default; GeoJSON or GML per the `geometrie` parameter) + the entity's `exportFields`. URL columns are intentionally absent in map mode. `processMap` explodes each document into one row per linked PIAN; knihovna_3d and samostatny_nalez carry their own geometry (`hasPian=false`); the deployed build resolves PIANs for dokument too (via its linked records), so the dokument map export returns rows with the PIAN columns populated.
- Map listing stride: `SolrSearcher.addCommonParams` computes `start = page × rows` also when `mapa=true`, and `addExportParams` honours the requested rows (≤1000) — the map listing endpoint and the `/api/exp` map download therefore both serve the window implied by the paginator's page and rows choice.
- Map-export page load cost: the default entity branch issues one `/api/search/geometrie` request **per PIAN row** after the listing response (a HAR recording of an initial `rows=100` akce load showed ~240 geometry requests), each carrying a literal `format=undefined` query value — tolerated by the server's `switch` default (WKT), but the page's request count scales with the row×PIAN product.
- `export-mapa.component` (Angular): the default entity branch iterates `doc.pian` per document and resolves geometry per PIAN via `/api/search/geometrie`; the whole table **and paginator** render inside `@if (docs().length > 0)` — an entity whose export rows are empty would render a completely empty page (observed for dokument while export-D08 was open).
- XLSX is written with Apache POI (XSSF), all cells as strings; CSV quoting handles embedded commas, quotes, and newlines.
- Fields marked `translated: true` in `exportFields` pass through `I18n.translate((String) val)`; with `byPath` extraction the values are single strings, so the historical JSONArray ClassCastException (export-D05) is gone.
- Element-level protection: per-entity `filter()` strips chranene_udaje-derived fields (katastr, lokalizace, …) from docs whose `pristupnost` exceeds the session's access level; an anonymous session runs at level A (okres is not protected and stays populated). Translated `pristupnost` values: A→anonym, B→badatel, C→archeolog, D→archivář. Graded fill-in verified at all three levels: a badatel session fills badatel-level records, an archeolog session fills badatel- and archeolog-level records but withholds archivář-level ones, an archivář session fills all three.

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
6. XLSX structure inspection: download `GET /api/exp?entity=<entity>&format=xlsx&rows=<N>&lang=cs&sort=ident_cely+asc` and unpack the workbook — an XLSX is a ZIP. Read `xl/worksheets/sheet1.xml` for cell placement (the `r="D2"` references and the `dimension ref` make header/data misalignment directly visible) and `xl/sharedStrings.xml` for the shared-string values (multivalue rendering defects are visible here as quoted or bracketed strings). This run's command floor admits the download-to-cache and unpack shapes, so the inspection is in-session; where a session's floor refuses them, an Excel check by the maintainer covers the user-visible aspect.
7. Map listing stride comparison: request the same `page=1` twice on `/api/search/export_mapa` with different `rows` values (`rows=10`, `rows=500`); if both responses start at the same record, the page stride ignores the requested rows — with `start = page × rows` they must start at record #11 and #501 respectively.
8. Empty-map-export candidates: `GET /api/exp?entity=<entity>&mapa=true&geometrie=WKT&format=json&rows=3` — an entity whose rows resolve no PIAN returns `[]` and its map-export page renders empty.
9. Multi-valued field candidates: fetch a dokument CSV sorted by `datestamp desc` with `rows=30` and scan the `dokument_autor` column for `"; "` — nálezové zprávy frequently carry multiple authors; the row cites the discovery recipe (sort + rows), not a fixed record.

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

## Current verification (2026-09-11, digiarchiv-test.aiscr.cz, build `v4.0.3-215-g20163f8`, anonymous session plus credentialed probes at badatel/archeolog/archivář levels)

Regression pass on the known scenario: four dev commits since the previously verified build `v4.0.3-211-g467f7386`; frontend bundle changed (hash `g20163f8`), so the client-side paginator checks from 2026-09-10 were not carried over.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Export pagination (non-map) — `page`/`rows` respected, pages disjoint and ordered (`/api/exp`) | verified (page=0 vs page=1, `ident_cely asc`, 2+2 disjoint records) |
| Server-side row cap | verified — `rows=20000` returns exactly 1000 documents |
| CSV header/data alignment — akce 17/17, komponenta 12/12, dokument 15/15, samostatny_nalez 19/19, knihovna_3d 14/14 (hidden entries excluded) | verified — export-D01 stays fixed |
| XLSX header/data alignment | verified at cell level in-session (komponenta `dimension A1:L4`, dokument `A1:O31`; header and data cells agree) — export-D01 XLSX aspect stays fixed |
| Map export columns (`pian_ident_cely`, `pian_presnost`, `pian_typ`, `pian_zm10`, `pian_wgs84`, `geometrie`) in download data and header, aligned | verified (CSV, 20 columns; header and data agree) — export-D02 stays fixed |
| `/api/exp` without `format` | verified — raw JSON, HTTP 200 — export-D03 stays fixed |
| `/api/exp?mapa=true` without `geometrie` | verified — WKT default (`POINT(...)` in the `geometrie` column) — export-D04 stays fixed |
| knihovna_3d export, translated `f_obdobi`/`f_areal` (`byPath` extraction) | verified (CSV, JSON, XML) — export-D05 stays fixed |
| Map export pagination stride | verified — `/api/search/export_mapa` `rows=10&page=1` → `start: 10` with first record #11; `rows=500&page=1` → `start: 500` — export-D07 stays fixed |
| dokument map export | verified — `/api/exp?entity=dokument&mapa=true` returns rows with populated `pian_ident_cely`/`pian_wgs84`/`geometrie` (POLYGON WKT) — export-D08 stays fixed |
| XLSX multivalue rendering | verified at cell level in-session — **export-D06 fixed**: `sharedStrings.xml` carries bare joined values (komponenta `okres` `Semily`; dokument multi-author `Běhoun, Petr; Těsnohlídek, Jakub` and `Flek, František; Kypta, Jan; Podliska, Jaroslav`), no literal quote wrapping |
| CSV multivalue rendering | verified — array-derived fields joined with `"; "` and quoted when they contain the delimiter (valid CSV; parsers recover the bare value) |
| XML and JSON downloads translated per `exportFields`, mutually consistent | verified (knihovna_3d XML/JSON; `dokument_autor` renders as repeated XML elements — see Corrections — and as a native array in JSON) |
| URL columns (`handle`, `api_prefix`, `show_in_AMCR`) in non-map downloads, per-environment URLs | verified (CSV, XML, JSON, XLSX) |
| Map display heatmap (regression from the issue: "Solr nevraci heatmap") | verified — `facet_heatmaps.loc_rpt.counts_ints2D` present over the issue's Prague bounding box |
| Element-level protection in anonymous downloads (katastr/lokalizace empty for badatel/archeolog/archivář records) | verified (akce badatel-level row; samostatny_nalez archeolog- and archivář-level rows) |
| Credentialed export, graded protection fill-in | verified in-session with maintainer-supplied test accounts — badatel session fills a badatel-level record (katastr, lokalizace populated); archeolog session fills the archeolog-level record and withholds the archivář-level one; archivář session fills all |
| samostatny_nalez `obdobi` populated (earlier missing-data defect) | verified |
| komponenta export field list per #148 | verified present; the optional nálezy aggregation was not implemented (waived) |
| XLSX opened and read in Excel (cosmetic display) | verified (maintainer-assisted check, 2026-09-11) — workbooks from `/export?entity=komponenta` and `/export?entity=dokument` open with no repair prompt; multivalue cells display bare joined values |
| Browser-UI paginator pages (`/export`, `/export-mapa`) | verified (maintainer-assisted browser check, 2026-09-11, on the changed frontend build) — page counts shown, 10/100/1000 row-size choices work, page 2 continues from record 11 |

### Known defects

- None open. All eight recorded defects (export-D01 … export-D08) are resolved; export-D06 was verified fixed on 2026-09-11 (bare joined values in XLSX cells, verified at cell level in-session).

### Pre-existing observations

- HandleServlet record-page denial (out of scope for the export feature, first observed 2026-09-05): an authenticated session below a record's access level got HTTP 500 on the `/id/` record page while anonymous users got a clean 403-style denial. **Verified fixed on 2026-09-10** — maintainer-assisted check with a badatel-level account: clean denial / proper page. Retained here for provenance; the record-page behaviour is owned outside this scenario.

### Corrections made during this run

- The durable half stated XML renders multivalue fields as bare joined values; the deployed build `v4.0.3-215-g20163f8` renders them as repeated elements (one element per array value). The durable half above is amended to the observed behaviour; the change is benign (idiomatic XML multivalues), recorded as a change, not a defect.
- The 2026-09-10 verification marked XLSX cell-level unpacking as blocked by the session's command floor; this run's floor admitted the download-to-cache and `tar -xf` unpacking shapes, so the inspection moved in-session and superseded the maintainer's Excel check as the primary evidence for export-D06.
- The matrix initially recorded the Excel visual and browser-UI paginator checks as not examined; the maintainer performed both in-session after the artefacts were drafted (2026-09-11), and their results are incorporated above — both clean, so no correction to any conclusion was needed.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-05 | digiarchiv-test.aiscr.cz (dev branch, v4.1.0 milestone build) | First verification: regression pass over #148 fixes (URL columns, map columns, rows handling, XML consistency, rows cap, heatmap) all confirmed fixed except as noted; six defects recorded (export-D01 … export-D06); D01/D02 confirmed at XLSX cell level by unpacking the workbooks. |
| 2026-09-05 | digiarchiv-test.aiscr.cz (same build) | Maintainer-assisted checks completed: `/export` paginator verified; `/export-mapa` paginator defective (export-D07 minted, backend probes attached); credentialed (badatel) export behaviour verified. Adjacent out-of-scope observation: an authenticated session below a record's access level gets HTTP 500 on the `/id/` record page instead of a clean denial (anonymous gets 403) — a record-page defect, not export; reported in the issue comment. |
| 2026-09-10 | digiarchiv-test.aiscr.cz (v4.1.0 build, frontend hash g714e9423) | Regression pass after fix work: export-D01–D05 verified fixed (CSV/XLSX cell level, four formats, source mechanisms confirmed); export-D06 changed (CSV half fixed, XLSX half open); export-D07 changed (page count fixed via `/api/search/export_mapa`, stride defect recorded in reduced form); export-D08 minted (dokument map export empty); HandleServlet out-of-scope observation verified fixed (maintainer-assisted, badatel). Maintainer-assisted checks completed in-session: `/export-mapa` paginator mechanics (navigate + refetch; HAR analysis), XLSX display in Excel, credentialed (badatel) export and element-level protection re-verified. |
| 2026-09-10 | digiarchiv-test.aiscr.cz (build `v4.0.3-211-g467f7386`) | Second regression pass the same day, after further fix work: export-D07 verified fixed (listing `start = page × rows`, download window follows requested rows, UI page 2 continues from record 11); export-D08 verified fixed (dokument map export returns rows with PIAN columns; page renders); export-D06 changed again (raw arrays gone; joined values wrapped in literal quotes in XLSX cells — cosmetic residual, stays open); full matrix re-verified. XLSX cell-level unpacking was blocked by the session's command floor, so XLSX was verified through the maintainer's Excel check instead. |
| 2026-09-11 | digiarchiv-test.aiscr.cz (build `v4.0.3-215-g20163f8`) | Regression pass on the new build (four dev commits since `g467f7386`): **export-D06 verified fixed** — XLSX cells carry bare `"; "`-joined values, confirmed at cell level in-session by unpacking the workbooks (the command floor now admits the download-to-cache and `tar -xf` shapes). XML multivalue rendering changed to repeated elements (durable half amended). Credentialed graded protection verified in-session at badatel, archeolog, and archivář levels with maintainer-supplied test accounts. Maintainer-assisted checks completed in-session: Excel visual display (clean, bare values) and browser-UI paginators on the changed frontend (page counts, row-size choices, page-2 continuation — all as expected). No defects open; matrix re-verified. |
