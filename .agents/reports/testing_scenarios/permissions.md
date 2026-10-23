# Permission model — testing scenario

**Key:** permissions (feature)

**Scope:** the permission behaviour of every backend surface that serves record data — landing pages, OAI-PMH, search/handle/legacy APIs, geometry and export endpoints, the File API, the image and PDF endpoints, and the admin/integration servlets — at record level and element level, for all roles. Issues [ARUP-CAS/aiscr-digiarchiv-2#370](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/370), [#237](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/237) (rules definition), and [#1151](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/1151) (SN protected-element visibility for an archaeologist's own projects) drove it.

**How to use this record:** the *Durable knowledge* half states the rules and the complete surface inventory; the **Current verification** block carries the latest dated verification and supersedes the prior one; the **Verification log** holds one entry per past run. Coverage cells record the observed result — `✓` = matches the documented rule, `✗` + finding id = deviation, `·` = no surface for that type by design, `—` = **not examined in that run**. A later run starts from the durable half and the log; superseded matrices are recoverable from this file's history, not retained beside the current one. Fill or annotate cells only with fresh evidence, never from assumption.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | `https://api-test.aiscr.cz/2.2/oai` | `main-X4LCF425.js`, embedded build `v4.1.0-11-g880f1c3e` (2026-10-23). The backend exhibits both #1151 fixes (`e73b5c17` removes the `stav == 4` early grant; `7f97f99e` adds `historie:[json]` to the search projection), so the deployed backend is at or after `7f97f99e`; the bundle's base commit `880f1c3e` is not in the local checkout. Establish the backend by behaviour, not by the bundle or footer literal. Earlier verified builds: `v4.1.0-7-geafb47ea` (pre-fix, 2026-10-23), `v4.0.3-236-g41592838` (2026-09-24). |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | `https://api.aiscr.cz/2.2/oai` | `v4.0.3-237-g96deec70-dirty` (commit `96deec70`, dev HEAD merged by the release PR #1109; release tag `v4.1.0` peels to `d1f084bf`). The v4.1.0 release does not carry the #1151 fixes (`e73b5c17`, `7f97f99e`); its permission behaviour is the pre-fix state and was not re-probed on 2026-10-23. D07, D14 and D19 are closed there as of 2026-09-29. The earlier hotfix removed the FedoraServlet read actions (`GET_ID`/`REQUEST`), closing the D13 raw-read path. `/thumb-large` absent (404). |

OAI quirks: OAI identifiers use the **environment's own domain prefix** (`https://api-test.aiscr.cz/id/…`, `https://api.aiscr.cz/id/…`); GetRecord with the other environment's prefix silently fails with `idDoesNotExist`. Set names containing a colon must be percent-encoded (`set=dokument%3A3d`). Versioned OAI endpoints (`/2.1/oai`, `/2.2/oai`) serve the same filtered core and differ only by XSLT. The **version-less** endpoint `https://api.aiscr.cz/oai` 302-redirects to the current version — a convenient Basic-auth role-probe shape.

### Surface inventory (routes and gates)

Complete enumeration from `web.xml` + `@WebServlet` annotations (2026-09-10 source audit, amended on later passes). `ApiServlet` (`/api`, also `/api/*` in web.xml) is a pure path-normalizing forwarder, so every action servlet is also directly reachable under its own path (e.g. `/search/query` ≡ `/api/search/query`).

| Route | Servlet / action | Gate mechanism | Permission-relevant behaviour |
| --- | --- | --- | --- |
| `GET /id/<ident>`, `GET /map/<ident>` | `HandleServlet` `checkId` | 404 (no doc in `entities`) → 410 (`is_deleted`) → 401 (`!searchable` + anonymous) → 403 (per-model `filterOAI`) → 200 | landing / map gating; the query projects `pristupnost`, `stav`, and for SN also `projekt`/`projekt_organizace`/`predano_organizace`, so the SN organisation clauses resolve on this path; rendered page is a shell, data comes from the handle API |
| `GET /id/<ident>/file/<uuid>` (+ `/thumb`, `/thumb/page/N`, `/thumb-large`, `/paradata`, `/paradata/<distribution>`, distribution suffixes per #693) | `HandleServlet` file path | `HandleServlet.isFileAllowed` per XPath rule row; #693 rule: distributions and paradata follow the original's rules; Basic auth and session cookie both accepted here (not on `checkId` — Basic-auth landing requests are treated as anonymous); rate limiter per IP (≥ 1 s between requests, also across distinct files) | File API; `/thumb` documented ungated; `/thumb-large`, `/thumb/page/N`, `/paradata` follow the original's rules; #693 distributions and paradata follow the original's rules (see the [`file-distributions`](file-distributions.md) scenario) |
| `GET /oai/*` (verbs GetRecord/ListRecords/ListIdentifiers; oai_amcr, oai_dc) | `OAIServlet` → `OAIRequest.filter` | per-model `filterOAI` (record level) + element string surgery (chranene/oznamovatel) | OAI-PMH; Basic auth logs in; oai_dc derived after filtering |
| `GET /api/search/query` | `SearchServlet` QUERY → per-entity `EntitySearcher` | pristupnost-suffixed projected fields + `searcher.filter(jo, request)` post-filter | search visibility + element masking; E downgraded to D; restricted records' public fields are served anonymously by design; for SN the protected block follows `SamostatnyNalezSearcher.isAllowed` — the `pristupnost` gate and the record-access arms decide it; the `stav=4` unconditional grant is gone (D20 fixed), and the C organisation arms are ineffective on the handle path (pre-existing #244 state) |
| `GET /api/search/handle?id=` | `SearchServlet` HANDLE | per-model `filterOAI` on the entities doc + the searcher's `filter(jo, request)` | record JSON for landing pages; for SN the protected block follows `isAllowed` (D20 fixed; the C organisation arms are the pre-existing #244 state) |
| `GET /api/search/id` | `SearchServlet` ID | searcher field projection + element masking; **no** `filterOAI` record-level gate (same posture as `/api/search/query`) | legacy id lookup — reachable on both deployments; serves searcher-filtered, element-masked docs (`?id=<non-searchable ident>` returns `numFound: 0`); for SN the protected block follows `isAllowed` (D20 fixed; the C organisation arms are the pre-existing #244 state). It is not a removal-guarded route; do not treat it as absent |
| `GET /api/search/id_as_child` | `SearchServlet` ID_AS_CHILD | child-doc fetch | child record JSON (verify per parent state) |
| `GET /api/search/gml`, `/wkt`, `/geometrie` | `SearchServlet` GML/WKT/GEOMETRIE | GEOMETRIE takes `id` (pian ident) plus `loc_rpt` as a **4-value bbox** (`lat1,lng1,lat2,lng2`) and a required `format` (`GML`/`GeoJSON`/other=WKT); it queries the role-suffixed `loc_rpt_<pr>` field (masks restricted pians for anonymous); its pristupnost check is commented out in source (latent if the suffixing is ever dropped); reads `pian_chranene_udaje` blocks | geometry extracts |
| `GET /api/search/pians` | `SearchServlet` PIANS → `PIANSearcher.getMapPians` | requires a `rows` parameter (missing rows → NumberFormatException error body); projects `pian:[json]`, `pian_id`, `ident_cely`, `organizace`, `pristupnost` and role-suffixed `loc_rpt`/`loc` | map pian points — idents without coordinates below the role |
| `GET /api/search/mapa`, `/export`, `/export_mapa` | `SearchServlet` MAPA/EXPORT/EXPORT_MAPA | per-searcher filter (same as query) | map search + export variants |
| `GET /api/search/stats`, `/stats_index`, `/export_stats_index`, `/období`, `/thesauri`, `/log`, `/check_relations`, `/home` | `SearchServlet` auxiliary actions | aggregate/heslar/analytics | statistics and vocabularies — low sensitivity, spot-check |
| `GET /exp` | `ExportServlet` → `searcher.export(request)` | **no servlet-level gate**; protection only inside `EntitySearcher.export` (per-searcher `filter()`); map path reads `*_chranene_udaje` geometry | csv/xlsx/xml/json export; `mapa=true` serves geometry (GeoJSON/GML); `entity=projekt` serves records and restricted projekts export without the chranene block |
| `GET /pdf?id=<soubor_id>&page=N` | `PdfServlet` | `ImageAccess.isAllowed(request, true)` — the gate resolves the dokument by the **soubor id** (`getDokBySoubor`); a record-ident `id` resolves to nothing and returns **401 as a not-found**; SN always allowed (issue #85), dokument by `pristupnost` | PDF page images from the `thumbsDir` cache (or an empty placeholder when absent); gated on both deployments |
| `GET /img/thumb?id=<soubor_id>`, `/img/full?id=` | `ImageServlet` actions (only `thumb` and `full` exist — `thumb-large` is not a valid action and 500s) | `thumb` ungated (`ImageAccess.isAllowed(…, false)` = true); `full` gated by `ImageAccess.isAllowed(…, true)` + rate limiter | image variants by soubor id; `ImageAccess` full gate: SN → **always allowed** (issue #85), dokument → imgPr=A or userPr ≥ imgPr or same-org (same-org restricted to users ≤ C); a dokument PDF on `/img/full` may return 500 from the backend path (see pre-existing observations) |
| `GET /fedora/*` | `FedoraServlet` (actions: INDEX_FULL, STOP_INDEX, INDEX_UPDATE, STOP_UPDATE, INDEX_ENTITIES, INDEX_MODEL, INDEX_ID, REINDEX_FILTER, CHECK_DATESTAMP — **GET_ID/REQUEST removed** on both deployments) | allowedIP list OR localhost OR `pristupnost >= indexSecLevel` (config, default E) — **the gate trusts the network position: unauthenticated local-network/VPN clients pass the gate** (the 500 no-enum error for an invalid action is raised inside the gate-passed branch); public-internet anonymous denied | raw Fedora object access + reindex — admin surface; the raw record XML read paths are closed on both deployments (read actions removed, permissions-D13 closed — the network-trusting posture and mutating actions accepted as-is by operator decision) |
| `GET /mus/*` | `MuseionServlet` | pristupnost read (default A when anonymous); FORBIDDEN paths present — gate shape unclear from source | museion predmety integration — reachable, empty test data, no demonstrable leak |
| `GET /fav/*` | `FavoritesServlet` | session `userid` scoping | per-user favourites only |
| `GET /user/*` | `LoginServlet` | — | login/logout/islogged (credentials, not record data) |
| `GET /texts/*`, `/config/*`, `/i18n/*`, `/feedback` | Texts/Config/I18n/FeedbackServlet | — | UI chrome, no record data |
| JSP shells: `/results`, `/export`, `/export-mapa`, `/print`, `/stats`, `/museion`, `/favorites`, `/home`, `/id2/*`, `/map2/*`, `/registrace`, `/napoveda` | `StaticServlet` (index.jsp) | none (client-side) | all data comes from the API routes above |

`PausedFilter` on `/*` is an availability gate only. `ApiServlet.getSafePath` blocks `..` and backslashes; traversal-shaped input dies as 404.

### Architecture and implementation facts

- **Solr core routing decides landing-page existence.** `HandleServlet.checkId` queries the **`entities`** core. Top-level types there: `projekt`, `archeologicky_zaznam` (akce/lokalita), `dokument` (incl. 3D), `adb`, `pian`, `samostatny_nalez`, `ext_zdroj`, `let`. Child entities there: `dokumentacni_jednotka`, `komponenta`, `komponenta_dokument`, `dokument_cast`, `vyskovy_bod`, `neident_akce`. Types `uzivatel`, `heslo`, `ruian_*`, `organizace`, `osoba` index into their own cores → **always 404 on landing pages** (intended feature). The **`oai`** core serves OAI-PMH for all types, so OAI-PMH can serve records the landing page cannot.
- **filterOAI call sites use fresh model instances.** Both `HandleServlet.checkId` and `OAIRequest.filter` obtain the model via `FedoraModel.getFedoraModel(entity)`, which returns a **new, unpopulated instance**. A `filterOAI` implementation must read record state from the `SolrDocument` argument; model instance fields (e.g. `stav`) are default-valued on these paths. `SamostatnyNalez.filterOAI` reads the doc's `stav`/`pristupnost`/`historie` plus the projected `projekt`/`projekt_organizace`/`predano_organizace` fields, so its A/B rows follow #237's `stav=4` rule and its C organisation clauses resolve on both the entities path and OAI.
- **Where the rules live:** record-level rules in `FedoraModel.filterOAI` implementations (`web4/fedora/models/*.java`); file rules in `HandleServlet.isFileAllowed`; image rules in `ImageAccess`; element-level hiding in `OAIRequest.filter` (OAI) and via pristupnost-suffixed secured fields plus per-searcher `filter()` post-removal (search/handle/export).
- **The suffix mechanism has two halves and both must hold:** the query must project role-suffixed variants (`loc_rpt_<pr>`, `lat_<pr>`, suffixed secured JSON fields) **and** the post-filter must remove the unsuffixed block when `doc.pristupnost > userPr`. A searcher whose projected fields omit `pristupnost`, or whose `filter()` throws on a missing field, silently serves the unsuffixed block (permissions-D12 shape on projekt; D11 on komponenta — both fixed). `SolrSearcher.getSufixesByLevel(pr)` returns the suffixes at or below the session role; `prSufixAll = {A,B,C,D}`.
- **The legacy `/api/search/id` action runs the same searcher field projection and element masking as `/api/search/query`** (verified 2026-09-29 on both deployments), and returns `numFound: 0` for a non-searchable ident. It does not run the record-level `filterOAI` gate, so it serves a restricted record's public fields anonymously — the same by-design posture as the search API, not a record-level bypass. The earlier recorded "action removed" state no longer holds; the route is live.
- **SN protected-element masking has two implementations with different published scopes.** `OAIRequest.filter` hides `chranene_udaje` whenever the record's `pristupnost` exceeds the user's role — the OAI element table. `SamostatnyNalezSearcher.filter` delegates to `isAllowed`, which applies `pristupnost <= userRole` first and then the SN record-access rule (`SN01` owner, project organisation, `predano_organizace`); the `stav == 4` early return was removed by `e73b5c17`, closing D20. `historie` is now in the search projection (`historie:[json]`, `7f97f99e`), so the `getJSONArray("historie")` read no longer throws (D21 fixed); `historie` was already served anonymously by OAI, so the projection is not a new disclosure class. The C organisation and `predano` arms read `samostatny_nalez_projekt` and `samostatny_nalez_predano_organizace`, which the handle document served for a non-searchable SN omits; `optString` returns `""` rather than `null`, so the `if (projektId == null)` fallback never runs and those arms do not grant the block — the pre-existing state tracked in [#244](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/244), not a #1151 regression. The B owner arm works because the `SN01` author is present in `historie`. The searcher surfaces are `/api/search/query`, `/api/search/handle` and `/api/search/id`; export's field set does not carry the block; OAI still masks it per its own table. #1151's scope is record access; element-level full access for the C organisation is #244's scope.
- **`historie` is now in the searcher projection.** `7f97f99e` added `historie:[json]` to `SamostatnyNalezSearcher.getSearchFields`, so `isAllowed` can read the `SN01` author without throwing (D21 fixed). `historie` was already served anonymously by OAI, so the projection is not a new disclosure class; it does increase the searcher response size.
- **Restricted `stav=6` projekts are a distinct masking population.** A stav=6 projekt is anonymously visible at record level, so its search and handle documents reach anonymous clients and rely entirely on the post-filter to drop `projekt_chranene_udaje`. A D12 sweep must include `stav:6` explicitly (recipe 9) — the non-stav-6 restricted population alone does not cover it.
- **`/map/<ident>`** applies the identical gating before forwarding to `/map2`. Bare `/map` forwards to `/map2` (permissions-D01 fixed).
- **Rate limiting:** the File API and `/img/full` limiter throttles **per IP**, including bursts across distinct files — keep **≥ 1 s between requests** and at most ~3 per burst; a violation returns **429** (`Retry-After` observed `0` on the interval branch and `500` on the concurrent branch — the latter reads as milliseconds in a header defined in seconds). The limiter keys on client IP, so proxy buffering or egress-IP rotation can defeat observation from outside.
- **Page cache:** on test (`isTestEnv=true`) the landing-page cache is skipped entirely; on production a 1-day anonymous cache exists but only affects the rendered shell (record data is always fetched live per user), so it has no permission impact. The `/pdf` page-image cache is consulted only after the `ImageAccess` gate, so a warm cache cannot turn a gate refusal into a served page.
- **Child-entity indexing:** `dokumentacni_jednotka` docs stay `searchable=true` and are gated in `filterOAI` by the parent-propagated `stav` (anonymous gets 403, not 401, for a non-archived parent); `dokument_cast` inherits the parent dokument's `searchable` (401 shape); `vyskovy_bod` inherits `stav`/`pristupnost`/`searchable` from the ADB at index time. VB geometry fields (`vyskovy_bod_geom_wkt`, `vyskovy_bod_geom_gml`) are indexed **without pristupnost suffixes**, so `VyskovyBodSearcher.filterOne` must remove them when `doc.pristupnost` exceeds the session role; with that filter in place no deployment serves them to anonymous clients restricted above A (D07 closed).
- **Entity-search stubs (pre-existing):** `/api/search/query?entity=dokumentacni_jednotka` and `entity=dokument_cast` return error bodies (searcher stubs). Neither is a data exposure; the UI does not surface them as top-level entity tabs.
- **The handle API never serves `projekt_oznamovatel`** — the oznamovatel element is not in the projekt searcher's field list; the OAI XML side applies the oznamovatel rules in `OAIRequest.filter`.

### Feature or entity model

**Roles / pristupnost:** A anonym, B badatel, C archeolog, D archivář, E admin/ARÚ (E is also granted synthetically to users with `cteni_dokumentu`). Heslar ids: `HES-000865`=A … `HES-000868`=D. Comparison is lexicographic on the letter (A < B < C < D < E); the search servlets downgrade E to D.

**Record-level visibility (landing page / OAI record body):**

| Type | A | B | C | D-E |
| --- | --- | --- | --- | --- |
| projekt | stav=6 | stav=6 | stav>=1 | any |
| archeologicky_zaznam | stav=3 | any | any | any |
| dokument, ext_zdroj, adb | stav=3 | any | any | any |
| pian | stav=2 | any | any | any |
| samostatny_nalez | stav=4 | stav=4 or SN01 owner | + org match (projekt/predano) | any |
| uzivatel | never | self only | self only | any |
| let, heslo, ruian_*, organizace, osoba | any | any | any | any |

The deployed `SamostatnyNalez.filterOAI` follows the table on all rows; the C-branch organisation clauses (project organisation and receiving organisation, `predano_organizace`) resolve on OAI and on the entities path, and the B/C owner arms compare the **SN01 author's identity** (`userId.equals(uzivatelSN01)`) on both the file gate and the record filter (permissions-D08, D16 fixed). The rules are identical on both deployments.

**`searchable` flag (drives anonymous 401 vs 403):** AZ = stav 3; dokument = stav 3; samostatny_nalez = stav 4; pian = ident does not start with `N`; projekt = has related dokument/samostatny_nalez/akce; adb, let, ext_zdroj = always searchable; uzivatel = field not set (moot, 404 anyway); **child entities** — komponenta and dokument_cast inherit the parent's state; dokumentacni_jednotka and vyskovy_bod are `searchable=true` at index time and rely on `filterOAI` for gating (DJ reads the parent-propagated `stav`; VB inherits the ADB's full state triple).

**Element-level (inside a 200 response):**

- `chranene_udaje` (katastr, lokalizace, geometrie) hidden when record pristupnost > user pristupnost. Applies to projekt, archeologicky_zaznam, adb, pian, samostatny_nalez; **dokument and ext_zdroj have no element-level restriction** by spec.
- **SN under #1151:** the published OAI element table protects `samostatny_nalez/chranene_udaje` by the record's `pristupnost`; OAI implements it. The searcher path applies `isAllowed`: below the record's role, the block is removed unless the caller is entitled (SN01 owner for B; SN01 owner, project organisation, or `predano_organizace` for C); D/E always see it. After the fixes, the unconditional `stav=4` grant is gone (D20 fixed) and the `historie` read no longer throws (D21 fixed). The C organisation arms are ineffective for a non-searchable SN because the handle document omits the project and `predano` fields — the pre-existing state tracked in [#244](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/244), not a #1151 regression. OAI remains stricter by design; #1151's scope is record access, while element-level full access for the C organisation is #244's scope.
- `projekt/oznamovatel`: A-B never; C only stav=1 or own organisation; D-E always. The handle API does not serve the element at all (see architecture facts).
- In OAI-PMH responses, restricted elements are replaced by the literal `HTTP/1.1 403 Forbidden`; restricted whole records get `<amcr:amcr…>HTTP/1.1 403 Forbidden</amcr:amcr>`; deleted records carry `status="deleted"` headers.
- An entity's deviation from this mechanism is recorded in that entity's scenario (for komponenta, see the [`komponenta`](komponenta.md) scenario).

**File API** (`https://…/id/<ident>/file/<uuid>[/thumb][/thumb/page/N]`, Basic auth or session cookie; see <https://arup-cas.github.io/aiscr-api-home/file-api/>):

| XPath | A | B | C |
| --- | --- | --- | --- |
| `//projekt/soubor` | never | never | stav=1 OR (stav 2-6 AND own org) |
| `//dokument/soubor` | pr=A AND stav=3 | (pr<=B AND stav=3) OR own (D01) | (pr<=C AND stav=3) OR own org (D01 creator's org) |
| `//samostatny_nalez/soubor` | stav=4 | stav=4 OR own (SN01) | stav=4 OR own (SN01) OR own org (predano/projekt) |

Deployed state (source `HandleServlet.isFileAllowed`, both deployments — the release adds no permission code over the test build):

- **projekt** — implements the published row exactly (permissions-D15 fixed).
- **dokument** (and `knihovna_3d`) — implements the published rows: A `pr=A AND stav=3`; B `(pr<=B AND stav=3) OR D01 owner`; C `(pr<=C AND stav=3) OR D01 creator's organisation`. The owner and creator-organisation arms work for `stav≠3`.
- **samostatny_nalez** — implements the published rows: A/B/C `stav=4` without pristupnost conditions; B and C owner arms on the **SN01 author's identity** (`userId.equals(uzivatelSN01)`); C organisation arms on `samostatny_nalez_predano_organizace` and the projekt organisation. The dokument C row's D01 creator-organisation arm is published behaviour and distinct from the SN owner arms.

Small thumbnails (`/thumb`) are **always public**; large thumbnails (`/thumb/page/N`, `/thumb-large`) and `/paradata` follow the original-file rules. **File distributions and paradata are additional File API surfaces (#693):** `/id/<ident>/file/<uuid>/{dist}` (any non-reserved suffix resolves as a distribution, `orig` included) and `/id/<ident>/file/<uuid>/paradata[/{dist}]` (the bare form serves the paradata of `orig`, always `text/plain`). They resolve through the same `getDocumentFromFile` lookup and the **same `isFileAllowed` gate** as the original file, so the rule table above governs them identically: a gate change on the original (the D09 fix, the D18 collateral) propagates to distributions and paradata **by design** — it is #693's contract working, not a separate divergence. An unknown distribution returns 404; the rate limiter covers all forms; the `soubor_distri` search facet exposes the live distribution paths. The full surface contract — including the internal reader `/api/img/*` surface, which is deliberately undocumented and must not be "fixed" to match — and the limiter facts live in the [`file-distributions`](file-distributions.md) scenario. **Image endpoints** (`/img`) apply their own `ImageAccess` rules (see surface inventory) — SN always allowed at full size, dokument by pristupnost. **`/pdf`** shares those rules and is gated on both deployments (D14 closed).

**Child records:** child entities live in the entities core with their own idents derived from the parent: `dokumentacni_jednotka` = `<AZ>-D01…`; `komponenta` (under AZ or SN) = `<parent>-K001…`; `dokument_cast` = `<dokument>-D001…`; `vyskovy_bod` = `<ADB>-V0001…`.

### Probe-record registry

Anchors verified on the deployed builds. **Drift rule:** records change state; re-verify each record's current state (OAI or search) before using it as a probe, and re-derive replacements via the recipes when it has drifted. The same ident can hold **different states in the two environments** — never assume parity. File UUIDs come from the record's `soubor` elements and are **environment-specific** (the same record can bind different file UUIDs on test and production — re-read the soubor path per environment).

| Ident | Type | Properties that make it a probe | Surfaces |
| --- | --- | --- | --- |
| `C-202009490A` | akce | pristupnost C, archived (stav=3), restricted element case; carries DJ `-D01` and komponenta `-K001` | landing, OAI, search, handle, file (children), element ladder |
| `M-200500013A` | archeologicky_zaznam | pristupnost B — element ladder one step up | landing, element ladder |
| `C-202204147A` | akce | test: unarchived → anonymous 401 case (production: 200, archived) | landing |
| `C-TX-193001369` | dokument | test: unarchived → anonymous 401 (production: 200, archived); carries dokument_cast `-D001` | landing, children |
| `M-TX-202100125` | dokument | pr=C, stav=3 — restricted file sample; test file `3a0f7078-e26f-404d-aaa9-2b5abbb5e3d2`, production file `fe377f16-1da6-4912-b8d4-6f3095324879` (soub-340129 — the soubor id is the same in both environments) | file API, `/pdf`, `/thumb-large` |
| `C-TX-192700656` | dokument | pr=A, stav=3 — file-gate A-row positive; file `2c5db7d7-95fd-46a2-9859-819cadf72fb2` | file API |
| `C-TX-202400188` | dokument | pr=D, stav=3, organizace ORG-000091 — D18 B-row probe and C organisation-arm sample; file `e74e2652-cd97-4dda-9918-80a78d139e38` | file API |
| `M-TX-202100123` | dokument | pr=D, stav=3, organizace ORG-000077 (foreign) — D18 B/C-row probe; file `08c40264-40c0-4582-892d-66c00aff747c` | file API |
| `X-C-TX-000001130` | dokument | pr=D, stav=1, created by the B test account (D01 owner; creator org ORG-000091) — B my-record arm and C organisation arm; file `1a33a96a-3264-4949-aaba-25c5b4a2937b` | file API |
| `ADB-PRAH71-000861` | adb | test: pr=C, stav=2; production: pr=A, stav=1 — restricted-state case (record refused at record level by stav; the chranene block is pr-legitimate where pr=A); carries VB children | landing, OAI, search, handle, children |
| `P-0134-000003` | pian | stav=2, state-matching — public baseline | landing, OAI |
| `P-2213-100119` | pian | pr=C on test; production pr=A — restricted chranene geometry probe | search, geometrie, `/exp` mapa |
| `C-LET-00001` | let | open type sample | landing, OAI |
| `C-202402033` | projekt | **test** deleted tombstone (410 on landing and handle, OAI `status="deleted"`); production: 401 (not deleted — re-derive there) | search, handle |
| `C-201122587` | projekt | stav=6 with oznamovatel (PII masking case) | OAI oznamovatel |
| `M-201300453` | projekt | stav=6, pr=A — public stav=6 landing sample (200 on both environments) | landing, search |
| `M-200500013` | projekt | production: pr=A, stav=6 — anonymously visible stav=6 projekt whose chranene block is pr-legitimate; test: used as a stav=6 probe | search, handle |
| `C-202600010` | projekt | production: exists, not searchable → anonymous landing **401** and `/api/search/id` `numFound: 0`; test: restricted stav=3 → 403 | landing, OAI, `/api/search/id` |
| `C-202101848` | projekt | stav=6, organizace ORG-000030; PDF file `06fbe1a2-62cc-4d5b-9674-abbd8c9754a4` | file API (D15 A/B/C negative) |
| `C-202500044` | projekt | stav=4, organizace ORG-000091 (own); file `0c06098a-a0a1-42fb-ab92-7bc9dd61dacc` | file API (C own-org positive) |
| `C-202007460` | projekt | stav=6, organizace ORG-000091 (own); file `ed5eaf7b-4087-4a89-936b-34c568fc1886` | file API (C own-org positive; A/B negative) |
| `C-202111855` | projekt | stav=1; file `9cc29a95-c95e-4f92-a4e1-db3d74489c4a` | file API (C stav=1 arm) |
| `X-C-000025454` | projekt | **production deleted tombstone** (fresh; OAI `set=projekt` `status="deleted"`, datestamp 2026-09-29) — 410 on landing and `/map/` | landing, map, OAI |
| `C-202508186` | projekt | production stav=6, pr=B — restricted stav=6 masking sample (appears in the D12 sweep) | search, handle |
| `M-202600319-N00030` | samostatny_nalez | production pr=C, stav=4 — anonymous landing 200 (public-archived SN) | landing, search |
| `C-TX-202600554` | dokument | production pr=A, stav=3 — public dokument with a PDF file; `soub-826403` — `/pdf` positive control | `/pdf`, `/img/full` |
| `C-202600009-N00014` | samostatny_nalez | pr=C, stav=1, created by the B test account (owner clause; projekt organisation ORG-000091); file `c74d3136-3cab-468a-8405-3f79b89ab555`; carries komponenta `-K001` | landing, OAI, file, children, backend |
| `C-202600010-N00085` | samostatny_nalez | pr=C, stav=4 (public-archived SN); files `cafc9a90-3b8b-40f8-ace6-5791b0b6ffd2` (soub-653685), `e5115f1a-72ce-4eaa-a2d9-f52544aaa370` (soub-653686); carries komponenta `-K001` | landing, OAI, handle, file, backend |
| `C-202500044-N00001` | samostatny_nalez | pr=A, stav=4 — file-gate A-row positive; file `04d822bb-2706-430e-9ef2-720f8dcb5b8a` | file API |
| `M-202301371-N00006` | samostatny_nalez | pr=D, stav=4; projekt, `predano_organizace` and SN01 author all ORG-000066 (foreign) — D09 probe | file API |
| `C-202009779-N00022` | samostatny_nalez | pr=D, stav=1 — creator-org clause probe, both arms match: `predano_organizace` ORG-000091, projekt `C-202009779` org ORG-000091; created SN01 by U-001975 | landing, handle, OAI |
| `C-202211308-N00230` | samostatny_nalez | pr=D, stav=1 — creator-org clause probe, projekt arm only: `predano_organizace` ORG-000099 (mismatch), projekt `C-202211308` org ORG-000091 (match); created SN01 by U-004219 | landing, handle, OAI |
| `C-202009779-N00031` | samostatny_nalez | pr=C, stav=3, projekt `C-202009779` org ORG-000015, `predano_organizace` ORG-000084, SN01 author U-004219 (ORG-000091) — **D16 probe**: every published C-row arm dead; file `f3ba25a8-9c79-46cd-924d-803b56e46e92` | file API, landing, handle |
| `ADB-PRAH71-008099` | adb | production restricted ADB (produces the `-V0001…-V0006` VB children checked in the D07 sweep) | landing (children), search |
| `U-001003` | uzivatel | the B test account (ORG-000011) — own-record clause (2026-10-23) | OAI |
| `U-000005` | uzivatel | the C test account (ORG-000103) — counterpart for the creator-org clause probes (2026-10-23) | session/islogged, landing, OAI |
| `U-001389` | uzivatel | the D test account (ORG-000094) — control session (2026-10-23) | session/islogged, landing, OAI |
| `M-202500301` | projekt | production stav=6 projekt with a file — D15 production control | production file API |
| `M-202600319-N00016` | samostatny_nalez | production pr=D, stav=4 find with a file — D09 production control | production file API |
| let / heslo / ruian / organizace / osoba samples | open types | any state-matching record served to anonymous | OAI (re-derive via recipes) |

Test accounts (role B/C/D): `badatel.ai@arup.cas.cz` (U-001003, ORG-000011), `archeo.ai@arup.cas.cz` (U-000005, ORG-000103), `archivar.ai@arup.cas.cz` (U-001389, ORG-000094) as observed 2026-10-23 — passwords are **operator-provided per session and never recorded in this report**. Sessions expire ~30 min. The C account's organisation is ORG-000103; older registry anchors that name ORG-000091 are foreign to it and are no longer C own-organisation probes.

### Discovery recipes

Do not rely on registry anchors without re-verification; re-discover per session when needed. All steps work anonymously unless stated.

1. **Enumerate and classify records:** `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` (100 records/page, resumptionToken pagination). Classify each record by regex on the body: whole-record restriction `(?s)>\s*HTTP/1\.1 403 Forbidden\s*</amcr:amcr>`; element restriction `<amcr:chranene_udaje>HTTP/1\.1 403 Forbidden`; deleted header `status="deleted"`. Visible records expose `<amcr:stav>` and `<amcr:pristupnost … id="HES-…">` (id → A/B/C/D) for precise classification. `from`/`until` datestamp windows slice the stream.
2. **Predict 401 vs 403:** `GET /api/search/query?entity=<e>&q=ident_cely:"<ident>"` — `numFound: 0` while the record exists in OAI ⇒ not searchable ⇒ anonymous landing returns **401**; `numFound > 0` while OAI shows the record restricted ⇒ **403**.
3. **Visible baselines / element ladder:** from the same OAI scan, take records with the target `stav` and desired pristupnost level (A = open baseline; B/C/D = element hidden up to that role). Direct: `GET /api/search/query?entity=akce&q=pristupnost:<A-E>&rows=5`.
4. **404 candidates:** fabricate idents with valid prefixes but nonexistent numbers (e.g. bump an existing ident's numeric tail far out of range). Expect 404 for all roles.
5. **410 candidates:** scan a set for `status="deleted"` headers — `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` returns recent tombstones in the same page as live records (a fresh deletion carries its deletion datestamp), so a single page frequently yields a usable tombstone with no maintainer action; slice with `from`/`until` when the tombstone is not on the first page. Deleted **core-type** docs stay in the entities index with `is_deleted=true` and return **410** on landing, `/map/` and handle for every role; deleted `uzivatel`/`osoba` ids 404 (never served on landing by design). A maintainer-controlled deletion remains the fallback when no tombstone is present.
6. **File-API candidates:** file UUIDs come from the OAI `soubor` elements of **visible** records (`<amcr:url>`, `<amcr:path>`; some records have `soubor` without a Fedora path — skip those). The legacy `/api/search/id` endpoint returns the record's stored fields including the `soubor_filepath` values (and the `soub-XXXXXX` id) for a visible record — use it or the search response to read file elements. Coverage per rule row: dokument and SN files plentiful; projekt files findable via `entity=projekt&q=soubor_filepath:rest*` (the response projects `stav` and `projekt_organizace` — pick per arm: own/other organisation, stav 1–6; stav=1 projekts can be absent from the test data — ask the maintainer); restricted pr=D dokumenty with a chosen organisation via `entity=dokument&q=pristupnost:D AND stav:3 AND soubor_filepath:rest*` and the `<amcr:organizace>` element of the OAI record. For `/img` and `/pdf` probes, the soubor id drives the endpoint; a public dokument found via `entity=dokument&q=pristupnost:A AND stav:3 AND soubor_filepath:rest*` gives the `/pdf` positive control (`200`).
7. **Child-record candidates:** derive from parent metadata — OAI `archeologicky_zaznam` records list `dokumentacni_jednotka` idents; `adb` records list `vyskovy_bod` idents; cast/komponenta idents follow the fixed suffix patterns. `/api/search/query?entity=komponenta|vyskovy_bod` (with an ident filter) confirms existence (DJ and dokument_cast entity queries are stubs).
8. **Cross-environment comparison:** run the same probes against production; data drifts — never assume the same ident has the same state in both environments. Production probing needs explicit operator approval.
9. **Restricted-children search sweep:** `GET /api/search/query?entity=vyskovy_bod&q=-stav:3&rows=0` (add `AND -pristupnost:A` for elevated children) — a non-zero `numFound` counts children of restricted parents that remain anonymously searchable; the geometry verdict needs `rows>0` because the fields are projected per record, and the **positive control** `q=stav:3` proves the same field is served for a public child. Same shape for other child entities where `searchable` is not parent-derived; for komponenta/projekt add restricted variants and inspect the chranene blocks — for projekt, sweep `-pristupnost:A AND stav:6` separately (the anonymously visible population, see architecture facts). To test element masking, request the record and check the **unsuffixed block key**: on the search API the block is projected as a JSON **string** (`"field":"{…}"`), on a searcher that projects it as `[json]` (the legacy id action) it is an **object** — both forms matter when reading a raw response.
10. **Role sessions:** `curl -s -D - "https://digiarchiv-test.aiscr.cz/user/login?user=<email>&pwd=<pwd>" -o NUL` captures `Set-Cookie: JSESSIONID=…`; verify with `/user/islogged?wantsUser=true` replaying the cookie; replay on any surface for role views — the file path accepts the session cookie as well as Basic auth, the landing `checkId` path treats Basic auth as anonymous. OAI accepts Basic auth directly. On a Windows allowlisted shell, put the URL first and the flags after (`curl.exe "https://…/id/<IDENT>" -H "Cookie: JSESSIONID=<SID>" -o NUL -w "%{http_code}\n"`) — flag-first variants may not match the permitted shapes.
11. **Element masking comparison (backend):** for a restricted record, compare the search/handle/export/legacy-id response for anonymous vs B vs C — the suffixed-field mechanism must show the chranene block only from the matching role up; an unsuffixed block present for anonymous is a leak (D11/D12 shape).
12. **Deployed build identity:** fetch the instance's `/`, resolve the `main-<hash>.js` reference, download it and read the embedded git-describe object (`raw:"v…"`, `hash:"g…"`, `distance:…`, `tag:"v…"`) — a milestone build describes from the last tag, so compare the commit hash, not the literal. Recipe and the footer-literal caveat live in the [`version-footer`](version-footer.md) scenario.
13. **Restricted SN candidates (element-level probe):** non-archived SN are `searchable=false`, so `/api/search/query` cannot enumerate them (`ident_cely:"<ident>"` returns `numFound: 0` even for an entitled role). Find a `stav≠4` candidate via OAI `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=samostatny_nalez` with a D session (Basic auth), filtering on `<amcr:stav>` and `<amcr:pristupnost … id="HES-…">`; alternatively take an ident from a project's SN children or the SN's `<amcr:projekt>` and open `/id/<ident>` and `/api/search/handle?id=<ident>` directly. Archived SN (`stav=4`) are `searchable=true` and can be enumerated with `GET /api/search/query?entity=samostatny_nalez&q=pristupnost:D AND stav:4&rows=3`. Read `samostatny_nalez_chranene_udaje` from the response, and read the **body** on the handle path: the handle document for a non-searchable SN omits `samostatny_nalez_projekt` and `samostatny_nalez_predano_organizace`, so `isAllowed`'s `optString`-based fallback cannot resolve the C organisation arms (the pre-existing #244 state); the C own-project probe therefore shows the record loading without the block, while the B owner probe shows the block.

### Verification commands

```bash
# landing-page / map status code (swap the host for production: digiarchiv.aiscr.cz)
curl -s -o /dev/null -w "%{http_code}\n" https://digiarchiv-test.aiscr.cz/id/<IDENT>
curl -s -o /dev/null -w "%{http_code}\n" https://digiarchiv-test.aiscr.cz/map/<IDENT>
# OAI-PMH record (anonymous = restricted view; add -u user:pwd for role view; use the environment's own domain prefix)
curl "https://api-test.aiscr.cz/2.2/oai?verb=GetRecord&identifier=https%3A%2F%2Fapi-test.aiscr.cz%2Fid%2F<IDENT>&metadataPrefix=oai_amcr"
# searchability / search behaviour
curl "https://digiarchiv-test.aiscr.cz/api/search/query?entity=<ENTITY>&q=ident_cely%3A%22<IDENT>%22&rows=1"
# data served to the landing page (element-level / child-leak checks)
curl "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<IDENT>"
# legacy id lookup (searcher projection + element masking; numFound 0 for non-searchable)
curl "https://digiarchiv-test.aiscr.cz/api/search/id?id=<IDENT>"
# geometry actions (geometrie needs id + 4-value loc_rpt bbox + format; pians needs rows)
curl "https://digiarchiv-test.aiscr.cz/api/search/pians?entity=pian&q=pristupnost:C&rows=200"
curl "https://digiarchiv-test.aiscr.cz/api/search/geometrie?id=<PIAN_IDENT>&loc_rpt=<lat1,lng1,lat2,lng2>&format=GeoJSON"
# export (csv/xlsx/xml/json; mapa=true serves geometry)
curl "https://digiarchiv-test.aiscr.cz/exp?entity=<ENTITY>&format=json&mapa=true&geometrie=GeoJSON"
# image endpoints by soubor id
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/img/thumb?id=<SOUBOR_ID>"
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/img/full?id=<SOUBOR_ID>"
# PDF page images (gated via ImageAccess)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/pdf?id=<SOUBOR_ID>&page=1"
# admin/integration surfaces (expect 403/500 anonymously)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/fedora/request"
# role-view on any surface (see recipe 10 for login)
curl -s -o /dev/null -w "%{http_code}\n" -H "Cookie: JSESSIONID=<SID>" https://digiarchiv-test.aiscr.cz/id/<IDENT>
# File API (>= 1s between requests, max ~3 per burst; Basic auth or session cookie for role tests)
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb/page/1"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb-large"
# SN protected-element probe (anonymous and role sessions; see recipe 13)
curl "https://digiarchiv-test.aiscr.cz/api/search/query?entity=samostatny_nalez&q=pristupnost:D%20AND%20stav:4&rows=2"
curl "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<IDENT>"
curl "https://digiarchiv-test.aiscr.cz/api/search/id?id=<IDENT>"
# OAI element control for the same record (Basic auth for a role; use the environment's own prefix)
curl -u "<USER>:<PASSWORD>" "https://api-test.aiscr.cz/2.2/oai?verb=GetRecord&identifier=https%3A%2F%2Fapi-test.aiscr.cz%2Fid%2F<IDENT>&metadataPrefix=oai_amcr"
```

## Current verification (2026-10-23, second #1151 regression, test `digiarchiv-test.aiscr.cz` / `api-test.aiscr.cz`; bundle `main-X4LCF425.js`, embedded build `v4.1.0-11-g880f1c3e`)

**Scope and limitations.** The run was a regression on #1151 after `e73b5c17` and `7f97f99e`; it probed the test instance with fresh B/C/D sessions (B = U-001003/ORG-000011, C = U-000005/ORG-000103, D = U-001389/ORG-000094) and did not probe production. The acceptance scope for this run is #1151's record-level access: an entitled B/C user opens the record and the D21 error is gone. Element-level full access for an archaeologist's own organisation is the pre-existing state tracked in [#244](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/244), not a #1151 regression; OAI remains governed by its published element table. The non-searchable SN candidate is reachable only by landing/OAI/handle (recipe 13), not by `/api/search/query`; the searchable `stav=4` population is the D20 probe. Counts remain point-in-time.

Grid legend: `✓` observed and matches the stated expectation · `✗` + finding id = deviation · `—` = not examined in this run.

### Verified behaviour matrix

**SN `stav=4`, `pristupnost=D`, non-entitled to the test accounts (project and `predano` organisation foreign; SN01 author not B/C)**

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/api/search/query?entity=samostatny_nalez&q=pristupnost:D AND stav:4` | 200, no block ✓ | 200, no block ✓ | 200, no block ✓ | 200 + block ✓ (D sees all) |
| `/api/search/handle?id=<SN>` | 200, no block ✓ | 200, no block ✓ | 200, no block ✓ | 200 + block ✓ |
| `/api/search/id?id=<SN>` | 200, no block ✓ | 200, no block ✓ | 200, no block ✓ | 200 + block ✓ |
| OAI `GetRecord` `<SN>` | 200 body, block masked ✓ | 200 body, block masked ✓ | 200 body, block masked ✓ | 200 body, block present ✓ |
| `/api/search/query?entity=samostatny_nalez&q=pristupnost:A AND stav:4` | 200 + block ✓ (positive control) | — | — | — |

**SN `stav=1`, `pristupnost=D`, C own-project (project organisation = C's ORG-000103), SN01 author = the B account**

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/id/<SN>` landing | 401 ✓ (not searchable) | 200 ✓ (SN01 owner) | 200 ✓ (project organisation) | 200 ✓ |
| `/api/search/handle?id=<SN>` | 401 ✓ | 200, no error, block present ✓ (owner full access) | 200, no error, block absent ✓ (known #244 state) | 200 + block ✓ (control) |
| `/api/search/query?...ident=<SN>` | `numFound: 0` ✓ (not searchable; not a visibility verdict) | `numFound: 0` ✓ | `numFound: 0` ✓ | `numFound: 0` ✓ |
| OAI `GetRecord` `<SN>` | 200 body, whole record `HTTP/1.1 403 Forbidden` ✓ | 200 body, served, block masked ✓ | 200 body, served, block masked ✓ | 200 body, served, block present ✓ |

Notes: D21 is closed because B and C receive the record body with no `historie` error; the block absence for C is the pre-existing #244 state, not a #1151 regression. The C organisation arms fail because the handle document omits `samostatny_nalez_projekt` and `samostatny_nalez_predano_organizace`; the B owner arm works because the SN01 author is present. The `predano_organizace` arm was not isolated with a live candidate (no checked SN with `predano = ORG-000103`); the same missing-field path applies. The protected values (cadastre, localization, geometry) were not copied into this report; candidates are re-derived by recipe 13.

### Known defects

- **permissions-D20 — fixed.** The prior verification recorded that anonymous, B and C clients received `samostatny_nalez_chranene_udaje` for every `stav=4` SN regardless of `pristupnost`, because `isAllowed` returned `true` on `stav == 4` before comparing `pristupnost`. `e73b5c17` removed that early return. This run verified the fix on a non-entitled `pristupnost=D`, `stav=4` SN: anonymous, B and C receive no block on `/api/search/query`, `/api/search/handle` and `/api/search/id`; D receives it; the `pristupnost=A` positive control still serves its block; OAI masks the block for anonymous/B/C and serves it to D.
- **permissions-D21 — fixed.** The prior verification recorded that `/api/search/handle` returned HTTP 200 with `{"error":"… historie … not found"}` for a restricted SN outside `stav=4`, because `isAllowed` read `historie`, which the search projection omitted. `7f97f99e` added `historie:[json]` to `getSearchFields`. This run verified the fix on a `pristupnost=D`, `stav=1` SN: the B owner and the C own-project session both receive the landing page and the handle body with no error; the B owner receives the protected block.
- **No open #1151 defect.** D20 and D21 are fixed. The C own-organisation protected-element gap is pre-existing and tracked in [#244](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/244) (`Pro archeologa umožnit zobrazení chráněných údajů u záznamů vlastní organizace`); see the separated pre-existing observations below. It is not attributed to #1151 and does not block this acceptance.

### Corrections made during this run

- The prior verification recorded D20 and D21 as open; both are fixed by the deployed backend (`e73b5c17` removes the `stav=4` early grant, `7f97f99e` adds `historie` to the projection). The prior test-build row named `v4.1.0-7-geafb47ea`; this run's bundle embeds `v4.1.0-11-g880f1c3e`, and the backend behaviour confirms both fixes.
- The prior framing "OAI masks, the searcher diverges" is refined: OAI masks by its published element table; the searcher grants the block to entitled users. The C own-organisation arm is the pre-existing #244 scope, not a defect of #1151.
- `historie:[json]` was added to the searcher projection; `historie` was already served anonymously by OAI, so this is not a new disclosure class. The prior "latent code observation" about the omitted `historie` field is superseded.

### Maintainer-assisted and unverified

- Production was not probed; the production state of D20 and D21 (and of the #244 gap) is not inferred from the test instance.
- The `predano_organizace` arm was not isolated with a live candidate; the project-organisation arm is verified, and the same missing-field path affects `predano`.
- D01–D19 were not re-examined.

## Findings registry

| ID | Severity | Surface | Status | One-line summary |
| --- | --- | --- | --- | --- |
| D01 | Low | bare `/map` | **fixed** | `/map` forwards to `/map2` (200) |
| D02 | — | SN record rules | **changed** | fix-wave rewrite superseded by D06/D09 |
| D03 | High | legacy `/api/search/id` | **changed — route live and filtered** | the route is reachable on both deployments and returns searcher-filtered, element-masked results (non-searchable → `numFound: 0`); the earlier "action removed" state does not hold, and the original non-searchable-record exposure is not reproducible |
| D04 | Low | file API rate limiter | **closed** (operator decision: reactive handling) | both limiter branches live-verified (interval `Retry-After: 0`; concurrent `Retry-After: 500` — reads as milliseconds in a seconds-defined header) |
| D05 | Low | child-record landing | **fixed** | DJ/dokument_cast/VB inherit parent rules again; search residual was D07 |
| D06 | High | SN record level | **fixed** | the A/B branches' `pristupnost <= userPr` conditions contradicted #237's `stav=4` rule — the public stav=4 SN serves anon/B masked |
| D07 | High | search API (vyskovy_bod) | **fixed** | restricted VB geometry withheld from anonymous clients on both deployments (production verified 2026-09-29) |
| D08 | High | OAI-PMH (SN) | **fixed** | the OAI-side SN owner clause resolves — the B-owner receives the own SN with chranene masked per pristupnost |
| D09 | Medium | file API | **fixed** | file-gate SN rows over-blocked: the A/B/C rows carry `stav=4` without pristupnost conditions, matching the published table |
| D10 | High | file API `/thumb-large` | **fixed** | undocumented endpoint bypassed all permission checks; the variant matrix now follows the original's rules |
| D11 | High | backend surfaces (komponenta) | **fixed** | komponenta docs served the SN chranene block unmasked; now masked below the matching role |
| D12 | High | search + handle API (projekt) | **fixed** | anonymous search/handle served `projekt_chranene_udaje` for restricted stav=6 projekts; the post-filter drops the block whenever pristupnost exceeds the user's role |
| D13 | High | `/fedora/*` | **closed — solved in current state** (operator accepted the network-trusting posture and the mutating actions as-is) | unauthenticated raw-Fedora access from the local network/VPN; read actions removed on both deployments |
| D14 | High | `/pdf` | **fixed** | `/pdf` page images are gated via `ImageAccess` on both deployments (production verified 2026-09-29) |
| D15 | High | file API (projekt) | **fixed** | projekt file gate applied the record-level rule instead of the published `//projekt/soubor` row |
| D16 | Medium | file API (SN) | **fixed** | the SN file-gate C-row owner arm compares the SN01 author's identity (`userId.equals(uzivatelSN01)`), matching the published table and the code's rule comment; the organisation-of-author grant is gone |
| D17 | High | landing pages + handle API (SN) | **fixed** | the SN organisation clauses resolve on the entities path (checkId projects projekt/predano organisations) |
| D18 | High | file API (dokument) | **fixed** | the dokument file-gate B and C rows carry the pristupnost conditions again; the D01 owner and creator-organisation arms preserved |
| D19 | Medium | export (`/exp`) | **fixed** | `entity=projekt` export serves records; restricted projekts export without the chranene block (production verified 2026-09-29) |
| D20 | Critical | search/handle/id (SN) | **fixed** | `isAllowed` returned `true` on `stav == 4` before comparing `pristupnost`; `e73b5c17` removed the early grant. Verified: anon/B/C receive no `samostatny_nalez_chranene_udaje` for a non-entitled `pristupnost=D`, `stav=4` SN; D and the `pristupnost=A` positive control receive it; OAI masks it |
| D21 | High | search/handle (SN, `stav≠4`) | **fixed** | `isAllowed` read `historie`, which the search projection omitted; `7f97f99e` added `historie:[json]`. Verified: B owner and C own-project handle bodies return without the `historie` error |
| D22 | — (pre-existing) | search/handle (SN, `stav≠4`, C organisation arms) | pre-existing — tracked in [#244](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/244) | the handle document omits `samostatny_nalez_projekt` and `samostatny_nalez_predano_organizace`; `optString` returns `""` so the `projektId == null` fallback never runs and the C organisation/`predano` arms do not grant the protected block; this predates #1151 and is not a regression |

D20 and D21 are fixed on the test instance. D22 is the pre-existing #244 state, not a #1151 regression. The earlier production role-gated checks remain maintainer-assisted and were not re-probed in this run; D01–D19 were not re-examined on 2026-10-23.

## Pre-existing observations

- **permissions-D22 — pre-existing, tracked in [#244](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/244), non-verdict-bearing:** the C organisation and `predano` arms of `SamostatnyNalezSearcher.isAllowed` do not grant `samostatny_nalez_chranene_udaje` for a restricted, non-searchable SN on the handle path. Evidence: a `pristupnost=D`, `stav=1` SN from the C account's project organisation loads for C (landing and handle 200) but without the block; the B owner and D receive it. The handle document omits `samostatny_nalez_projekt` and `samostatny_nalez_predano_organizace`; `optString` returns `""` so the `if (projektId == null)` fallback never runs. #244's title and body describe exactly this element-level own-organisation access; the issue predates #1151 and is not attributed to it.
- The `dokumentacni_jednotka` and `dokument_cast` entity-search endpoints are stubs and return error bodies — pre-existing, unrelated to permissions, no data exposure.
- Quoted-phrase Solr interpolation (code-verified, low): `HandleServlet.getDocument`'s `soubor_filepath:"<url-path>"` filter and OAIRequest's `ident_cely:"<id>"` interpolate the request path/identifier without escaping query metacharacters (`InitServlet.asSafePath` blocks traversal and backslashes only). No privilege bypass found — the permission gate still runs on whatever document the query returns — but the construction is injection-shaped; escaped terms would remove the class.
- Test-data quality: several records carry placeholder file paths (`"neni"`) and omit `soubor/path`+`soubor/url` elements in OAI output; well-formed records (verified via the D view) do serve both. Probes use well-formed records — do not read the omission as build behaviour.
- Environment-specific file bindings: `M-TX-202100125` binds test file `3a0f7078-…` but production file `fe377f16-…`; always re-read the soubor path from the environment being probed.
- Production index: the production entities core reports komponenta docs in its count surface, but `entity=komponenta` queries error there — komponenta fix verification stays test-side and is not inferred from the count.
- `/img/full` on a dokument PDF (`soub-826403`, public; production 2026-09-29) returns `500` — the same backend-path class already recorded for an SN file, not a gate outcome (the gate passes a public file). Inconclusive for permissions; not a finding.
- The subject repository's `review_config.toml` carries no `deployed_verify` allowlist, so the production probe targets are declared by this corpus and the maintainer's explicit direction rather than by repository configuration. Recorded as a configuration observation, not a defect.

## Verification log

| Date | Pass | Scope / outcome |
| --- | --- | --- |
| 2026-08-28 | initial | point-in-time report consolidated into this scenario when the corpus moved to this repository |
| 2026-09-10 | 1–8 | regression passes 1–8: D01/D03/D05 fixed; D06 introduced then narrowed; D08 narrowed; D09 open; record-type matrix; children + backend matrix; source audit and route/gate enumeration; full grid pass finding D13 (raw-Fedora access) and D14 (`/pdf` ungated); production probes classifying D13 to local-network scope, D12 active there, D14 placeholder-only; #693 pattern moved to the file-distributions scenario |
| 2026-09-11 | 9 | #370 regression re-verification: D06 narrowed, D08 narrowed, D09 re-verified, D10 fixed, D11 fixed, D12 fixed both environments, D13 posture unchanged, D14 fixed on test; #370 items 1/3/4 verified; anchor registry refreshed |
| 2026-09-15 | 10 | D06 fixed on test; D08 narrowed further; uzivatel own-record clause verified; D03 fixed (action removed); D13 fixed by hotfix; D14 escalated to a live production leak; operator decisions: D07/D14 deferred to the production release, D04 closed |
| 2026-09-17 | 11 | D08 narrowed to the owner clause; D09 confirmed defect (SN A/B pristupnost conditions); D13 closed; holdouts re-verified; #370 items 1–4 re-verified; registry extended |
| 2026-09-23 | 12 | regression pass: D15 found (projekt file gate), D12 reopened (stav=6 population), D09 extended to C, D16 narrowed, D17 found (SN org clauses on entities path), concurrent 429 live-verified; production compared anonymously |
| 2026-09-23 | 13 — full pass | all permissions re-verified against the published OAI-PMH and File API tables on build `v4.0.3-230-g1669e29c-dirty`, all surfaces, roles A/B/C/D with fresh sessions; **D12, D15, D17, D09 fixed** by the new build; **D18 found** (dokument B/C rows lost pristupnost — unintended collateral of the D09 fix), **D19 found** (projekt export broken), **D16 confirmed** with the maintainer-provided `C-202009779-N00031`; D08 still open; production compared anonymously (file gates follow the published rows; D07/D14 deferred to the production follow-up); document cleaned to current state per operator instruction. **Operator decisions:** D18 is unintended collateral of the D09 fix — the fix needs a separate decision branch for SN files; D16's author-organisation arm is stale and to be removed (documentation authoritative), scoped so the other SN arms keep working; D07/D14 listed as a production later-check, not findings |
| 2026-09-24 | 14 — regression round | build `v4.0.3-235-g544d7851-dirty`: **D08, D18, D19 fixed** (B-owner receives the own SN on OAI with chranene masked; dokument B/C pristupnost conditions restored with the D01 owner/creator-organisation arms and the pr=A/stav=3 positive preserved — paradata follows the fixed gate identically; projekt export serves records, restricted projekts without the chranene block); **D16 still open — root cause established:** the SN C row implements the SN01 author's *organisation* where the code's own rule comment and the published table state the author's *identity* (probe re-validated: all published arms dead, SN01 author U-004219 of ORG-000091, file binding and served file confirmed); D09/D12/D15/D17 hold (spot cells re-probed); production D07/D14 re-probed anonymously — still open (fix wave not released there) |
| 2026-09-24 | 15 — regression round | build `v4.0.3-236-g41592838`: **D16 fixed** — C 403 on `C-202009779-N00031` (arms re-verified unchanged: pr=C, stav=3, projekt ORG-000015, predano ORG-000084), D 200 control; the SN C-row owner arm now compares the SN01 author's identity (`userId.equals(uzivatelSN01)`); **zero collateral** — the full verification table conforms: C 200 via the projekt-organisation arm (`C-202600009-N00014`), B 200 SN01 owner, anon/C 200 on the stav=4 find, B/C 403 on the pr>D other-organisation dokument file (D18 holds), C 200 via the dokument D01 creator-organisation arm; no open findings on the test build; production D07/D14 pending the fix-wave release |
| 2026-09-29 | 16 — production release regression | production build `v4.0.3-237-g96deec70-dirty` (dev HEAD `96deec70`, merged by release PR #1109; tag `v4.1.0` = `d1f084bf`; permission tree diff over the test build touches only `CITATION.cff` and two Angular footer files), anonymous probes only: **D07, D14, D19 confirmed fixed on production**; D12, D10, D01, D05, D06, D13 hold; the #370 checklist codes 404/410/401/403 verified anonymously (fresh tombstone `X-C-000025454`); **D03 changed** — the legacy `/api/search/id` route is live and applies searcher filtering plus element masking, so the earlier "action removed" state does not hold and the non-searchable exposure is not reproducible; the release code equals the fully fixed test build, so role-gated arms (D08/D09/D15/D16/D17/D18, #370 items 1 and 5) remain maintainer-assisted on production for want of credentials and are not inferred from anonymous results; no new finding. The 2026-09-24 test verification is recoverable from this file's history |
| 2026-10-23 | 17 — #1151 regression | test bundle `v4.1.0-7-geafb47ea`, backend at/after `4fc0ecbd`: **D20 found** — anonymous/B/C receive `samostatny_nalez_chranene_udaje` for every `stav=4` SN on search/handle/id; **D21 found** — `/api/search/handle` returns an HTTP 200 error body for a restricted `stav=1` own-project SN (C) and own-record SN (B) because `isAllowed` reads a `historie` field the projection omits; OAI masks the block (control); the corrected candidate recipe uses OAI/landing because non-archived SN are not searchable; prior defects D01–D19 not examined; test-account identities corrected |
| 2026-10-23 | 18 — #1151 fix regression | test bundle `main-X4LCF425.js`, embedded build `v4.1.0-11-g880f1c3e`: **D20 fixed** — `e73b5c17` removed the `stav == 4` early grant; anonymous/B/C receive no block on query/handle/id for a non-entitled `pristupnost=D`, `stav=4` SN; D and the `pristupnost=A` positive control receive it; **D21 fixed** — `7f97f99e` added `historie:[json]`; B owner and C own-project landing/handle return 200 with no `historie` error; **D22 confirmed as pre-existing #244 state, not a #1151 regression** — the C organisation/`predano` arms do not grant the block for a non-searchable restricted SN because the handle document omits the project/`predano` fields and the `optString` fallback never runs; OAI masks per its table; prior D01–D19 not examined |
