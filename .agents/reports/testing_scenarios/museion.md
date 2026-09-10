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
- Client config: `/api/config` — `showMuseion` flag drives the toolbar filter icon, but it is an independent client-config key (default config merged with the custom `client` section; returned as-is by `Options.getClientConf()`): it is never derived from the `museion` server section, so removing all configured instances does not flip it and the icon stays (verified live 2026-09-10 — museion-D07). Hiding the icon requires setting `showMuseion: false` explicitly.
- Results page: `/museion/<ident>` — lazy Angular route, opened in a new tab from the result row's Museion button.

### Architecture and implementation facts

- `MuseionServlet` dispatches the `/mus/*` actions; `MuseionClient` calls each configured `end_point` per query and unions the results by `organizaceId`. The same endpoint may be configured more than once under different accounts (each account = one organization) and several endpoints may serve the same organization; the union handles both. Per-endpoint failures are swallowed: an unreachable instance contributes nothing rather than failing the whole request (the SOAP client uses a short timeout per endpoint).
- Helper index: a Solr core (`museion`) holding `amcrId` + entity `type`, rebuilt by `/api/mus/index` from `predmetyStatistika`. Refresh is cron-driven; request-scoped cache expiration options were removed from the config in favour of the periodic job. The rebuild's cleanup (`clean(start)`, a `deleteByQuery` on `indextime` older than the run start) runs only per processed endpoint: with zero configured endpoints the loop body never executes, no cleanup runs, and the core keeps stale entries (museion-D08). Ids from Museion are expected trimmed: whitespace-suffixed shapes were observed on the 2026-09-06 deployment and were gone by 2026-09-10 after the provider and the integration both added trimming (per the issue thread); an unprefixed id shape may still occur (data-state observation below).
- Filter mechanics: `inMuseion=true` adds an fq join from the helper index onto `ident_cely`; matching docs additionally carry an `inMuseion` flag, and the per-row Museion button in the results list is gated on that per-doc flag (an anchor to `museion/<ident>` with `target="_blank"` and tooltip `museion.zobrazit_predmety`). The toolbar facet itself is gated on `config.showMuseion`.
- Entity type mapping: `P` = projekt, `A` = akce, `N` = samostatny_nalez (the samostatny_nalez letter changed from legacy `S` to `N`; the statistika payload carries the type per id element).
- Results page model: `pristup` (`FULL`/`BASIC`) is per record. Table mode = FULL when any `predmetSys` or `predmetPom` record has `pristup === "FULL"`, else BASIC. Full table renders the merged `predmetSys` + `predmetPom` rows against one shared column list of 36 columns with `pristup` first; the BASIC table renders the simplified single-column list. Cell values render raw (no date or number pipes) — API strings reach the DOM unchanged, which is what keeps dates in their API form; `null` renders as an empty cell.
- Results page response handling: the fetch carries the entity type alongside the id (`museionPredmety(id, typ)`); the subscribe is guarded (`e.predmetyDleAmcrId && Object.keys(...).length > 0` gates the model population), `loading` clears on every path, and an error response triggers an alert dialog. With no data the organisation list stays empty and the template renders the `museion.not_found` message ("Záznam se nepodařilo najít.") instead of the table; the counters render through null-safe access.
- Selector: organizations are the keys of `predmetyDleAmcrId` (only instances that returned data for the record are present); the label is the i18n key `museion.organizaceId` ("Správce sbírky"), rendered before the field. Counts shown from `pocetPom` / `pocetSys`.
- Column headers render from i18n keys `museion.<column>` with tooltips from `museion.desc.<column>`; the leading `pristup` column labels from `museion.pristup` ("Přístup").
- Table CSS: `table[mat-table]` uses `width: max-content; min-width: 100%; table-layout: auto`; header cells `min-width: max-content; white-space: nowrap`; `sbirka`/`popis` 220 px; `dataceVzniku`/`kontextPlocha` 140 px.
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

### Verification commands

