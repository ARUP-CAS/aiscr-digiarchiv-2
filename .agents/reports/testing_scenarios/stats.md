# Admin statistics page — testing scenario

**Key:** stats (feature)
**Scope:** the /stats page — log-usage aggregates shown to every visitor and the
admin-only index-state aggregates — across the web surface and the
search/stats API endpoints. Driving issue: ARUP-CAS/aiscr-digiarchiv-2#892.
**Principle:** concrete record ids are deliberately not embedded — record states
drift. Use the discovery recipes below to find fresh candidates; a verification
command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: https://digiarchiv-test.aiscr.cz/ (build read from the main
  bundle's `raw:"v…"` metadata). Production: https://digiarchiv.aiscr.cz/.
- `/stats` — the statistics page (StatsComponent; its template compiles into
  the main bundle, not a lazy chunk).
- `/api/search/stats` — log-core (Solr `logs`) aggregates: facets `type`,
  `entity`, `ident_cely`; range facet `indextime` (default start
  `NOW/YEAR-1YEAR`, weekly gaps; `interval` = DAY|WEEK|MONTH; `date=from,to`
  filter where either side may be `null`); `stats.field`
  `{!countDistinct=true}ident_cely`; configured `statsIpFilter` IP exclusions.
  Anonymous access works; sessions with pristupnost > C additionally get
  `user` and `ip` facets.
- `/api/search/stats_index` — admin index aggregates, gated on session
  pristupnost > C (anonymous gets 200 with `{}`): entities-core facet pivot
  `entity,stav` with `-komponenta_zdroj:samostatny_nalez`; `ruian` core facet
  (ruian_kraj / ruian_okres / ruian_katastr counts); core totals (heslar,
  organizations, osoba, uzivatel). Params: `show_deleted` (adds is_deleted
  docs), `only_visible` (non-deleted + searchable:true only; the UI then also
  hides the ruian and cores sections, while the backend still returns them).
- `/api/search/export_stats_index` — CSV export per entity / pivot / core
  (`field`, `pivotField`, `pivotValue` params; `ruian` exports from the ruian
  core). Gated like stats_index.

### Architecture and implementation facts

- The page renders its graph with echarts driven by Angular signals
  (`series`, `chartOptions`, `loading`, `index_entities`, `ruian`,
  `cores_info`). The pre-#892 version mutated plain objects after the echarts
  directive had read them, so a completed response never repainted the graph
  until an unrelated event (clicking into "Datum do") forced change detection
  — the original #892 hang. Fixed by commit 186d32c (signals); confirmed by
  the maintainer 2026-05-11 and again 2026-09-17.
- The admin index card iterates `@for (p of f.pivot)` with no null guard (the
  `f.pivot && f.value !== 'komponenta'` guard was removed by 45b9e5b together
  with the komponenta exclusion). Entities with no stav-bearing documents
  (e.g. let) yield a bucket without `pivot` and the loop renders nothing for
  it — the template relies on @for treating a missing array as empty
  (verified live on build v4.0.3-225-g45b9e5b1).
- komponenta carries no stav of its own; since 45b9e5b it inherits the parent
  root document's stav at **index time** (server_config.json
  `fields.komponenta.facets.root` contains `"stav"`; Komponenta.fillSolrFields
  copies it via setFieldsFromRoot). There is no backfill: documents not
  reindexed since the config change remain without stav, so the komponenta
  pivot sum can trail the entity total until a full reindex.
- SN-derived komponenta virtual documents are excluded from the stats pivot
  by the `-komponenta_zdroj:samostatny_nalez` filter (they deep-copy the SN
  document and would double-count under samostatny_nalez).
- The ruian section reads `[string, number][]` tuples (json.nl=arrarr); the
  template must index `c[0]`/`c[1]`. The object-shape `c.name`/`c.value` form
  renders empty rows — the "looks like an error on test, production answers
  ruian_*" state reported in #892. Fixed by 9143a73.
- The `show_deleted` and `only_visible` checkboxes re-fetch stats_index
  (getIndex); the log graph (searchStats) is unaffected by them.
- `show_deleted` and `only_visible` are parsed with `Boolean.parseBoolean`:
  the value must be the literal `true` (or `false`); `1` is silently ignored
  and yields the default view. The frontend always sends true/false.
- The admin index card lists every entity value present in the entities core,
  including child types not offered by the search UI (dokument_cast,
  dokumentacni_jednotka).

### Feature or entity model

- Two aggregate families: log-usage stats (all users) and index-state stats
  (admin only). Every entity value in the entities core appears in the index
  card, each with a stav pivot where the entity carries stav. let has no
  stav; projekt carries stavy -1..8; the others carry their domain stavy.
- Under `only_visible` only records the search layer deems searchable are
  counted: komponenta is searchable only when the parent record is archived
  and not deleted, so searchable komponenta always shows the inherited stav 3;
  samostatny_nalez appears only in stav 4; dokument only in stav 3.

### Discovery recipes

1. Deployed build and compiled template: fetch
   `https://digiarchiv-test.aiscr.cz/stats`, extract `main-*.js` from the
   shell, download it, and search `raw:"v` for the build metadata. Confirm the
   compiled stats markers: ruian rows index tuples (`exportStats("ruian",n[0],n[0])`,
   `t[0]`/`t[1]`), and no `!=="komponenta"` guard around the pivot loop (the
   single `!=="komponenta"` in the bundle belongs to the home page cards).
2. Per-entity stav presence (anonymous): compare
   `/api/search/query?entity=<E>&rows=0` with the same query plus `&q=stav:*`
   — the numFound difference is the stav-bearing population.
