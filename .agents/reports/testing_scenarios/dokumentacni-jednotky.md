# Documentation units (dokumentační jednotky) — testing scenario

**Key:** dokumentacni-jednotky (feature)
**Scope:** the documentation-unit (DJ) block on the `akce` and `lokalita` record pages and the DJ landing page: the type-of-evidence line (`dj_negativni_jednotka`) and the PIAN block attached to each DJ. Driven by [ARUP-CAS/aiscr-digiarchiv-2#1146](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/1146). Mechanisms this feature depends on are owned elsewhere: record-level gating and permission masking by [`permissions`](permissions.md), the `komponenta` record type by [`komponenta`](komponenta.md), and landing-page shells by [`persistent-landing-pages`](persistent-landing-pages.md). This scenario records only the DJ display deviations.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Base URL | Frontend build observed on 2026-10-23 |
| --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz` | `v4.1.0-11-g880f1c3e` |
| Production | `https://digiarchiv.aiscr.cz` | `v4.0.3-237-g96deec70-dirty` |

- Record pages: `/id/<ident_cely>`. They are client-rendered; the served HTML is only the application shell (see [`persistent-landing-pages`](persistent-landing-pages.md)), so the DJ block is not present in the fetched HTML.
- Record data: `/api/search/handle?id=<ident_cely>`. Search: `/api/search/query?entity=<entity>&q=<query>`. Client configuration: `/api/config`.
- Deployed frontend templates are verifiable without a browser: fetch the homepage, resolve the hashed `main-*` and modulepreloaded `chunk-*` files, and search the chunk that carries `app-dok-jednotka` for the compiled branch functions.
- Deployed build identity: fetch the homepage, resolve `main-<HASH>.js`, and read the embedded `raw:"v..."` git-describe object from that bundle.

### Architecture and implementation facts

- **Index model.** `DokumentacniJednotka.java` maps the XML element `negativni_jednotka` to the Solr field `dj_negativni_jednotka`. The `akce` document additionally carries `az_dokumentacni_jednotka` as embedded JSON and `az_dj_negativni_jednotka` as the DJ flags projected to the parent level.
- **DJ search field list.** `DokJednotkaSearcher.getSearchFields` combines `fields.common` with `fields.dokumentacni_jednotka.header` and `.detail` from `server_config.json`, then adds `pian_id:az_dj_pian`, the role-suffixed `loc_rpt`/`loc`, `katastr`, and `dj_komponenta:[json]`. A field absent from both configuration lists is not returned by the DJ handle. Commit `dbf148fb` (#1146) added `dj_negativni_jednotka` to `header` and `dj_pian` to `detail`; before it, the DJ handle omitted the flag, the template ternary read `undefined`, and every DJ displayed "pozitivní".
- **PIAN enrichment.** `DokJednotkaSearcher.addPian`, called from `getChilds`, attaches the full `pian` document to each DJ whose `dj_pian` reference resolves, using the role-filtered PIAN search fields. On the test build the DJ handle carries both `pian` (object) and `dj_pian` (reference).
- **Akce page data flow.** The akce handle returns DJs inside `az_dokumentacni_jednotka` (embedded JSON, without a `pian` member). `akce.component.ts` matches each `dj.dj_pian.id` against the akce's top-level `pian` array, sets `dj.pian` to the matched object, and updates the result signal to trigger rendering. The akce template no longer passes `[pians]` to `<app-dok-jednotka>`.
- **Lokalita page data flow.** `lokalita.component.ts` matches each `dj.dj_pian.id` against the lokalita's top-level `pian` array and assigns the matched object to `dj.pian` (not `dj.dj_pian`), then updates the result signal and the `az_dokumentacni_jednotka` signal. The lokalita template iterates `_result().az_dokumentacni_jednotka` and no longer passes `[pians]` to `<app-dok-jednotka>`; commit `880f1c3e` (#1146) made this change. On the handle path the sampled lokalita carried the DJ's PIAN in the top-level `pian` array.
- **DJ template branch model (test build).** `@if (_result().pian)` renders the PIAN object; `@else if (_result().dj_pian)` passes `_result().dj_pian.id` (a bare ident string, which renders nothing because `app-pian` needs a payload object); `@else` loops `pians()` and emits no literal text. Commit `880f1c3e` removed the stray `C ` literal from the fallback branch. The `dj_pian.id` branch is reached only when the enriched `pian` object is absent.
- **Payload contract.** `Entity` treats the `result` input as the payload object; `app-pian` renders only when `_result()?.ident_cely` is truthy. A bare ident string passed as `result` therefore renders nothing.

### Feature or entity model

- **Type of evidence.** `dj_negativni_jednotka = true` means a negative documentation unit (no finds); `false` means a positive one. Verified against the corpus: every sampled DJ with `true` had zero `dj_komponenta`, and every sampled DJ with `false` had at least one.
- Each DJ carries at most one `dj_pian` reference, and the linked PIAN is displayed inside that DJ's card.
- Display labels: `card.desc.Dok. jednotka - negativní zjištění` translates to "Typ zjištění" with values `negativní` / `pozitivní`.
- The client configuration exposes `commonFacets` `noevidence` (`-az_dj_negativni_jednotka:0 AND az_dj_negativni_jednotka:*`) and `posevidence` (`az_dj_negativni_jednotka:0`), both consistent with `true = negative`.

### Discovery recipes

1. **Negative DJs with linked PIANs:** `GET /api/search/query?entity=akce&q=az_dj_negativni_jednotka:true&rows=10&fl=ident_cely,az_dokumentacni_jednotka`; inspect each DJ's `dj_pian` in `az_dokumentacni_jednotka`.
2. **Positive DJs:** the same query with `az_dj_negativni_jednotka:false`; confirm that the DJ's `dj_komponenta` is non-empty.
3. **DJ landing page:** derive a DJ ident (`<akce>-D<nn>`) from recipe 1 and fetch `GET /api/search/handle?id=<DJ_IDENT>`; compare its field set between the environments.
4. **Lokalita with a DJ and a PIAN:** `GET /api/search/query?entity=lokalita&q=*:*&rows=20&fl=ident_cely,az_dokumentacni_jednotka,az_dj_pian`, then `GET /api/search/handle?id=<LOK_IDENT>` and inspect `az_dokumentacni_jednotka[].dj_pian` together with the top-level `pian` array.
5. **Deployed template:** fetch `/`, resolve `main-*.js` and the modulepreloaded `chunk-*.js` files, search for `app-dok-jednotka` and `Dok. jednotka - negativn` in the chunk that carries the component, and read the compiled branch functions.
6. **Build identity:** fetch `/`, resolve `main-<HASH>.js`, and read the `raw:"..."` value.

### Verification commands

```bash
# field set behind a DJ landing page (swap the host for the other environment)
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<DJ_IDENT>"
# akce payload with embedded DJs
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<AKCE_IDENT>"
# negative and positive DJ candidates
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&q=az_dj_negativni_jednotka%3Atrue&rows=10&fl=ident_cely,az_dokumentacni_jednotka"
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&q=az_dj_negativni_jednotka%3Afalse&rows=10&fl=ident_cely,az_dokumentacni_jednotka"
# deployed template chunk
curl.exe -s "https://digiarchiv-test.aiscr.cz/<CHUNK>"
```

Anonymous records need no credentials. Rate-limit a sweep to about one request per second.

## Current verification (2026-10-23, second verification; test `v4.1.0-11-g880f1c3e`, production not re-probed)

**Scope and limitations.** Anonymous read-only probes of the test instance plus inspection of the deployed Angular chunks. The maintainer visually confirmed the `akce`, `lokalita`, and DJ landing-page examples in this session. Production was not re-probed; its build identity and the pre-fix field-set comparison are the first verification's observations. The test build moved from `v4.1.0-7-geafb47ea` to `v4.1.0-11-g880f1c3e`, which includes `880f1c3e` (#1146).

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Type of evidence on the DJ landing page | works — the test DJ handle carries `dj_negativni_jednotka`; the deployed ternary renders `negativní` for `true` and `pozitivní` for `false`. The first verification observed the production DJ handle omitting the field. |
| Type mapping | works — true renders "negativní", false renders "pozitivní"; corpus correlation is true ↔ zero `dj_komponenta`, false ↔ at least one |
| Visual render on the `akce` page | works — maintainer-confirmed: both sampled DJ display "Typ zjištění: negativní" |
| Per-DJ PIAN on the `akce` page | works — the `akce` template no longer passes the parent `pian` array to DJs; each DJ receives its own `pian` object; the two sampled DJ link two distinct PIANs and each renders once (maintainer-confirmed) |
| PIAN on the DJ landing page | works — the test DJ handle serves `pian` (full object) and `dj_pian` (reference); the first template branch renders it (maintainer-confirmed) |
| PIAN on the `lokalita` page | fixed — D01 resolved: the lokalita component now assigns the matched PIAN object to `dj.pian` and the template passes no parent `pians`; the sampled DJ renders its PIAN once (maintainer-confirmed) |
| Fallback branch for a DJ without a PIAN | fixed — D02 resolved: the deployed DJ fallback function contains only the `pians()` loop and no literal text; no sampled record exercised the branch, so its user-visible reach is verified from the compiled bundle |

### Known defects

No open defects. D01 and D02 from the first verification are resolved by `880f1c3e`; their current state is recorded in the matrix and the verification log.

### Corrections made during this run

None. The first verification's D01 and D02 are confirmed fixed, not corrected.

### Unverified

- Production was not re-probed in this run; its build identity and pre-fix field-set comparison are the first verification's observations.
- A record whose DJ has neither a `pian` payload nor a `dj_pian` reference: none appeared in the sampled corpus, so the fallback branch is verified from the compiled bundle rather than from a record.
- The `dj_pian.id` fallback path (a DJ whose PIAN is referenced but absent from the parent `pian` array): not exercised on a sampled record; the branch passes a bare ident and renders nothing.
- The `noevidence` / `posevidence` search filters: not exercised; outside the issue's surfaces.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-10-23 | test `v4.1.0-7-geafb47ea`; production `v4.0.3-237-g96deec70-dirty` | First verification of #1146: the DJ handle now serves `dj_negativni_jednotka` and the `akce` page renders each DJ's own PIAN once (both maintainer-confirmed); D01 records the `lokalita` PIAN regression introduced by the fix; D02 records the stray fallback literal; the pre-fix `akce` render is left unverified because production already carries the flag. |
| 2026-10-23 | test `v4.1.0-11-g880f1c3e` | Second verification of #1146 after `880f1c3e`: the `lokalita` DJ block renders each DJ's own PIAN from the enriched `dj.pian` (D01 fixed, maintainer-confirmed) and the DJ fallback literal is gone (D02 fixed); the `akce` and DJ landing pages remain correct. |