Identifier-free, re-runnable against the test instance (rate-limit gently; anonymous session):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/config"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/mus/statistika"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/mus/predmety_by_id?id=<AMCR_ID>"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/museion"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&rows=0&inMuseion=true"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&rows=0"
```

`/api/mus/index` and `/api/fedora/index_update` are mutating and never probed by this scenario.

## Current verification (2026-09-10, digiarchiv-test.aiscr.cz, dev branch, anonymous session plus maintainer-performed checks)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Multi-instance configuration (three client accounts; statistics answer two organizations) | verified — both organizations answer through the app-mediated API |
| Query fans out to all configured instances for both `predmetyDleAmcrId` and `predmetyStatistika` | verified (statistika covers both organizations; predmety responses can carry both) |
| Union by `organizaceId` (same endpoint under multiple accounts) | verified — a multi-organization record returns both blocks |
| Only instances that returned data for the record are offered in the selector | verified — a record held by one organization returns exactly that one |
| Selector labeled "Správce sbírky", rendered before the field | verified (deployed i18n `museion.organizaceId` + template); maintainer browser check, 2026-09-10 |
| Switching between instances loads that instance's data | verified (maintainer-assisted browser check, 2026-09-10) |
| Information message when no instance returns data | verified — `museion.not_found` ("Záznam se nepodařilo najít.") renders for the empty case; maintainer browser check, 2026-09-10 |
| Filter icon hidden when no Museion instance is configured | **failed** — live-tested by the maintainer 2026-09-10 (all endpoints removed from the config): `showMuseion` stays `true` in `/api/config` because the flag is an independent client-config key never derived from the configured instances — museion-D07 |
| Helper index cleared when no instance is configured | **failed** — with zero endpoints `/api/mus/index` runs no cleanup (the delete runs only per processed endpoint); `/api/mus/statistika` correctly returns `{"statistika": {}}` but the filter still narrows akce to 6 from stale helper-index entries — museion-D08 |
| Museion button on projekt, akce, samostatny_nalez result rows (docs with the `inMuseion` flag) | verified — filtered docs carry the flag; deployed results chunk gates the anchor on it |
| Results open on a separate page in a new tab | verified — deployed results chunk anchor (`museion/<ident>`, `target="_blank"`); maintainer opened the pages during the checks |
| Entity types P/A/N respected | verified — statistika ids carry `P`/`A`/`N`, including `N` for samostatny_nalez |
| BASIC-only response → simplified table; any FULL → full table | verified — deployed mode logic (any `predmetSys`/`predmetPom` `pristup === "FULL"`); maintainer visual check |
| Column order of the full table | verified — 36 columns with `pristup` first (`columnsFull` in the deployed chunk; i18n `museion.pristup`); maintainer visual check |
| `predmetSys` / `predmetPom` share columns, no duplication | verified — one merged row list against one column list |
| Dates rendered in API format (no unix timestamps) | verified — live responses carry ISO date strings; deployed cell rendering is raw interpolation with no date pipe |
| Minimum column widths | verified — deployed component CSS (220 px / 140 px / header `max-content`); maintainer visual check |
| Horizontal scrolling correctness incl. header/footer background | verified (maintainer-assisted browser check, 2026-09-10) |
| `inMuseion` filter (helper index + join) | verified — the filter narrows akce from 172231 to 6 and matching docs carry the flag |
| False zeros on BASIC records in mixed tables | verified fixed — the API serializes BASIC numeric fields as `null` and the cells render empty; maintainer visual check |
| `/api/search/museion` endpoint | verified fixed — HTTP 500 (`No enum constant ...Actions.MUSEION`): the dead action is removed from the dispatch enum; not called by the frontend |
| Empty-response robustness of the page | verified — guarded subscribe, `not_found` message, null-guarded counters |
| Responsive layout of the results page | **failed** — selector and counts still squeeze on narrow viewports (museion-D06) |
| Index refreshed by periodic job, not request cache | not verified — cron not confirmed (maintainer-assisted); `/api/mus/index` mutating, not probed |
| Admin quick actions (level E user menu) | verified (maintainer-performed check, 2026-09-10) — actions present and working |

### Defect outcomes since the 2026-09-06 verification

- museion-D01: **fixed** — `columnsFull` in the deployed lazy chunk starts with `pristup` (36 columns) and the deployed Czech i18n carries `museion.pristup` ("Přístup") with its `desc` tooltip; maintainer visual check confirmed the rendered column.
- museion-D02: **fixed** — the deployed template renders the `museion.not_found` branch when `organizaceIds` is empty, counters render null-guarded, and the maintainer browser check confirmed the message for a known record with no Museion data.
- museion-D03: **fixed** — the live API serializes BASIC numeric fields as `null` (verified on a two-organization record with 2 BASIC + FULL rows); the maintainer visual check confirmed empty cells instead of zeros.
- museion-D04: **fixed** — the unknown-id response is still bare `{}`, but the deployed subscribe is guarded and `loading` clears on every path, so the page renders the `not_found` message instead of spinning.
- museion-D05: **fixed** — `/api/search/museion` now fails with HTTP 500 (`No enum constant ...Actions.MUSEION`); the dead action was removed from the dispatch enum and the misleading 200-with-NPE body is gone.
- museion-D06: **still present** — maintainer browser check 2026-09-10: the selector and the pocetPom/pocetSys counts still squeeze on narrow viewports.
- museion-D07: **new (minted in the same-day extension of this run)** — the maintainer removed all endpoints from the config and reindexed: the filter icon stays because `showMuseion` is never derived from `museion.end_points`.
- museion-D08: **new (minted in the same-day extension of this run)** — the same zero-endpoint rebuild left the helper index stale (no cleanup executed); the `inMuseion` filter kept narrowing akce to 6 against an empty statistika.

### Known defects

- museion-D06 (cosmetic): **responsive view not optimised — the selector and the pocetPom/pocetSys counts squeeze on narrow viewports.** Maintainer-observed on 2026-09-06 and re-observed 2026-09-10; desktop rendering works as expected.
- museion-D07 (functional): **the filter icon is not hidden when no Museion instance is configured.** `showMuseion` is an independent client-config key (`Options.getClientConf()` returns the merged client config as-is); it is never derived from the `museion` server section, so removing all configured endpoints leaves `showMuseion: true` and the icon visible. Verified live 2026-09-10 by the maintainer (all endpoints removed, `/api/config` still `"showMuseion": true`) and from source (`ConfigServlet`, `Options`). Hiding the icon currently requires manually setting `showMuseion: false`; consider deriving the flag from the configured instances (or documenting the manual switch).
- museion-D08 (functional): **the helper index is not cleared when a rebuild runs with zero configured instances.** `MuseionClient.indexStatistika()` iterates the configured `end_points` and its stale-entry cleanup (`clean(start)`, `deleteByQuery` on `indextime`) executes only per processed endpoint; with an empty `end_points` array nothing is deleted, so the core keeps the previous entries. Verified live 2026-09-10: after the maintainer's reindex with zero endpoints, `/api/mus/statistika` returns `{"statistika": {}}` while `inMuseion=true` still narrows akce to 6.

### Data-state observations (not defects of this repository)

- Museion-side id hygiene: the whitespace-suffixed shapes observed on 2026-09-06 (trailing space on `M-202500178`, trailing tab on `M-202601584-N00048`) are gone from the statistika payload — the provider (Axiell) and the integration both trim now (per the issue thread). The unprefixed id (`202609962`, typ P, in ORG-000071 `amcrIdPom`, alongside the prefixed `C-202609962`) remains and cannot join onto `ident_cely`.
- On the test data, the `N`-type ids present in the Museion helper index still have no counterpart in the entities index (queries for `C-202609962-N00001`, `M-202601584-N00048`, `M-202500178-N00009` return `numFound: 0`), so the samostatny_nalez Museion filter still returns 0 — a test-data state, not a filter defect. Re-verify with fresh statistika before relying on it.

### Corrections made during this run

- One durable-knowledge correction: this scenario earlier claimed the `museion` config section drives `showMuseion` (no configured instance → icon hidden). The same-day extension of this run disproved it live — the maintainer removed all endpoints and the icon stayed, and the source (`ConfigServlet`, `Options`) shows `showMuseion` is an independent client-config key never derived from `end_points`. The durable knowledge above now states the corrected mechanism; the corresponding issue-comment check "Ověřit skrytí ikony filtru" is resolved as failed (museion-D07, museion-D08).

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-06 | digiarchiv-test.aiscr.cz (dev branch) | First verification of the Museion integration (#553): multi-instance fan-out, union, selector, BASIC/FULL table logic, filter join, admin actions verified (admin and browser checks maintainer-performed); six defects minted (museion-D01 … museion-D06, with D03 re-opening an issue-marked-fixed item); Museion-side id-hygiene and missing-entity data states recorded as observations. |
| 2026-09-10 | digiarchiv-test.aiscr.cz (dev branch) | Regression pass (#553) after the 2026-09-07 fixes: museion-D01–D04 verified fixed on the deployed build and live API, museion-D05 resolved by action removal (HTTP 500), museion-D06 still present; id whitespace trimmed on both sides, the unprefixed id remains; maintainer browser/admin checks re-confirmed selector switching, admin quick actions, and the visual table states. Same-day extension: the maintainer live-tested the zero-endpoint configuration (icon stays, helper index stale after reindex) — museion-D07 and museion-D08 minted and the `showMuseion` durable claim corrected; the test config was left with zero configured endpoints by the maintainer's test. |