3. komponenta origin split (anonymous):
   `/api/search/query?entity=komponenta&rows=0&q=stav:* AND -komponenta_zdroj:samostatny_nalez`
   versus the same without `stav:*`.
4. Deleted population: as admin, read the index card with and without
   "Zobrazit smazané" and diff the per-entity totals; anonymously, OAI-PMH
   tombstones (`status="deleted"` headers in ListRecords sets on the test OAI
   endpoint) sample the same population.
5. Admin card content: a D/E test-account session reading
   `/api/search/stats_index` (optionally `show_deleted=true`,
   `only_visible=true` — the literal values matter, see above). Use the
   role-session recipe documented as recipe 10 of the `permissions` scenario
   (header-dump login, then URL-first cookie replay) — it is the shape the
   verifying environment's allowlist admits.

### Verification commands

- `curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/stats" -o stats.json -w "%{http_code}"` —
  anonymous; expect 200 with `facet_counts.facet_fields` and indextime ranges.
- `curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/stats_index" -o si.json -w "%{http_code}"` —
  anonymous; expect `200 {}` (content is pristupnost-gated).
- `curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=<E>&rows=0&q=stav:*" ...` —
  recipe 2; replace `<E>` per the recipe.
- `curl.exe "https://digiarchiv-test.aiscr.cz/api/search/stats_index?show_deleted=true" -H "Cookie: JSESSIONID=<SID>" ...` —
  D-session variant (recipe 5 and the permissions scenario's recipe 10);
  replace `<SID>` from the login.
- Rate-limit gently; credentialed probes only at maintainer direction.

## Current verification (2026-09-17, test build v4.0.3-225-g45b9e5b1)

Evidence sources this run: anonymous API and bundle probes from the verifying
session, direct D-role stats_index fetches (default, show_deleted=true,
only_visible=true) through the permissions scenario's role-session recipe,
the three admin-view reads (default, show_deleted, only_visible) supplied
in-session by the maintainer, and maintainer in-session confirmations of the
admin browser-console state and the CSV export from the index card.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Anonymous stats page and API load | Verified — /api/search/stats 200 with full facet shape; shell and bundle served |
| Admin stats page completes without interaction (original #892 hang) | Verified — maintainer read all three index-card states from the admin view in-session; signal fix (186d32c) also confirmed 2026-05-11 |
| ruian section renders production-shaped values | Verified — ruian_katastr(13091), ruian_kraj(14), ruian_okres(77); compiled tuple form confirmed in the deployed bundle (9143a73 live) |
| komponenta split by inherited stav | Verified — pivot renders stav 3(69357), 1(2896), 2(2214); coverage 74467/220543 — see stats-D01 |
| show_deleted toggle | Verified — +167 docs across 9 entities: pian +49, dokument +34, projekt +25, akce +19, adb +16, knihovna_3d +10, ext_zdroj +8, let +4, lokalita +2; unchanged for dokument_cast, dokumentacni_jednotka, komponenta, vyskovy_bod, samostatny_nalez. The in-thread state "the toggle changes only a single samostatny_nalez" no longer reproduces |
| only_visible toggle | Verified — searchable-only population (komponenta 211264, all stav 3 = 69357; samostatny_nalez only stav 4; dokument only stav 3); ruian and cores sections correctly hidden |
| Admin reads vs anonymous probes cross-check | Verified — komponenta only_visible total (211264) and stav:3 count (69357) equal the anonymous search-visible counts exactly |
| let bucket renders without pivot | Verified — let(486) shows no stav rows and the card renders; the unguarded pivot loop tolerates the missing array |
| D-session stats_index fetches (default / show_deleted=true / only_visible=true) | Verified first-hand — all values match the maintainer reads (let 486→490 with deleted; komponenta 211264 visible-only with a single stav-3 pivot of 69357; ruian tuple array and cores present in every backend response) |
| exportStats CSV buttons | Verified — maintainer-confirmed in-session (D/E export from the index card works) |
| Browser console state on /stats as admin | Verified — maintainer-confirmed in-session (console clean) |

### Known defects

- stats-D01: komponenta stav pivot is incomplete until a reindex — 74467 of
  220543 non-deleted komponenta docs carry stav (146076 do not), because
  45b9e5b inherits stav at index time only. Verified 2026-09-17 from the
  maintainer's admin-view read and anonymous search-side counts (69357 in
  both). Action: full or komponenta-targeted reindex. Severity Low
  (admin-only statistics display; self-resolves per record as records are
  reindexed, but the split misrepresents coverage until then).

### Pre-existing observations (not introduced by the #892 changes)

- The "Datum od" clear-button guard is inverted (`@if (!datumod)`, versus
  "Datum do"'s `@if (datumdo)`): ngOnInit always defaults datumod to a year
  ago, so the clear affordance never shows while a date is set. Blame dates
  the lines to the #443-era commits (ed9c475 / 5cc9462, Oct 2025).
  Non-verdict-bearing; recorded for the maintainers.
- projekt's pivot carries stav 0 (48, 55 with deleted) and -1 (10, 15 with
  deleted) values — data-state observation recorded for the maintainers; out
  of #892's scope.

### Corrections made during this run

- The run first concluded that direct credentialed stats_index probes were
  unavailable in this environment (a cookie-jar curl shape was refused by the
  command allowlist). The permissions scenario's role-session recipe
  (header-dump login plus URL-first cookie replay) is admitted and performed
  the same probes successfully, so the conclusion is corrected: the D-gated
  content above is verified first-hand, not only through the maintainer's
  reads. This was a tooling-shape issue in the verifying environment, not a
  subject defect.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
