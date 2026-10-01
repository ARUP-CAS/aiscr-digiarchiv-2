# Search date and year filters — testing scenario

**Key:** date-filters (feature)
**Scope:** the extended-search date and year filters on the results facet panel ("filtruj dle") — the localized datepicker calendar, the rok-type year-only inputs, single-sided date intervals, and the date/rok value contract through the search API — plus the stats page datepickers that share the same global locale wiring. Driven by ARUP-CAS/aiscr-digiarchiv-2#893.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — Angular frontend (client-rendered; SSR serves the shell only), API under `/api/`. Verified anonymously unless stated.
- Production instance: `https://digiarchiv.aiscr.cz/` — same shape; the two instances carry different builds (see the current verification for the state at the last run).
- Results facet panel: `/results?entity=<entity>` — client-rendered; the panel's filter UI is compiled into a lazy chunk (discovery recipe 2), so verifying it means inspecting the deployed chunk, not the SSR shell.
- Search API: `SearchServlet` at `/api/search/query` — carries the date/rok filter parameters; the full parameter contract is documented in this repository at `web/docs/search-servlet-query.md`.
- Client config: `/api/config` — `filterFields` lists every filterable field with its type (`text`, `date`, `rok`, `number`, `boolean`); the facet panel renders the input pair by that type.
- Stats page (`/stats`) uses the same datepicker inputs for its date range; both surfaces take their calendar locale from the global Angular providers.

### Architecture and implementation facts

- The facet panel is `FacetsDynamicComponent` (`web/src/main/ng/src/app/components/facets-dynamic/`): a field selector fed by `config.filterFields`, an operator menu (or/and/not), and a type-dependent input pair. "Aplikovat filtr" writes the filter to the URL as `<field>=<value>:<operator>` (default `or`) and navigates, so every applied filter is shareable as a results URL.
- Calendar localization is global, not per-component: `app.config.ts` provides `MAT_DATE_LOCALE: 'cs-CZ'`, `provideLuxonDateAdapter()`, `registerLocaleData(localeCs)`, and `MY_FORMATS` (`parse.dateInput: 'd.M.yyyy'`, `display.dateInput: 'dd.MM.yyyy'`). Week start and month names therefore follow the Czech locale for every datepicker in the app — the facet panel and the stats page alike.
- Filter value contracts (backend `SolrSearcher.addFilters`, documented in `web/docs/search-servlet-query.md`):
  - `date` — `YYYY-MM-DD,YYYY-MM-DD`; either side may be `null` for an open interval (single-sided filtering).
  - `rok` / `number` — `od,do`; an empty start means `*`.
  - The frontend builds the value by concatenating both sides and appends the operator as a `:` suffix.
- The `rok` input pair is two controls per side: a visible numeric input bound to the year (`rokod`/`rokdo`) and a hidden date input bound to a `FormControl` (`rokoddate`/`rokdodate`) that the multi-year datepicker drives through `chosenYearHandler` (a `yearSelected` closes the picker and sets the year). This is what keeps a year filter displaying a bare year instead of a full date.
- The `date` inputs parse flexibly (`d.M.yyyy`): `1.1.1990` and `01.01.1990` both parse; display is always `dd.MM.yyyy`.
- Apply guard: `canFilter()` requires at least one value for the `date`, `number`, and `rok` types, so applying an empty pair (the earlier "do null" state) is not possible from the UI; a hand-built both-`null` URL is accepted by the API and simply returns the unfiltered set.

### Feature or entity model

- Date-type filter fields (deployed `/api/config`): `dokument_datum_zverejneni`, `extra_data_datum_vzniku`, `let_datum`, `projekt_datum_zahajeni`, `projekt_datum_ukonceni`, `akce_datum_zahajeni`, `akce_datum_ukonceni`, `datum_provedeni`, `samostatny_nalez_datum_nalezu`.
- Rok-type filter fields: `dokument_rok_vzniku`, `extra_data_rok_od`, `extra_data_rok_do`, `neident_akce_rok_zahajeni`, `neident_akce_rok_ukonceni`, `az_adb_rok_popisu`, `az_adb_rok_revize`.
- The field list is per-entity in the facet selector, but the type contract is shared: a probe recipe against one entity's date field applies to the others unchanged.

### Discovery recipes

