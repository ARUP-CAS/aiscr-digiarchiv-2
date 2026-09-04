# Export — testing scenario

**Key:** export (feature)
**Scope:** the export feature — pagination in the export window, download buttons (CSV/XLSX/XML/JSON), record URLs in exports, and the map export; issue [ARUP-CAS/aiscr-digiarchiv-2#148](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/148) drove it.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Surface | URL | Role |
| --- | --- | --- |
| Export page | `https://digiarchiv-test.aiscr.cz/export` | Angular page: table + paginator + download links |
| Map export page | `https://digiarchiv-test.aiscr.cz/export-mapa` | same, with spatial columns and `mapa=true` |
| Table data API | `…/api/search/export?entity=<e>&rows=N&page=P` (and `/api/search/export_mapa`) | what the table displays |
| Download endpoint | `…/api/exp?entity=<e>&format=csv\|xlsx\|xml\|json&rows=N&page=P[&mapa=true][&q=…]` | `ExportServlet` (`/exp`); serves the file |

### Architecture and implementation facts

- The UI download buttons build the `/api/exp` URL from `document.location.search` — the download always reflects the current page, page size, sort and filters.

### Feature or entity model

Client configuration (`…/assets/config.json`) drives the feature:

- `exportFields` — per-entity field list (name/label/path/translated) driving table columns and CSV/XLSX/JSON downloads; entities covered: dokument, akce, lokalita, projekt, samostatny_nalez, knihovna_3d, komponenta.
- `choiceApi` — the extra URL columns rendered in the UI table (API and AMČR record links, env-specific).
- `serverUrl` — Digiarchiv base used for the handle column.
- `exportRowsLimit` — default rows for the export table (100 on test).
- `mapOptions.docsForMarker` — the rows count forced for `mapa=true` (500 on test).

### Discovery recipes

1. **Formats:** request `/api/exp?entity=<e>&rows=3&format=<csv|xlsx|xml|json>` and check HTTP status, `Content-Type`, and body shape (CSV: header + rows; XLSX: binary PK magic; XML: `<docs><doc>…`; JSON: translated array).
2. **Pagination:** vary `rows` and `page` on `/api/search/export` (table) and `/api/exp` (download) and compare returned idents — consecutive pages must return consecutive, non-overlapping record sets.
3. **Rows cap:** try `rows=5000`/`rows=20000` and count returned docs.
4. **Map mode:** add `mapa=true` (and optionally a `vyber`/`loc_rpt` bbox) — verify the location filter (numFound drops vs non-map), the forced rows count, and that `page` still advances by that count.
5. **URL columns:** check the first CSV header line and the JSON keys for the URL columns (currently absent — see export-D01); check the UI table header for `#`/handle/`choiceApi` columns.
6. **Element protection:** export a record known to be element-restricted for anonymous (see the [`permissions`](permissions.md) scenario for how to find one) and verify its secured values are absent from all formats.
7. **Cross-environment:** repeat against production when it receives the build; expect data drift in record sets and `numFound`.

## Current verification (2026-08-28, `digiarchiv-test` build with #148, commit `437f6e63` on `dev`; production compared where relevant)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Export table pagination (page sizes 10/100/1000, go-to-page, sort) | works; `rows`/`page` params honored by the table API |
| Download honors current page/filters (`page`, `rows`, `q`, `mapa`) | works — consecutive pages return consecutive record sets |
| CSV download | 200 `text/csv;charset=UTF-8`, label header + translated rows |
| XLSX download | 200 `application/xlsx`, valid binary (PK/zip magic) |
| XML download | 200 `text/xml` — **raw docs**: untranslated ids, nested JSON, not shaped by `exportFields` |
| JSON download | 200, translated per `exportFields` (label keys, translated heslar values) |
| Map export table columns (PIAN, přesnost, PIAN-typ, ZM10, definiční bod WGS-84) | present in UI table |
| `mapa=true` filtering (records with location, pristupnost-suffixed loc fields) | works |
| Element-level protection in downloads (pr>B `chranene_udaje`) | holds — restricted katastr/lokalizace absent from anonymous CSV/XML/JSON (mechanism owned by the [`permissions`](permissions.md) scenario) |
| Server-side rows cap | **none** — `rows=20000` returns 20000 docs in one request |

### Known defects

- export-D01: **URL columns missing in downloads** — the three record URLs (Digiarchiv handle, API, AMČR) are rendered only in the UI table (`#` + handle + `choiceApi` columns); none of the four download formats contains them because `ExportServlet` builds purely from `exportFields`.
- export-D02: **Map columns missing in map downloads** — the `/export-mapa` table shows spatial columns (PIAN, přesnost, typ, ZM10, definiční bod), but the `mapa=true` download contains only the standard `exportFields` columns.
- export-D03: **`rows` ignored when `mapa=true`** — `SolrSearcher.addCommonParams` forces `rows = mapOptions.docsForMarker`; the map paginator's page-size selector has no effect. A commented-out `Math.max(docsForMarker, rows)` line marks the original intent.
- export-D04: **XML format inconsistent** — raw documents instead of the translated `exportFields` shape used by CSV/XLSX/JSON.
- export-D05 (minor): no server-side rows cap (see the matrix); "export all records across pages at once" is not implemented (page-wise only).

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-28 | `digiarchiv-test` (build with #148, commit `437f6e63` on `dev`) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
