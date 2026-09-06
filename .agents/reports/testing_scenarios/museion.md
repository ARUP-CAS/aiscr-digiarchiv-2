# Museion finds integration — testing scenario

**Key:** museion (feature)
**Scope:** the Museion finds integration — SOAP-driven `predmetyDleAmcrId` data for projekt, akce, and samostatny_nalez records, the `/museion/:id` results page (selector, BASIC/FULL table), the `inMuseion` search filter built on the helper index fed by `predmetyStatistika`, the config-driven filter-icon visibility, and the admin quick actions. Driven by ARUP-CAS/aiscr-digiarchiv-2#553.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — Angular frontend (client-rendered; SSR serves the shell only), API under `/api/`. Verified anonymously unless stated.
- Museion SOAP service: configured per instance in the server config `museion` section (each entry: `end_point`, `clientId`, `clientSecret` — credentials never recorded here). The test deployment carries two configured instances (organizations `ORG-000071`, `ORG-000077`). The SOAP endpoints are always app-mediated; the run never probes them directly. The upstream dev service has recurring maintenance windows around hourly builds (per the issue thread).
- `MuseionServlet` at `/api/mus/*`: `predmety_by_id?id=<AMCR_ID>` (finds per record), `statistika` (per-organization counts and id lists), `index` (helper-index rebuild; mutating, cron-driven). Responses are pretty-printed JSON.
- `/api/mus/predmety_by_id` response shapes: a record with data → `{"predmetyDleAmcrId": {"<organizaceId>": {...predmetSys/predmetPom...}}}`; a known record with no Museion data → `{"predmetyDleAmcrId": {}}`; an unknown id → bare `{}`.
- `/api/search/museion`: present in the deployed backend but dead (museion-D05); not called by the frontend.
- Search filter: the `inMuseion` query parameter on `/api/search/query` (the frontend sets it as a URL query parameter and drops it when the filter is off).
- Client config: `/api/config` — `showMuseion` flag drives the toolbar filter icon; the `museion` config section drives it (no configured instance → icon hidden).
- Results page: `/museion/<ident>` — lazy Angular route, opened in a new tab from the result row's Museion button.

### Architecture and implementation facts

- `MuseionServlet` dispatches the `/mus/*` actions; `MuseionClient` calls each configured `end_point` per query and unions the results by `organizaceId`. The same endpoint may be configured more than once under different accounts (each account = one organization) and several endpoints may serve the same organization; the union handles both. Per-endpoint failures are swallowed: an unreachable instance contributes nothing rather than failing the whole request (the SOAP client uses a short timeout per endpoint).
- Helper index: a Solr core (`museion`) holding `amcrId` + entity `type`, rebuilt by `/api/mus/index` from `predmetyStatistika`. Refresh is cron-driven; request-scoped cache expiration options were removed from the config in favour of the periodic job.
- Filter mechanics: `inMuseion=true` adds an fq join from the helper index onto `ident_cely`; matching docs additionally carry an `inMuseion` flag, and the per-row Museion button in the results list is gated on that per-doc flag. The toolbar facet itself is gated on `config.showMuseion`.
- Entity type mapping: `P` = projekt, `A` = akce, `N` = samostatny_nalez (the samostatny_nalez letter changed from legacy `S` to `N`; the statistika payload carries the type per id element).
- Results page model: `pristup` (`FULL`/`BASIC`) is per record. Table mode = FULL when any `predmetSys` record has `pristup === "FULL"`, else BASIC. Full table renders the merged `predmetSys` + `predmetPom` rows against one shared column list; the BASIC table renders the simplified single-column list. Cell values render raw (no date or number pipes) — API strings reach the DOM unchanged, which is what keeps dates in their API form.
- Selector: organizations are the keys of `predmetyDleAmcrId` (only instances that returned data for the record are present); the label is the i18n key `museion.organizaceId` ("Správce sbírky"), rendered before the field. Counts shown from `pocetPom` / `pocetSys`.
- Column headers render from i18n keys `museion.<column>` with tooltips from `museion.desc.<column>`.
- Table CSS: `table[mat-table]` uses `width: max-content; min-width: 100%; table-layout: auto`; header cells `min-width: max-content; white-space: nowrap`; `sbirka`/`popis` 220 px; `dataceVzniku`/`kontextPlocha` 140 px.
- Admin quick actions (config reload `config?reset=true`, Fedora index update `/api/fedora/index_update`, Museion index update `/api/mus/index`) live in the user menu, gated on `pristupnost === 'E'`.

