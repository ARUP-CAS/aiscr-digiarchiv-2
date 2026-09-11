# Museion finds integration — testing scenario

**Key:** museion (feature)
**Scope:** the Museion finds integration — SOAP-driven `predmetyDleAmcrId` data for projekt, akce, and samostatny_nalez records, the `/museion/:id` results page (selector, BASIC/FULL table), the `inMuseion` search filter built on the helper index fed by `predmetyStatistika`, the config-driven filter-icon visibility, and the admin quick actions. Driven by ARUP-CAS/aiscr-digiarchiv-2#553.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — Angular frontend (client-rendered; SSR serves the shell only), API under `/api/`. Verified anonymously unless stated.
- Museion SOAP service: configured per instance in the server config `museion` section (each entry: `end_point`, `clientId`, `clientSecret` — credentials never recorded here). The test deployment configures three SOAP client accounts — one against the upstream dev service and two against the demo service — and the app-mediated statistics answer with two organizations (ORG-000071, ORG-000077); the union by `organizaceId` handles an organization served under several accounts. The SOAP endpoints are always app-mediated; the run never probes them directly. The upstream dev service has recurring maintenance windows around hourly builds (per the issue thread).
- `MuseionServlet` at `/api/mus/*`: `predmety_by_id?id=<AMCR_ID>` (finds per record), `statistika` (per-organization counts and id lists), `index` (helper-index rebuild; mutating, cron-driven). Responses are pretty-printed JSON.
- `/api/mus/predmety_by_id` response shapes: a record with data → `{"predmetyDleAmcrId": {"<organizaceId>": {...predmetSys/predmetPom...}}}`; a known record with no Museion data → `{"predmetyDleAmcrId": {}}`; an unknown id → bare `{}`. BASIC records serialize their non-identifier fields as `null` (an earlier build serialized numeric defaults such as `0` — museion-D03).
- `/api/search/museion`: the action name is absent from the search dispatch enum, so the request fails with HTTP 500 (`No enum constant ...Actions.MUSEION`); the frontend never calls it (the filter goes through `/api/search/query` with `inMuseion`). Resolved museion-D05.
- Search filter: the `inMuseion` query parameter on `/api/search/query` (the frontend sets it as a URL query parameter and drops it when the filter is off).
- Client config: `/api/config` — the toolbar filter icon follows the `showMuseion` client-config key. The bundled default client config (`/assets/config.json` in the webapp) ships `"showMuseion": false`; a deployment enables the flag in the custom `.amcr/config.json` `client` section together with the Museion instance setup (the `museion` server section). The key is an independent client-config flag — `Options.getClientConf()` returns the merged client config as-is, with no runtime derivation from the configured instances — so removing only the server-section endpoints while leaving the flag `true` keeps the icon visible, while removing the Museion configuration as a unit (flag included) lets the value fall back to the default `false` and the icon hides (maintainer-confirmed live 2026-09-11 — museion-D07 resolved). Any config change requires a config reset (`/api/config?reset` or the admin quick action).
- Results page: `/museion/<ident>` — lazy Angular route, opened in a new tab from the result row's Museion button.

### Architecture and implementation facts

