# Documentation units (dokumentační jednotky) — testing scenario

**Key:** dokumentacni-jednotky (feature)
**Scope:** the documentation-unit (DJ) block on the `akce` and `lokalita` record pages and the DJ landing page: the type-of-evidence line (`dj_negativni_jednotka`) and the PIAN block attached to each DJ. Driven by [ARUP-CAS/aiscr-digiarchiv-2#1146](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/1146). Mechanisms this feature depends on are owned elsewhere: record-level gating and permission masking by [`permissions`](permissions.md), the `komponenta` record type by [`komponenta`](komponenta.md), and landing-page shells by [`persistent-landing-pages`](persistent-landing-pages.md). This scenario records only the DJ display deviations.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Base URL | Frontend build observed on 2026-10-23 |
| --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz` | `v4.1.0-7-geafb47ea` |
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
- **Lokalita page data flow.** `lokalita.component.ts` performs the same match but assigns the full object to `dj.dj_pian` (not `dj.pian`); the lokalita template still passes `[pians]="_result().az_dj_pian"`. On the handle path the sampled lokalita carried an empty `az_dj_pian` while its top-level `pian` array held the DJ's PIAN.
- **DJ template branch model (test build).** `@if (_result().pian)` renders the PIAN object; `@else if (_result().dj_pian)` renders `<app-pian [result]="_result().dj_pian.id">`; `@else` emits a literal `C ` followed by `@for (pian of pians())`. The literal is present in the deployed chunk as `r(0,"C ")`.
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
5. **Deployed template:** fetch `/`, resolve `main-*.js` and the modulepreloaded `chunk-*.js` files, and search for `app-dok-jednotka`, `Dok. jednotka - negativn`, and `C ` in the chunk that carries the component; read the compiled branch functions.
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

## Current verification (2026-10-23, test `v4.1.0-7-geafb47ea`, production `v4.0.3-237-g96deec70-dirty`)

**Scope and limitations.** Anonymous read-only probes of both instances plus inspection of the deployed Angular chunks. The maintainer visually confirmed the `akce` and `lokalita` examples in this session. Production was used only to compare the DJ field set (the pre-fix state); the production `akce` payload already served the flag, so the pre-fix `akce` render cannot be re-derived.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Type of evidence on the DJ landing page | works — the test DJ handle carries `dj_negativni_jednotka` (true for both sampled negative DJs); the production DJ handle omits the field, and the deployed ternary then renders "pozitivní" for every DJ |
| Type mapping | works — true renders "negativní", false renders "pozitivní"; corpus correlation is true ↔ zero `dj_komponenta`, false ↔ at least one |
| Visual render on the `akce` page | works — maintainer-confirmed: both sampled DJ display "Typ zjištění: negativní" |
| Per-DJ PIAN on the `akce` page | works — the `akce` template no longer passes the parent `pian` array to DJs; each DJ receives its own `pian` object; the two sampled DJ link two distinct PIANs and each renders once (maintainer-confirmed) |
| PIAN on the DJ landing page | works — the test DJ handle serves `pian` (full object) and `dj_pian` (reference); the first template branch renders it |
| PIAN on the `lokalita` page | broken — see D01 |
| Fallback branch for a DJ without a PIAN | defect — see D02 |

### Known defects

- **dokumentacni-jednotky-D01 — Medium (open).** On the `lokalita` record page the PIAN block of a DJ no longer renders. `lokalita.component.ts` replaces `dj.dj_pian` with the full PIAN object, but the deployed DJ template's `dj_pian` branch passes `dj_pian.id` (a bare ident string) to `app-pian`; `Entity` treats `result` as the payload, so `app-pian` sees no `ident_cely` and renders nothing. Introduced by `dbf148fb` (the branch is new); before it the template passed the object held in `dj_pian`. Evidence: the deployed chunk's branch function, `lokalita.component.ts`, `entity.ts`, and the `lokalita` handle payload in which the `dj_pian` reference is replaced from the top-level `pian` array. Maintainer-confirmed visually.
- **dokumentacni-jednotky-D02 — Low (open).** The DJ template's fallback branch begins with a literal `C ` text node (`r(0,"C ")` in the deployed chunk) before looping `pians()`. It is rendered whenever a DJ has neither `pian` nor `dj_pian`; the loop then emits the caller's PIAN array. Introduced by `dbf148fb`; the old template had no literal. No sampled DJ lacked `dj_pian`, so the branch was not exercised on a record in this run; the defect is demonstrable from the deployed chunk.

### Corrections made during this run

- The first working hypothesis placed the positive/negative defect on the `akce` page only. Production's `akce` handle already includes the flag, while the production DJ handle omits it; the maintainer reports the inversion on both surfaces. On the test build the `akce` render is correct (visual), so that part is recorded as verified rather than as a reproduced pre-fix state.

### Unverified

- The pre-fix render of the `akce` page: production already serves the flag and no pre-fix build is available.
- A record whose DJ has no linked PIAN: no such DJ appeared in the sampled corpus, so D02's user-visible reach is inferred from the deployed template.
- The `noevidence` / `posevidence` search filters: not exercised; outside the issue's surfaces.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-10-23 | test `v4.1.0-7-geafb47ea`; production `v4.0.3-237-g96deec70-dirty` | First verification of #1146: the DJ handle now serves `dj_negativni_jednotka` and the `akce` page renders each DJ's own PIAN once (both maintainer-confirmed); D01 records the `lokalita` PIAN regression introduced by the fix; D02 records the stray fallback literal; the pre-fix `akce` render is left unverified because production already carries the flag. |