### Feature or entity model

- Covered entities: projekt, akce, samostatny_nalez — each with the per-row Museion button when the doc carries the `inMuseion` flag.
- `predmetSys` and `predmetPom` share one column list (no duplication); `cislo` is the join-friendly identifier column.
- BASIC records carry only the identifier in practice, but the backend still serialises numeric fields as `0` (e.g. `pocetKusu: 0`) rather than omitting them.

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

## Current verification (2026-09-06, digiarchiv-test.aiscr.cz, dev branch, anonymous session plus maintainer-performed checks)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Multi-instance configuration (two instances on test) | verified — both organizations answer through the app-mediated API |
| Query fans out to all configured instances for both `predmetyDleAmcrId` and `predmetyStatistika` | verified (statistika covers both organizations; predmety responses can carry both) |
| Union by `organizaceId` (same endpoint under two accounts; two endpoints for one organization) | verified — multi-organization records return both blocks |
| Only instances that returned data for the record are offered in the selector | verified — a record held by one organization returns exactly that one |
| Selector labeled "Správce sbírky", rendered before the field | verified (deployed i18n `museion.organizaceId` + template); layout confirmed in the maintainer browser check |
| Switching between instances loads that instance's data | verified (maintainer-assisted browser check, 2026-09-06) |
| Information message when no instance returns data | **failed** — no message exists; see museion-D02 |
| Filter icon hidden when no Museion instance is configured | code-verified against the deployed bundle (`showMuseion` gating); the hidden state not live-verifiable without a config change — maintainer-assisted |
| Museion button on projekt, akce, samostatny_nalez result rows (docs with the `inMuseion` flag) | verified — flag present in filtered docs; button gated on it |
| Results open on a separate page in a new tab | verified — anchor with `target="_blank"` to `/museion/<ident>`; new-tab opening confirmed in the maintainer browser check |
| Entity types P/A/N respected | verified — statistika ids carry `P`/`A`/`N`, including `N` for samostatny_nalez |
| BASIC-only response → simplified table; any FULL → full table | verified — deployed mode logic (`any predmetSys pristup === "FULL"`); simplified table renders the single identifier column; confirmed working in the maintainer browser check |
| Column order of the full table | **failed** — `pristup` column missing; see museion-D01 |
| `predmetSys` / `predmetPom` share columns, no duplication | verified — one merged row list against one column list |
| Dates rendered in API format (no unix timestamps) | verified — API returns ISO date strings; deployed cell rendering is raw interpolation with no date pipe |
| Minimum column widths | verified — deployed component CSS (220 px / 140 px / header `max-content`) and maintainer browser check |
| Horizontal scrolling correctness incl. header/footer background | verified (maintainer-assisted browser check, 2026-09-06) |
| `inMuseion` filter (helper index + join) | verified — the filter narrows entity result sets (e.g. akce and projekt counts drop to the Museion-known subset) and matching docs carry the flag |
| Index refreshed by periodic job, not request cache | not verified — cron not confirmed (maintainer-assisted); `/api/mus/index` mutating, not probed |
| Admin quick actions (level E user menu) | verified (maintainer-performed check, 2026-09-06) — actions present and working |
| False zeros on BASIC records in mixed tables | **failed** — see museion-D03 |
| `/api/search/museion` endpoint | dead — see museion-D05 |
| Empty-response robustness of the page | **failed** — see museion-D02 and museion-D04 |
| Responsive layout of the results page | partially — works as expected at desktop; selector and counts squeeze on narrow viewports (museion-D06) |

### Known defects