- `MuseionServlet` dispatches the `/mus/*` actions; `MuseionClient` calls each configured `end_point` per query and unions the results by `organizaceId`. The same endpoint may be configured more than once under different accounts (each account = one organization) and several endpoints may serve the same organization; the union handles both. Per-endpoint failures are swallowed: an unreachable instance contributes nothing rather than failing the whole request (the SOAP client uses a short timeout per endpoint).
- `predmety_by_id` entity-type resolution: the frontend route carries no type parameter, so the page component passes an unset `typ` and the request omits it; `MuseionServlet.PREDMETY_BY_ID` then resolves the type server-side through `SolrSearcher.getEntityById(id)` (projekt → `P`, akce → `A`, samostatny_nalez → `N`) and returns the bare `{}` shape when the id resolves to no entity (fixed museion-D04's API side; the client guard handles the shape).
- Helper index: a Solr core (`museion`) holding `amcrId` + entity `type`, rebuilt by `/api/mus/index` from `predmetyStatistika`. Refresh is cron-driven; request-scoped cache expiration options were removed from the config in favour of the periodic job. The rebuild's cleanup (`clean(start)`, a `deleteByQuery` on `indextime` older than the run start) runs after the endpoint loop completes — including the zero-endpoint case, so a rebuild with no configured instances clears the core (fixed museion-D08; confirmed live 2026-09-11: after a zero-endpoint rebuild the `inMuseion` filter returned 0 results). Ids from Museion are expected trimmed: whitespace-suffixed shapes were observed on the 2026-09-06 deployment and were gone by 2026-09-10 after the provider and the integration both added trimming (per the issue thread); an unprefixed id shape may still occur (data-state observation below).
- Filter mechanics: `inMuseion=true` adds an fq join from the helper index onto `ident_cely` (`{!join fromIndex=museion to=ident_cely from=amcrId}type:*`); matching docs additionally carry an `inMuseion` flag, and the per-row Museion button in the results list (`result-actions`) is gated on that per-doc flag through `withMuseion()` (an anchor `museion/<ident>` with `target="_blank"` and tooltip `museion.zobrazit_predmety`). The toolbar facet itself is gated on `config.showMuseion`.
- Entity type mapping: `P` = projekt, `A` = akce, `N` = samostatny_nalez (the samostatny_nalez letter changed from legacy `S` to `N`; the statistika payload carries the type per id element).
- Results page model: `pristup` (`FULL`/`BASIC`) is per record. Table mode = FULL when any `predmetSys` or `predmetPom` record has `pristup === "FULL"`, else BASIC. Full table renders the merged `predmetSys` + `predmetPom` rows against one shared column list of 36 columns with `pristup` first; the BASIC table renders the simplified single-column list. Cell values render raw (no date or number pipes) — API strings reach the DOM unchanged, which is what keeps dates in their API form; `null` renders as an empty cell.
- Results page response handling: the page calls `museionPredmety(id, typ)` with the type unset (see the entity-type resolution above); the subscribe is guarded (`e.predmetyDleAmcrId && Object.keys(...).length > 0` gates the model population), `loading` clears on every path, and an error response triggers an alert dialog. With no data the organisation list stays empty and the template renders the `museion.not_found` message ("Záznam se nepodařilo najít.") instead of the table; the counters render through null-safe access.
- Selector: organizations are the keys of `predmetyDleAmcrId` (only instances that returned data for the record are present); the label is the i18n key `museion.organizaceId` ("Správce sbírky"), rendered before the field. Counts shown from `pocetPom` / `pocetSys`.
- Column headers render from i18n keys `museion.<column>` with tooltips from `museion.desc.<column>`; the leading `pristup` column labels from `museion.pristup` ("Přístup").
- Table CSS: `table[mat-table]` uses `width: max-content; min-width: 100%; table-layout: auto`; header cells `min-width: max-content; white-space: nowrap`; `sbirka`/`popis` 220 px; `dataceVzniku`/`kontextPlocha` 140 px. The component styles carry no media queries — the page has no responsive handling (museion-D06).
- Admin quick actions (config reload `config?reset=true`, Fedora index update `/api/fedora/index_update`, Museion index update `/api/mus/index`) live in the user menu, gated on `pristupnost === 'E'`.

### Feature or entity model

- Covered entities: projekt, akce, samostatny_nalez — each with the per-row Museion button when the doc carries the `inMuseion` flag.
- `predmetSys` and `predmetPom` share one column list (no duplication); `cislo` is the join-friendly identifier column.
- BASIC records carry the identifier (`cislo`) in practice; the other fields serialize as `null` and render empty.

### Discovery recipes

1. Records with Museion data (any entity): fetch `/api/mus/statistika` and read the per-organization `amcrIdSys`/`amcrIdPom` lists — those ids are the currently Museion-known AMČR records, each carrying its entity type (P/A/N).
2. Multi-instance candidates: an id that appears in two different organizations' lists in `/api/mus/statistika`.
3. Mixed BASIC/FULL candidates: fetch `/api/mus/predmety_by_id?id=<id>` for recipe-1 candidates and scan the `pristup` values inside `predmetSys`.
4. No-data record (empty case): any entity id present in search but absent from the statistika lists — e.g. take the first result of `/api/search/query?entity=akce&rows=1&sort=ident_cely+asc` and confirm it is not in the lists.
5. Unknown-id response shape: request `/api/mus/predmety_by_id?id=<syntactically valid nonexistent id>`.
6. Filter effect check: run the same `/api/search/query?entity=<entity>&rows=0` request with and without `inMuseion=true` and compare `numFound`; with the filter on, matching docs carry the `inMuseion` flag.
7. Deployed frontend chunks (client-side verification without a browser): fetch the page shell HTML for the script tag naming the current `main-*.js`, fetch that bundle, and extract the lazy chunk name from the text following the `museion/:id` route literal — the museion page chunk carries `columnsFull`, the guarded subscribe, the component CSS, and the template branches; the per-row Museion button lives in `result-actions` (in the eagerly loaded bundle tree). Chunk hashes change per build; the recipe survives them.

### Verification commands

Identifier-free, re-runnable against the test instance (rate-limit gently; anonymous session):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/config"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/mus/statistika"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/mus/predmety_by_id?id=<AMCR_ID>"
curl.exe -s -o NUL -w "%{http_code}" "https://digiarchiv-test.aiscr.cz/api/search/museion"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&rows=0&inMuseion=true"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&rows=0"
```

`/api/mus/index` and `/api/fedora/index_update` are mutating and never probed by this scenario (the maintainer runs them during assisted checks).

## Current verification (2026-09-11, digiarchiv-test.aiscr.cz, dev branch, anonymous session plus maintainer-performed checks)

Regression pass after the 2026-09-10 verification and the subsequent fixes discussed in the issue thread (museion-D08 cleanup fix; museion-D07 resolution).

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Multi-instance configuration (three client accounts; statistics answer two organizations) | verified — both organizations answer through the app-mediated API (fresh statistika) |
| Query fans out to all configured instances for both `predmetyDleAmcrId` and `predmetyStatistika` | verified — a multi-organization record returns both blocks; statistika covers both organizations |
| Union by `organizaceId` (same endpoint under multiple accounts) | verified — a multi-organization record returns both blocks |
| Only instances that returned data for the record are offered in the selector | verified — the response keys carry exactly the organizations with data; the no-data case returns an empty map |
| Selector labeled "Správce sbírky", rendered before the field | verified — deployed i18n `museion.organizaceId` (fresh cs.json fetch); browser rendering checked by the maintainer 2026-09-10 |
| Switching between instances loads that instance's data | not re-examined this run (browser check; maintainer-verified 2026-09-10) |
| Information message when no instance returns data | verified — the no-data record returns `{"predmetyDleAmcrId": {}}` and the deployed page chunk carries the `not_found` branch; maintainer browser check 2026-09-10 |
| Filter icon hidden when no Museion instance is configured | verified — maintainer config test 2026-09-11: with the Museion configuration removed the icon hides (the bundled default `showMuseion: false` was confirmed in the deployed default config; with the instances configured, `/api/config` serves `showMuseion: true`) — museion-D07 resolved |
| Helper index cleared when no instance is configured | verified — maintainer zero-endpoint reindex re-test 2026-09-11: after the rebuild the `inMuseion` filter returned 0 results; the fix (`clean(start)` after the endpoint loop) confirmed in source; the endpoints were then restored and fresh probes confirm statistika answering both organizations and the filter back to its normal narrowing — museion-D08 resolved |
| Museion button on projekt, akce, samostatny_nalez result rows (docs with the `inMuseion` flag) | verified — a freshly filtered doc carries `"inMuseion": true` and the join fq is present in the response params; the `result-actions` template gates the anchor on `withMuseion()` (source unchanged since the 2026-09-10 deployed-chunk verification) |
| Results open on a separate page in a new tab | verified — the `result-actions` anchor (`museion/<ident>`, `target="_blank"`, tooltip `museion.zobrazit_predmety`); deployed-chunk verification 2026-09-10 |
| Entity types P/A/N respected | verified — statistika ids carry `P`/`A`/`N`; the samostatny_nalez filter narrows to joinable docs |
| BASIC-only response → simplified table; any FULL → full table | verified — deployed page chunk mode logic (any `predmetSys`/`predmetPom` `pristup === "FULL"`); maintainer visual check 2026-09-10 |
| Column order of the full table | verified — `columnsFull` in the deployed page chunk starts with `pristup` (36 columns); deployed i18n `museion.pristup` ("Přístup") |
| `predmetSys` / `predmetPom` share columns, no duplication | verified — one merged row list against one column list (deployed chunk) |
| Dates rendered in API format (no unix timestamps) | verified — live responses carry ISO date strings; source template renders raw interpolation with no date pipe |
| Minimum column widths | verified — deployed page chunk CSS (220 px / 140 px / header `max-content`) |
| Horizontal scrolling correctness incl. header/footer background | not re-examined this run (browser check; maintainer-verified 2026-09-10) |
| `inMuseion` filter (helper index + join) | verified — the filter narrows akce from 172231 to 13; matching docs carry the flag |
| False zeros on BASIC records in mixed tables | verified — the live API serializes BASIC numeric fields as `null` (two-organization record with 2 BASIC + FULL rows) |
| `/api/search/museion` endpoint | verified — HTTP 500 (`No enum constant ...Actions.MUSEION`); not called by the frontend |
| Empty-response robustness of the page | verified — unknown id returns bare `{}`; the deployed subscribe is guarded and `loading` clears on every path |
| Responsive layout of the results page | **failed** — no responsive handling exists in the source styles or the deployed page chunk (no media queries); maintainer-observed 2026-09-06 and 2026-09-10 (museion-D06) |
| Index refreshed by periodic job, not request cache | not verified — cron still unconfirmed (maintainer-assisted); `/api/mus/index` mutating, run only by the maintainer during the re-test |
| Admin quick actions (level E user menu) | verified — the maintainer used the Museion index update quick action for the D08 re-test (2026-09-11); full set maintainer-verified 2026-09-10 |

### Defect outcomes since the 2026-09-10 verification

- museion-D01: **fixed (holds)** — `columnsFull` in the deployed page chunk starts with `pristup`; deployed i18n `museion.pristup` present.
- museion-D02: **fixed (holds)** — the no-data API shape and the deployed `not_found` branch re-verified.
- museion-D03: **fixed (holds)** — BASIC numeric fields serialize as `null` in the live response.
- museion-D04: **fixed (holds)** — unknown id returns bare `{}`; the deployed guarded subscribe re-verified; the server-side type resolution (client omits `typ`) documented in durable knowledge.
- museion-D05: **fixed (holds)** — `/api/search/museion` fails with HTTP 500.
- museion-D06: **still present** — no responsive CSS in the source styles or the deployed page chunk; maintainer-observed 2026-09-06 and 2026-09-10.
- museion-D07: **fixed (resolved)** — the maintainer's 2026-09-11 config test confirmed the icon hides when the Museion configuration is removed: the bundled default client config ships `showMuseion: false`, and the flag is enabled together with the Museion setup in the deployment's custom config, so removing the configuration hides the icon (config reset still required — the flag is not derived at runtime). The mechanism is documented in the issue thread; no code change was needed.
- museion-D08: **fixed** — `clean(start)` now runs after the endpoint loop in `MuseionClient.indexStatistika()` (confirmed in source), and the maintainer's 2026-09-11 zero-endpoint reindex re-test confirmed the deployed build clears the helper core (the `inMuseion` filter returned 0 results after the rebuild; the normal state was restored afterwards and re-verified by fresh probes).

### Known defects

- museion-D06 (cosmetic): **responsive view not optimised — the selector and the pocetPom/pocetSys counts squeeze on narrow viewports.** Maintainer-observed on 2026-09-06 and re-observed 2026-09-10; the source styles and the deployed page chunk carry no media queries, so no fix has been attempted; desktop rendering works as expected.

### Data-state observations (not defects of this repository)

- Museion-side id hygiene: the unprefixed id (`202609962`, typ P, in ORG-000071 `amcrIdPom`, alongside the prefixed `C-202609962`) is still present and cannot join onto `ident_cely`. The whitespace-suffixed shapes remain gone (trimmed on both sides since 2026-09-10).
- The `N`-type join state observed on 2026-09-10 (N ids with no counterpart in the entities index → the samostatny_nalez filter returned 0) no longer holds: the test entities index now has counterparts and the samostatny_nalez Museion filter returns 3. Re-verify with fresh statistika before relying on it.
- Current filter coverage on the test data: akce 13 (was 6 on 2026-09-10), projekt 7, samostatny_nalez 3; statistika per-organization counts: ORG-000071 pocetSysCelkem 3 / pocetPomCelkem 9, ORG-000077 pocetSysCelkem 22 / pocetPomCelkem 0.

### Corrections made during this run

- Two durable-knowledge corrections, each superseding a 2026-09-10 statement:
  - The `showMuseion` claim ("removing all configured instances does not flip it and the icon stays… hiding requires setting `showMuseion: false` explicitly") was imprecise: the bundled default is `false`, so removing the whole Museion configuration (the flag enabled with it) also hides the icon — maintainer-confirmed live 2026-09-11 (museion-D07 resolved). The 2026-09-10 observation remains true for the narrower operation it tested (server-section endpoints removed, flag left `true`).
  - The helper-index cleanup claim ("runs only per processed endpoint… with zero configured endpoints no cleanup runs") described the pre-fix code; the cleanup now runs after the endpoint loop, and the maintainer's live re-test confirmed the zero-endpoint rebuild clears the core (museion-D08 fixed).
- One durable-knowledge refinement: the page fetch no longer "carries the entity type alongside the id" — the route has no type parameter, the request omits `typ`, and the servlet resolves the type server-side (documented under *Architecture and implementation facts*).

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-06 | digiarchiv-test.aiscr.cz (dev branch) | First verification of the Museion integration (#553): multi-instance fan-out, union, selector, BASIC/FULL table logic, filter join, admin actions verified (admin and browser checks maintainer-performed); six defects minted (museion-D01 … museion-D06, with D03 re-opening an issue-marked-fixed item); Museion-side id-hygiene and missing-entity data states recorded as observations. |
| 2026-09-10 | digiarchiv-test.aiscr.cz (dev branch) | Regression pass (#553) after the 2026-09-07 fixes: museion-D01–D04 verified fixed on the deployed build and live API, museion-D05 resolved by action removal (HTTP 500), museion-D06 still present; id whitespace trimmed on both sides, the unprefixed id remains; maintainer browser/admin checks re-confirmed selector switching, admin quick actions, and the visual table states. Same-day extension: the maintainer live-tested the zero-endpoint configuration (icon stays, helper index stale after reindex) — museion-D07 and museion-D08 minted and the `showMuseion` durable claim corrected; the test config was left with zero configured endpoints by the maintainer's test. |
| 2026-09-11 | digiarchiv-test.aiscr.cz (dev branch) | Regression pass (#553) after the D08 fix and the D07 resolution: museion-D08 verified fixed in source and live (maintainer zero-endpoint reindex re-test — filter returns 0, then restored and re-verified), museion-D07 verified resolved by the maintainer's config test (icon hides when the Museion configuration is removed; default `showMuseion: false` confirmed in the deployed default config), museion-D01–D05 hold on the deployed build and live API, museion-D06 still present; N-type join state superseded (samostatny_nalez filter now returns 3), unprefixed id persists; durable knowledge corrected for the icon mechanism, the cleanup placement, and the server-side `typ` resolution; fresh frontend-chunk discovery recipe added. |