1. Fresh filterable fields per entity: `GET /api/config` and read `filterFields` — every entry with `type` `date` or `rok` is a candidate filter; the entity tab on `/results` chooses which are offered in the selector.
2. Locate the deployed facets chunk (client-side verification without a browser): fetch `/results?entity=akce` for the shell, note the `main-*.js` and `chunk-*.js` script names, fetch the chunks, and find the one carrying `chosenYearHandler` — that chunk is the compiled `FacetsDynamicComponent`.
3. Calendar-locale wiring in the deployed build: fetch `main-*.js` and check three markers: the provider pair `useValue:"cs-CZ"`, the formats object `parse:{dateInput:"d.M.yyyy"}`, and Czech locale data (month names such as `leden`).
4. Fresh filter-effect candidates: run the same `/api/search/query?entity=<entity>&rows=0` request with and without the filter parameter and compare `numFound`; a narrowed, error-free response is the candidate set.

### Verification commands

Identifier-free, re-runnable (rate-limit gently; anonymous session):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/config"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=dokument&rows=0"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=dokument&rows=0&dokument_rok_vzniku=1990,2000"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=dokument&rows=0&dokument_datum_zverejneni=2026-04-01,null:or"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=dokument&rows=0&dokument_datum_zverejneni=null,2026-03-30:or"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&rows=0&akce_datum_zahajeni=2020-01-01,2020-12-31"
```

The same probes against `https://digiarchiv.aiscr.cz` compare production; the state found at the last verification is recorded under the current verification (date-filters-D01).

## Current verification (2026-09-15, digiarchiv-test.aiscr.cz, anonymous session plus maintainer browser checks; production compared read-only)

First verification of this scenario (#893, closed): the calendar-localization and year-filter fix set verified on the test instance's deployed build and API, with the production instance compared.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Date-filter calendar localized in Czech, week starting Monday | verified — deployed main bundle carries the `MAT_DATE_LOCALE "cs-CZ"` provider, the Luxon date adapter, `MY_FORMATS` (`parse d.M.yyyy`, `display dd.MM.yyyy`), and Czech locale data; maintainer browser check 2026-09-15 confirmed the rendered calendar |
| `rok` filter shows the year only (no DD.MM.RRRR) | verified — deployed facets chunk carries the compiled `rok` template (numeric inputs on `rokod`/`rokdo`, hidden date inputs, `chosenYearHandler`); maintainer browser check 2026-09-15 |
| Single-sided date filter (only "od" or only "do") | verified — API probes: `dokument_datum_zverejneni=2026-04-01,null:or` → 3594 docs and `null,2026-03-30:or` → 191991 docs (baseline 195587), no error bodies |
| Flexible date input (`1.1.1990`) | verified — deployed `parse.dateInput: "d.M.yyyy"`; maintainer typed the value in the browser 2026-09-15 |
| Both-empty pair cannot be applied ("do null" state) | verified — deployed chunk carries the `canFilter` guard; maintainer confirmed the Apply button stays disabled; a hand-built both-`null` URL returns the unfiltered set (195587) without error |
| `dokument_rok_vzniku` year filter narrows | verified — `1990,2000` → 15258 docs (baseline 195587) |
| `akce_datum_zahajeni` date range narrows | verified — `2020-01-01,2020-12-31` → 8687 docs (baseline 172231) |
| Stats page calendar shares the localization | verified — stats template in the deployed main bundle; the locale providers are global (`app.config.ts`) |
| Fix set live in production | **failed** — production main bundle carries the pre-fix build (`parse dateInput "DD.MM.YYYY"`, the old `chosenYearHandler` calling `.year()`, no Czech locale data) and its API rejects single-sided date filters with `{"error":"org.json.JSONException: JSONObject[\"response\"] not found."}` while full ranges work (dokument baseline 204040; `dokument_rok_vzniku=1990,2000` → 15691; `akce_datum_zahajeni=2020-01-01,2020-12-31` → 9711) — date-filters-D01 |

### Known defects

- date-filters-D01 (deployment): **the #893 fix set is not yet live in production (`digiarchiv.aiscr.cz`).** Verified 2026-09-15 by direct comparison: the production bundle carries the pre-fix datepicker wiring and `chosenYearHandler`, and single-sided date filters error at the API level, so production users still see the English calendar (week from Sunday), the strict `DD.MM.YYYY` input, and cannot filter by one date side. Not a code regression — the fixed code is verified on the test instance; production awaits the release that carries it.

### Corrections made during this run

- None — first verification of this scenario.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-15 | digiarchiv-test.aiscr.cz, production compared read-only | First verification (#893): calendar localization, rok year-only inputs, single-sided date intervals, flexible input format, and the apply guard verified on the deployed build, API, and maintainer browser checks; production compared and found still on the pre-fix build — date-filters-D01 minted; no code defects found. |