- museion-D01: **`pristup` column missing from the full table.** The issue prescribes the full table with 36 columns, `pristup` first (record-level FULL/BASIC marker); the deployed build renders 35 columns starting with `cislo`. Verified on 2026-09-06 from the deployed lazy chunk: `columnsFull` holds the 35 names from `cislo` to `dataceUrceni` with no `pristup`, the deployed Czech i18n bundle has no `museion.pristup` label (nor a `desc` tooltip), and the only `pristup` in the chunk is the table-mode variable. The issue's granular-access resolution explicitly promised the leading access-type column.
- museion-D02: **no information message when no instance returns data — the page template instead dereferences a null model.** The issue requires an informační hláška for the no-data case. Verified on 2026-09-06: the API returns `{"predmetyDleAmcrId": {}}` for a known record with no Museion data (recipe 4); the deployed template has no empty-state branch and no corresponding i18n key, and renders the counters via an unguarded `predmetyDleAmcrId().pocetPom` / `.pocetSys` access, so the page fails to render rather than showing the message. The empty organisation list leaves the signal null after `selectOrganizace()` returns early.
- museion-D03: **false zeros for BASIC records in mixed tables.** The issue recorded this as fixed, but on the 2026-09-06 build the API still returns `"pocetKusu": 0` for `pristup: "BASIC"` records (numeric fields default to 0 instead of being omitted) and the deployed client renders cell values raw, so a mixed response shows 0 in the Počet kusů column for BASIC rows. Verified from the live API response of a two-organization record with 2 BASIC + 1 FULL `predmetSys` rows in one organization (recipe 3); the maintainer browser check confirmed the table renders those zeros.
- museion-D04: **unknown id leaves the page in infinite loading.** `/api/mus/predmety_by_id?id=<unknown>` returns bare `{}` (no `predmetyDleAmcrId` key); the deployed `ngOnInit` runs `Object.keys(e.predmetyDleAmcrId)` on it, which throws, so `loading` never clears and the progress bar spins forever. Reachable by hand-editing the `/museion/<id>` URL. Verified on 2026-09-06 from the live response shape plus the deployed chunk's `ngOnInit`.
- museion-D05: **`/api/search/museion` is a dead endpoint.** `SolrSearcher.getMuseion()` is commented out, so the action returns HTTP 200 with an NPE error body (`{"error":"java.lang.NullPointerException: ... \"jo\" is null"}`). Not called by the frontend (the filter goes through `/api/search/query` with `inMuseion`), so it is a latent API surface rather than a user-visible failure. Verified twice on 2026-09-06.
- museion-D06 (cosmetic): **responsive view not optimised — the selector and the pocetPom/pocetSys counts squeeze on narrow viewports.** Maintainer-observed on 2026-09-06; desktop rendering works as expected.

### Data-state observations (not defects of this repository)

- Museion-side id hygiene: the statistika payload contains ids with a trailing space (`"M-202500178 "`), a trailing tab (`"M-202601584-N00048\t"`), and an unprefixed id (`"202609962"`); such ids cannot join onto `ident_cely` and silently match nothing. Defensive trimming at index time would recover them.
- On the test data, the `N`-type ids (samostatny_nalez) and one `P`-type id (`M-202601584`) present in the Museion helper index have no counterpart in the entities index (queries for those idents return `numFound: 0`), so the samostatny_nalez Museion filter currently returns 0 — a test-data state, not a filter defect. Re-verify with fresh statistika before relying on it.

### Corrections made during this run

- The samostatny_nalez filter returning 0 was first suspected as a join/filter defect; resolved as a data state — the `N`-type ids held by the helper index are absent from the entities index on the test deployment.
- The `pristup` token seen in the deployed chunk (`pristup="FULL"`) was first readable as the missing leading column; corrected after extracting the full context — it is the table-mode variable, and the column itself is genuinely absent (museion-D01).
- The empty-case expectation (information message) was checked against the deployed build rather than assumed from the issue's checked items; it is unimplemented (museion-D02).

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-06 | digiarchiv-test.aiscr.cz (dev branch) | First verification of the Museion integration (#553): multi-instance fan-out, union, selector, BASIC/FULL table logic, filter join, admin actions verified (admin and browser checks maintainer-performed); six defects minted (museion-D01 … museion-D06, with D03 re-opening an issue-marked-fixed item); Museion-side id-hygiene and missing-entity data states recorded as observations. |
