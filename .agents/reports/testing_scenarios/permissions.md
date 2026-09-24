# Permission model — testing scenario

**Key:** permissions (feature)

**Scope:** the permission behaviour of every backend surface that serves record data — landing pages, OAI-PMH, search/handle/legacy APIs, geometry and export endpoints, the File API, the image and PDF endpoints, and the admin/integration servlets — at record level and element level, for all roles. Issues [ARUP-CAS/aiscr-digiarchiv-2#370](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/370) and [#237](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/237) (rules definition) drove it.

**How to use this record:** the *Durable knowledge* half states the rules and the complete surface inventory; the *Current verification* half is a set of **coverage grids** (record type × role per surface). A grid cell records the observed result for that combination: `✓` = matches the documented rule, `✗` + finding id = deviation, `·` = no surface for that type by design, `—` = **untested on this build — the worklist for the next pass**. Fill or annotate cells only with fresh evidence; never mark a cell from assumption.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | `https://api-test.aiscr.cz/2.2/oai` | carries #370 plus the fix waves through `41592838` (legacy `/api/search/id` removed; `/pdf` gated via `ImageAccess`; SN record rules per #237; SN organisation clauses on the entities path; projekt file gate per the published row; SN file gate without pristupnost conditions; OAI SN owner clause; dokument file-gate pristupnost conditions; projekt export entity; SN C-row owner arm on the SN01 author's identity). No open divergence from the published rules on the test build. Build identity: the served bundle embeds `v4.0.3-236-g41592838` while the footer hash alone does not identify the backend deploy — establish the backend by behaviour |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | `https://api.aiscr.cz/2.2/oai` | pre-fix-wave build: D07 (restricted-children VB geometry) and D14 (`/pdf` page images) remain open there; both are resolved by the upcoming production release of the fix wave. The partial hotfix removed the FedoraServlet read actions (`GET_ID`/`REQUEST`), closing the D13 raw-read path. The SN and projekt file gates follow the published rule table (D09, D15 absent), with no D12 stav=6 exposure; D17 and the test-only file-gate divergences (D16, D18) are not examinable anonymously. `/thumb-large` absent (404) |

OAI quirks: on the test instance, OAI identifiers use the **`https://api-test.aiscr.cz/id/…`** prefix (GetRecord with the production prefix silently fails with `idDoesNotExist`); set names containing a colon must be percent-encoded (`set=dokument%3A3d`). Versioned OAI endpoints (`/2.1/oai`, `/2.2/oai`) serve the same filtered core and differ only by XSLT. The **version-less** endpoint `https://api-test.aiscr.cz/oai` 302-redirects to the current version — a convenient Basic-auth role-probe shape.

### Surface inventory (routes and gates)

Complete enumeration from `web.xml` + `@WebServlet` annotations (2026-09-10 source audit, amended on later passes). `ApiServlet` (`/api`, also `/api/*` in web.xml) is a pure path-normalizing forwarder, so every action servlet is also directly reachable under its own path (e.g. `/search/query` ≡ `/api/search/query`).

| Route | Servlet / action | Gate mechanism | Permission-relevant behaviour |
| --- | --- | --- | --- |
| `GET /id/<ident>`, `GET /map/<ident>` | `HandleServlet` `checkId` | 404 (no doc in `entities`) → 410 (`is_deleted`) → 401 (`!searchable` + anonymous) → 403 (per-model `filterOAI`) → 200 | landing / map gating; the query projects `pristupnost`, `stav`, and for SN also `projekt`/`projekt_organizace`/`predano_organizace`, so the SN organisation clauses resolve on this path; rendered page is a shell, data comes from the handle API |
| `GET /id/<ident>/file/<uuid>` (+ `/thumb`, `/thumb/page/N`, `/thumb-large`, `/paradata`, `/paradata/<distribution>`, distribution suffixes per #693) | `HandleServlet` file path | `HandleServlet.isFileAllowed` per XPath rule row; #693 rule: distributions and paradata follow the original's rules; Basic auth and session cookie both accepted here (not on `checkId` — Basic-auth landing requests are treated as anonymous); rate limiter per IP (≥ 1 s between requests, also across distinct files) | File API; `/thumb` documented ungated; `/thumb-large`, `/thumb/page/N`, `/paradata` follow the original's rules; #693 distributions and paradata follow the original's rules (see the [`file-distributions`](file-distributions.md) scenario) |
| `GET /oai/*` (verbs GetRecord/ListRecords/ListIdentifiers; oai_amcr, oai_dc) | `OAIServlet` → `OAIRequest.filter` | per-model `filterOAI` (record level) + element string surgery (chranene/oznamovatel) | OAI-PMH; Basic auth logs in; oai_dc derived after filtering |
| `GET /api/search/query` | `SearchServlet` QUERY → per-entity `EntitySearcher` | pristupnost-suffixed projected fields + `searcher.filter(jo, pristupnost, org)` post-filter | search visibility + element masking; E downgraded to D |
| `GET /api/search/handle?id=` | `SearchServlet` HANDLE | per-model `filterOAI` on the entities doc | record JSON for landing pages |
| `GET /api/search/id` | `SearchServlet` ID | action **removed** (500 no-enum error, no data served) | legacy id lookup — gone; do not use for probing |
| `GET /api/search/id_as_child` | `SearchServlet` ID_AS_CHILD | child-doc fetch | child record JSON (verify per parent state) |
| `GET /api/search/gml`, `/wkt`, `/geometrie` | `SearchServlet` GML/WKT/GEOMETRIE | GEOMETRIE takes `id` (pian ident) plus `loc_rpt` as a **4-value bbox** (`lat1,lng1,lat2,lng2`) and a required `format` (`GML`/`GeoJSON`/other=WKT); it queries the role-suffixed `loc_rpt_<pr>` field (masks restricted pians for anonymous); its pristupnost check is commented out in source (latent if the suffixing is ever dropped); reads `pian_chranene_udaje` blocks | geometry extracts |
| `GET /api/search/pians` | `SearchServlet` PIANS → `PIANSearcher.getMapPians` | requires a `rows` parameter (missing rows → NumberFormatException error body); projects `pian:[json]`, `pian_id`, `ident_cely`, `organizace`, `pristupnost` and role-suffixed `loc_rpt`/`loc` | map pian points — idents without coordinates below the role |
| `GET /api/search/mapa`, `/export`, `/export_mapa` | `SearchServlet` MAPA/EXPORT/EXPORT_MAPA | per-searcher filter (same as query) | map search + export variants; `entity=projekt` export is broken on the current test build (D19) |
| `GET /api/search/stats`, `/stats_index`, `/export_stats_index`, `/obdobi`, `/thesauri`, `/log`, `/check_relations`, `/home` | `SearchServlet` auxiliary actions | aggregate/heslar/analytics | statistics and vocabularies — low sensitivity, spot-check |
| `GET /exp` | `ExportServlet` → `searcher.export(request)` | **no servlet-level gate**; protection only inside `EntitySearcher.export` (per-searcher `filter()`); map path reads `*_chranene_udaje` geometry | csv/xlsx/xml/json export; `mapa=true` serves geometry (GeoJSON/GML); the **projekt** entity errors for all records on the current test build (D19) — other entities export |
| `GET /pdf?id=<soubor_id>&page=N` | `PdfServlet` | `ImageAccess.isAllowed(request, true)` — the gate resolves the dokument by the **soubor id** (`getDokBySoubor`); a record-ident `id` resolves to nothing and returns **401 as a not-found**; SN always allowed (issue #85), dokument by `pristupnost` | PDF page images from the `thumbsDir` cache; production build has **no gate** (permissions-D14, open there until the upcoming production release) |
| `GET /img/thumb?id=<soubor_id>`, `/img/full?id=` | `ImageServlet` actions (only `thumb` and `full` exist — `thumb-large` is not a valid action and 500s) | `thumb` ungated (`ImageAccess.isAllowed(…, false)` = true); `full` gated by `ImageAccess.isAllowed(…, true)` + rate limiter | image variants by soubor id; `ImageAccess` full gate: SN → **always allowed** (issue #85), dokument → imgPr=A or userPr ≥ imgPr or same-org (same-org restricted to users ≤ C) |
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
- **The suffix mechanism has two halves and both must hold:** the query must project role-suffixed variants (`loc_rpt_<pr>`, `lat_<pr>`, suffixed secured JSON fields) **and** the post-filter must remove the unsuffixed block when `doc.pristupnost > userPr`. A searcher whose projected fields omit `pristupnost`, or whose `filter()` throws on a missing field, silently serves the unsuffixed block (permissions-D12 shape on projekt; D11 on komponenta — both fixed).
- **Restricted `stav=6` projekts are a distinct masking population.** A stav=6 projekt is anonymously visible at record level, so its search and handle documents reach anonymous clients and rely entirely on the post-filter to drop `projekt_chranene_udaje`. A D12 sweep must include `stav:6` explicitly (recipe 9) — the non-stav-6 restricted population alone does not cover it.
- **`/map/<ident>`** applies the identical gating before forwarding to `/map2`. Bare `/map` forwards to `/map2` (permissions-D01 fixed).
- **Rate limiting:** the File API and `/img/full` limiter throttles **per IP**, including bursts across distinct files — keep **≥ 1 s between requests** and at most ~3 per burst; a violation returns **429** (`Retry-After` observed `0` on the interval branch and `500` on the concurrent branch — the latter reads as milliseconds in a header defined in seconds). The limiter keys on client IP, so proxy buffering or egress-IP rotation can defeat observation from outside.
- **Page cache:** on test (`isTestEnv=true`) the landing-page cache is skipped entirely; on production a 1-day anonymous cache exists but only affects the rendered shell (record data is always fetched live per user), so it has no permission impact.
- **Child-entity indexing:** `dokumentacni_jednotka` docs stay `searchable=true` and are gated in `filterOAI` by the parent-propagated `stav` (anonymous gets 403, not 401, for a non-archived parent); `dokument_cast` inherits the parent dokument's `searchable` (401 shape); `vyskovy_bod` inherits `stav`/`pristupnost`/`searchable` from the ADB at index time. VB geometry fields (`vyskovy_bod_geom_wkt`, `vyskovy_bod_geom_gml`) remain indexed **without pristupnost suffixes**: the test build no longer serves them anonymously (D07 fixed there), production still does (D07 open there).
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

The deployed `SamostatnyNalez.filterOAI` follows the table on all rows; the C-branch organisation clauses (project organisation and receiving organisation, `predano_organizace`) resolve on OAI and on the entities path. The B-owner clause is dead on OAI (permissions-D08): the SN owner arm reads fields the oai document does not resolve; the landing/handle path serves the owner. Source: the `filterOAI` C branch is SN01 owner OR project organisation OR `predano_organizace`; the SN01 creator's organisation (`organizaceUzivatele`) is not used, so OAI has no creator-organisation arm.

**`searchable` flag (drives anonymous 401 vs 403):** AZ = stav 3; dokument = stav 3; samostatny_nalez = stav 4; pian = ident does not start with `N`; projekt = has related dokument/samostatny_nalez/akce; adb, let, ext_zdroj = always searchable; uzivatel = field not set (moot, 404 anyway); **child entities** — komponenta and dokument_cast inherit the parent's state; dokumentacni_jednotka and vyskovy_bod are `searchable=true` at index time and rely on `filterOAI` for gating (DJ reads the parent-propagated `stav`; VB inherits the ADB's full state triple).

**Element-level (inside a 200 response):**

- `chranene_udaje` (katastr, lokalizace, geometrie) hidden when record pristupnost > user pristupnost. Applies to projekt, archeologicky_zaznam, adb, pian, samostatny_nalez; **dokument and ext_zdroj have no element-level restriction** by spec.
- `projekt/oznamovatel`: A-B never; C only stav=1 or own organisation; D-E always. The handle API does not serve the element at all (see architecture facts).
- In OAI-PMH responses, restricted elements are replaced by the literal `HTTP/1.1 403 Forbidden`; restricted whole records get `<amcr:amcr…>HTTP/1.1 403 Forbidden</amcr:amcr>`; deleted records carry `status="deleted"` headers.
- An entity's deviation from this mechanism is recorded in that entity's scenario (for komponenta, see the [`komponenta`](komponenta.md) scenario).

**File API** (`https://…/id/<ident>/file/<uuid>[/thumb][/thumb/page/N]`, Basic auth or session cookie; see <https://arup-cas.github.io/aiscr-api-home/file-api/>):

| XPath | A | B | C |
| --- | --- | --- | --- |
| `//projekt/soubor` | never | never | stav=1 OR (stav 2-6 AND own org) |
| `//dokument/soubor` | pr=A AND stav=3 | (pr<=B AND stav=3) OR own (D01) | (pr<=C AND stav=3) OR own org (D01 creator's org) |
| `//samostatny_nalez/soubor` | stav=4 | stav=4 OR own (SN01) | stav=4 OR own (SN01) OR own org (predano/projekt) |

Deployed state (source `HandleServlet.isFileAllowed`, current test build):

- **projekt** — implements the published row exactly (permissions-D15 fixed).
- **dokument** (and `knihovna_3d`) — implements the published rows: A `pr=A AND stav=3`; B `(pr<=B AND stav=3) OR D01 owner`; C `(pr<=C AND stav=3) OR D01 creator's organisation`. The owner and creator-organisation arms work for `stav≠3`.
- **samostatny_nalez** — implements the published rows: A/B/C `stav=4` without pristupnost conditions; B and C owner arms on the **SN01 author's identity** (`userId.equals(uzivatelSN01)`); C organisation arms on `samostatny_nalez_predano_organizace` and the projekt organisation. The dokument C row's D01 creator-organisation arm is published behaviour and distinct from the SN owner arms.

Small thumbnails (`/thumb`) are **always public**; large thumbnails (`/thumb/page/N`, `/thumb-large`) and `/paradata` follow the original-file rules. **File distributions and paradata are additional File API surfaces (#693):** `/id/<ident>/file/<uuid>/{dist}` (any non-reserved suffix resolves as a distribution, `orig` included) and `/id/<ident>/file/<uuid>/paradata[/{dist}]` (the bare form serves the paradata of `orig`, always `text/plain`). They resolve through the same `getDocumentFromFile` lookup and the **same `isFileAllowed` gate** as the original file, so the rule table above governs them identically: a gate change on the original (the D09 fix, the D18 collateral) propagates to distributions and paradata **by design** — it is #693's contract working, not a separate divergence. An unknown distribution returns 404; the rate limiter covers all forms; the `soubor_distri` search facet exposes the live distribution paths. The full surface contract — including the internal reader `/api/img/*` surface, which is deliberately undocumented and must not be "fixed" to match — and the limiter facts live in the [`file-distributions`](file-distributions.md) scenario. **Image endpoints** (`/img`) apply their own `ImageAccess` rules (see surface inventory) — SN always allowed at full size, dokument by pristupnost. **`/pdf`** shares those rules on the test build (gated); the production build still serves it ungated (D14 — open there until the upcoming production release).

**Child records:** child entities live in the entities core with their own idents derived from the parent: `dokumentacni_jednotka` = `<AZ>-D01…`; `komponenta` (under AZ or SN) = `<parent>-K001…`; `dokument_cast` = `<dokument>-D001…`; `vyskovy_bod` = `<ADB>-V0001…`.

### Probe-record registry

Anchors verified on the current test build. **Drift rule:** records change state; re-verify each record's current state (OAI or search) before using it as a probe, and re-derive replacements via the recipes when it has drifted. File UUIDs come from the record's `soubor` elements and are **environment-specific** (the same record can bind different file UUIDs on test and production — re-read the soubor path per environment).

| Ident | Type | Properties that make it a probe | Surfaces |
| --- | --- | --- | --- |
| `C-202009490A` | akce | pristupnost C, archived (stav=3), restricted element case; carries DJ `-D01` and komponenta `-K001` | landing, OAI, search, handle, file (children), element ladder |
| `M-200500013A` | archeologicky_zaznam | pristupnost B — element ladder one step up | landing, element ladder |
| `C-202204147A` | akce | unarchived → anonymous 401 case | landing |
| `C-TX-193001369` | dokument | unarchived → anonymous 401; carries dokument_cast `-D001` | landing, children |
| `M-TX-202100125` | dokument | pr=C, stav=3 — restricted file sample; test file `3a0f7078-e26f-404d-aaa9-2b5abbb5e3d2`, production file `fe377f16-1da6-4912-b8d4-6f3095324879` (soub-340129 — the soubor id is the same in both environments) | file API, `/pdf`, `/thumb-large` |
| `C-TX-192700656` | dokument | pr=A, stav=3 — file-gate A-row positive; file `2c5db7d7-95fd-46a2-9859-819cadf72fb2` | file API |
| `C-TX-202400188` | dokument | pr=D, stav=3, organizace ORG-000091 — D18 B-row probe and C organisation-arm sample; file `e74e2652-cd97-4dda-9918-80a78d139e38` | file API |
| `M-TX-202100123` | dokument | pr=D, stav=3, organizace ORG-000077 (foreign) — D18 B/C-row probe; file `08c40264-40c0-4582-892d-66c00aff747c` | file API |
| `X-C-TX-000001130` | dokument | pr=D, stav=1, created by the B test account (D01 owner; creator org ORG-000091) — B my-record arm and C organisation arm; file `1a33a96a-3264-4949-aaba-25c5b4a2937b` | file API |
| `ADB-PRAH71-000861` | adb | restricted (stav≠3), pr=C; carries VB `-V0001` | landing, OAI, search, handle, children |
| `P-0134-000003` | pian | stav=2, state-matching — public baseline | landing, OAI |
| `P-2213-100119` | pian | pr=C — restricted chranene geometry probe (coordinates `49.4673196,13.4238193`) | search, geometrie, `/exp` mapa |
| `C-LET-00001` | let | open type sample | landing, OAI |
| `C-202402033` | projekt | **deleted** — tombstone (410) case: landing and handle 410 for all roles, OAI `status="deleted"` | search, handle |
| `C-201122587` | projekt | stav=6 with oznamovatel (PII masking case) | OAI oznamovatel |
| `M-201300453` | projekt | stav=6, pr=A — public stav=6 landing sample | landing, search |
| `C-202600010` | projekt | stav=3, organizace ORG-000210 (other than the test accounts'); file `342b7b35-fdc2-4fc7-b2df-bb3c0eecc524` | file API (D15 C negative) |
| `C-202101848` | projekt | stav=6, organizace ORG-000030; PDF file `06fbe1a2-62cc-4d5b-9674-abbd8c9754a4` | file API (D15 A/B/C negative) |
| `C-202500044` | projekt | stav=4, organizace ORG-000091 (own); file `0c06098a-a0a1-42fb-ab92-7bc9dd61dacc` | file API (C own-org positive) |
| `C-202007460` | projekt | stav=6, organizace ORG-000091 (own); file `ed5eaf7b-4087-4a89-936b-34c568fc1886` | file API (C own-org positive; A/B negative) |
| `C-202111855` | projekt | stav=1; file `9cc29a95-c95e-4f92-a4e1-db3d74489c4a` | file API (C stav=1 arm) |
| `M-200500013` | projekt | pr=B, stav=6 — D12 stav=6 masking sample | search, handle |
| `C-202600009-N00014` | samostatny_nalez | pr=C, stav=1, created by the B test account (owner clause; projekt organisation ORG-000091); file `c74d3136-3cab-468a-8405-3f79b89ab555`; carries komponenta `-K001` | landing, OAI, file, children, backend |
| `C-202600010-N00085` | samostatny_nalez | pr=C, stav=4 (public-archived SN); files `cafc9a90-3b8b-40f8-ace6-5791b0b6ffd2` (soub-653685), `e5115f1a-72ce-4eaa-a2d9-f52544aaa370` (soub-653686); carries komponenta `-K001` | landing, OAI, handle, file, backend |
| `C-202500044-N00001` | samostatny_nalez | pr=A, stav=4 — file-gate A-row positive; file `04d822bb-2706-430e-9ef2-720f8dcb5b8a` | file API |
| `M-202301371-N00006` | samostatny_nalez | pr=D, stav=4; projekt, `predano_organizace` and SN01 author all ORG-000066 (foreign) — D09 probe | file API |
| `C-202009779-N00022` | samostatny_nalez | pr=D, stav=1 — creator-org clause probe, both arms match: `predano_organizace` ORG-000091, projekt `C-202009779` org ORG-000091; created SN01 by U-001975 | landing, handle, OAI |
| `C-202211308-N00230` | samostatny_nalez | pr=D, stav=1 — creator-org clause probe, projekt arm only: `predano_organizace` ORG-000099 (mismatch), projekt `C-202211308` org ORG-000091 (match); created SN01 by U-004219 | landing, handle, OAI |
| `C-202009779-N00031` | samostatny_nalez | pr=C, stav=3, projekt `C-202009779` org ORG-000015, `predano_organizace` ORG-000084, SN01 author U-004219 (ORG-000091) — **D16 probe**: every published C-row arm dead; file `f3ba25a8-9c79-46cd-924d-803b56e46e92` | file API, landing, handle |
| `U-004495` | uzivatel | the B test account — own-record clause | OAI |
| `U-004496` | uzivatel | the C test account (org ORG-000091) — counterpart for the creator-org clause probes | session/islogged, landing, OAI |
| `M-202500301` | projekt | production stav=6 projekt with a file — D15 production control | production file API |
| `M-202600319-N00016` | samostatny_nalez | production pr=D, stav=4 find with a file — D09 production control | production file API |
| let / heslo / ruian / organizace / osoba samples | open types | any state-matching record served to anonymous | OAI (re-derive via recipes) |

Test accounts (role B/C/D): `badatel.ai@arup.cas.cz`, `archeo.ai@arup.cas.cz`, `archivar.ai@arup.cas.cz` — passwords are **operator-provided per session and never recorded in this report**. Sessions expire ~30 min.

### Discovery recipes

Do not rely on registry anchors without re-verification; re-discover per session when needed. All steps work anonymously unless stated.

1. **Enumerate and classify records:** `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` (100 records/page, resumptionToken pagination). Classify each record by regex on the body: whole-record restriction `(?s)>\s*HTTP/1\.1 403 Forbidden\s*</amcr:amcr>`; element restriction `<amcr:chranene_udaje>HTTP/1\.1 403 Forbidden`; deleted header `status="deleted"`. Visible records expose `<amcr:stav>` and `<amcr:pristupnost … id="HES-…">` (id → A/B/C/D) for precise classification. `from`/`until` datestamp windows slice the stream.
2. **Predict 401 vs 403:** `GET /api/search/query?entity=<e>&q=ident_cely:"<ident>"` — `numFound: 0` while the record exists in OAI ⇒ not searchable ⇒ anonymous landing returns **401**; `numFound > 0` while OAI shows the record restricted ⇒ **403**.
3. **Visible baselines / element ladder:** from the same OAI scan, take records with the target `stav` and desired pristupnost level (A = open baseline; B/C/D = element hidden up to that role). Direct: `GET /api/search/query?entity=akce&q=pristupnost:<A-E>&rows=5`.
4. **404 candidates:** fabricate idents with valid prefixes but nonexistent numbers (e.g. bump an existing ident's numeric tail far out of range). Expect 404 for all roles.
5. **410 candidates:** scan sets for `status="deleted"` headers — a fresh deletion tops the set listing (tombstone datestamp = deletion time). Deleted **core-type** docs stay in the entities index with `is_deleted=true` and return **410** on landing and handle for every role; deleted `uzivatel`/`osoba` ids 404 (never served on landing by design). Fastest route: maintainer-controlled deletion of a test record, then re-query the set listing.
6. **File-API candidates:** file UUIDs come from the OAI `soubor` elements of **visible** records (`<amcr:url>`, `<amcr:path>`; some records have `soubor` without a Fedora path — skip those). The legacy `/api/search/id` endpoint is removed — do not use it; read file elements from OAI or the search response. Coverage per rule row: dokument and SN files plentiful; projekt files findable via `entity=projekt&q=soubor_filepath:rest*` (the response projects `stav` and `projekt_organizace` — pick per arm: own/other organisation, stav 1–6; stav=1 projekts can be absent from the test data — ask the maintainer); restricted pr=D dokumenty with a chosen organisation via `entity=dokument&q=pristupnost:D AND stav:3 AND soubor_filepath:rest*` and the `<amcr:organizace>` element of the OAI record. For `/img` and `/pdf` probes, the soubor id drives the endpoint.
7. **Child-record candidates:** derive from parent metadata — OAI `archeologicky_zaznam` records list `dokumentacni_jednotka` idents; `adb` records list `vyskovy_bod` idents; cast/komponenta idents follow the fixed suffix patterns. `/api/search/query?entity=komponenta|vyskovy_bod` (with an ident filter) confirms existence (DJ and dokument_cast entity queries are stubs).
8. **Cross-environment comparison:** run the same probes against production; data drifts — never assume the same ident has the same state in both environments. Production probing needs explicit operator approval.
9. **Restricted-children search sweep:** `GET /api/search/query?entity=vyskovy_bod&q=-stav:3&rows=0` (add `AND -pristupnost:A` for elevated children) — a non-zero `numFound` counts children of restricted parents that remain anonymously searchable with unsuffixed fields. Same shape for other child entities where `searchable` is not parent-derived; for komponenta/projekt add `q=...` restricted variants and inspect the chranene blocks — for projekt, sweep `-pristupnost:A AND stav:6` separately (the anonymously visible population, see architecture facts).
10. **Role sessions:** `curl -s -D - "https://digiarchiv-test.aiscr.cz/user/login?user=<email>&pwd=<pwd>" -o NUL` captures `Set-Cookie: JSESSIONID=…`; verify with `/user/islogged?wantsUser=true` replaying the cookie; replay on any surface for role views — the file path accepts the session cookie as well as Basic auth, the landing `checkId` path treats Basic auth as anonymous. OAI accepts Basic auth directly. On a Windows allowlisted shell, put the URL first and the flags after (`curl.exe "https://…/id/<IDENT>" -H "Cookie: JSESSIONID=<SID>" -o NUL -w "%{http_code}\n"`) — flag-first variants may not match the permitted shapes.
11. **Element masking comparison (backend):** for a restricted record, compare the search/handle/export response for anonymous vs B vs C — the suffixed-field mechanism must show the chranene block only from the matching role up; an unsuffixed block present for anonymous is a leak (D11/D12 shape).

### Verification commands

```bash
# landing-page / map status code
curl -s -o /dev/null -w "%{http_code}\n" https://digiarchiv-test.aiscr.cz/id/<IDENT>
curl -s -o /dev/null -w "%{http_code}\n" https://digiarchiv-test.aiscr.cz/map/<IDENT>
# OAI-PMH record (anonymous = restricted view; add -u user:pwd for role view)
curl "https://api-test.aiscr.cz/2.2/oai?verb=GetRecord&identifier=https%3A%2F%2Fapi-test.aiscr.cz%2Fid%2F<IDENT>&metadataPrefix=oai_amcr"
# searchability / search behaviour
curl "https://digiarchiv-test.aiscr.cz/api/search/query?entity=<ENTITY>&q=ident_cely%3A%22<IDENT>%22&rows=1"
# data served to the landing page (element-level / child-leak checks)
curl "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<IDENT>"
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
```

## Current verification (2026-09-24 regression rounds on the test deploy, build v4.0.3-236-g41592838; fresh B/C/D sessions with operator-provided credentials; production compared anonymously with operator approval)

Grid legend: `✓` observed and matches the documented rule · `✗` + finding id = deviation · `·` = no surface for that type by design · `—` = untested on this build (worklist). Codes are the observed HTTP status; element state in parentheses. The grids carry the state verified by the 2026-09-23 full pass with the 2026-09-24 regression rounds applied to every defect-affected cell (build `v4.0.3-230` → `v4.0.3-236` outcomes); holdout cells were re-probed where the fix waves could have moved them.

### Defect outcomes

| Defect | Outcome on test | Production posture |
| --- | --- | --- |
| D08 | **fixed** — the B-owner receives the own SN on OAI (`C-202600009-N00014`, chranene masked per pristupnost) | not examined (needs a B session there) |
| D16 | **fixed** — C 403 on `C-202009779-N00031` (every published arm dead, arms re-verified unchanged); D 200 control | not examined |
| D18 | **fixed** — the dokument B/C rows carry the pristupnost conditions again; the D01 owner and creator-organisation arms preserved | not examined (production follows the published rows) |
| D19 | **fixed** — `entity=projekt` export serves records; restricted projekts export without the chranene block | not examined |
| D07 | fixed on test — restricted VBs carry no geometry fields anonymously | **open — resolved by the upcoming production release** (11,533 restricted-children VB docs, geometry served) |
| D14 | fixed on test — `/pdf` 401 on a restricted dokument | **open — live leak, resolved by the upcoming production release** (real page content of a restricted dokument) |
| D01, D03–D06, D09–D13, D15, D17 | fixed / closed — see the findings registry | D09/D15 absent (published rows hold); D10 absent (endpoint 404); D13 closed by hotfix |

### Issue #370 checklist mapping

1. *Non-archived records visible after login (landing only)* — ✓ `C-202204147A` and `C-TX-193001369`: anon 401, B 200.
2. *Same conditions as the API (#237)* — ✓ at record level, element level and the file gate; no open deviation on the test build (production: D07 and D14 pending the fix-wave release).
3. *404/410/401/403 distinction* — ✓ anonymous: fabricated ident 404; deleted projekt `C-202402033` 410 (also for C on landing/handle and D on handle); unarchived akce/dokument 401; DJ of an unarchived akce and VB of a restricted ADB 403.
4. *Correct return codes* — ✓ on the test build across the probed surfaces; production residuals D07 and D14 remain until the fix wave is deployed there.
5. *Own-organisation finds (C)* — ✓ `C-202009779-N00022` and `C-202211308-N00230`: C 200 on landing and handle (D17 fixed).

### Landing pages (`/id/`, incl. `/map/` variants)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C stav=3 (`C-202009490A`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| akce unarchived (`C-202204147A`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| AZ pr=B (`M-200500013A`) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) | 200 ✓ (full) |
| dokument stav=3 pr=C (`M-TX-202100125`) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument unarchived (`C-TX-193001369`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| adb restricted (`ADB-PRAH71-000861`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| ext_zdroj stav=1 (`BIB-0000052`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| pian state-matching (`P-0134-000003`) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| let (`C-LET-00001`) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=6 pr=A (`M-201300453`) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=6 restricted (`M-200500013` as search; landing follows) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt restricted stav=3 (`C-202600010`) | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| projekt restricted stav=4 own-org (`C-202500044`) | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| SN pr=C stav=4 (`C-202600010-N00085`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) | 401 ✓ | 200 ✓ (owner) | 200 ✓ (org) | 200 ✓ |
| SN pr=D stav=1 org-match (`C-202009779-N00022`) | 401 ✓ | 403 ✓ | 200 ✓ (org) | 200 ✓ |
| SN pr=D stav=1 projekt-org match (`C-202211308-N00230`) | 401 ✓ | 403 ✓ | 200 ✓ (org) | 200 ✓ |
| SN pr=D stav=1 org-match (`C-202009779-N00031`) | 401 ✓ | 403 ✓ | 403 ✓ (all arms foreign — correct) | 200 ✓ |
| SN pr=D stav=4 (`M-202301371-N00006`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) |
| uzivatel, heslo, ruian_kraj/okres/katastr, organizace, osoba | · 404 | · 404 | · 404 | · 404 |
| DJ of restricted akce (`C-202009490A-D01`) | 200 ✓ (inherited, masked) | 200 ✓ | 200 ✓ | 200 ✓ |
| DJ of unarchived akce (`C-202204147A-D01`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument_cast unarchived (`C-TX-193001369-D001`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| VB of restricted ADB (`ADB-PRAH71-000861-V0001`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| komponenta of public SN (`C-202600010-N00085-K001`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (pr-legitimate) | 200 ✓ |
| komponenta of unarchived-parent SN (`C-202600009-N00014-K001`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| deleted projekt (`C-202402033`) | 410 ✓ | 410 ✓ | 410 ✓ | 410 ✓ |
| fabricated ident | 404 ✓ | 404 ✓ | 404 ✓ | 404 ✓ |
| `/map/<unarchived akce>?mapa=true` | 401 ✓ | 200 ✓ | — | 200 ✓ |

Notes: `/map/<ident>` shares the gate with `/id/<ident>`; bare `/map` 200 (D01 fixed). Landing pages are SPA shells — status codes are gates; the element masking cells reflect the handle API data underneath (see handle grid). Element masking on the SN grid rows is verified via the handle bodies (chranene block absent below the matching role).

### OAI-PMH (GetRecord, `oai_amcr`)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| projekt stav=6, oznamovatel case (`C-201122587`) | 200 ✓ (chranene full — pr=A; oznamovatel masked) | 200 ✓ (oznamovatel masked) | 200 ✓ (oznamovatel masked: stav≠1, other org) | 200 ✓ (full incl. oznamovatel) |
| projekt restricted stav=3 (`C-202600010`) | 403 body ✓ | 403 body ✓ | 200 ✓ (stav>0) | 200 ✓ (full) |
| projekt restricted stav=4 own-org (`C-202500044`) | 403 body ✓ | 403 body ✓ | 200 ✓ | 200 ✓ |
| akce pr=C stav=3 (`C-202009490A`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| AZ pr=B (`M-200500013A`) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) | 200 ✓ (full) |
| akce unarchived (`C-202204147A`) | 403 body ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument pr=C stav=3 (`M-TX-202100125`) | 200 ✓ (no chranene element by spec) | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument unarchived (`C-TX-193001369`) | 403 body ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| adb restricted (`ADB-PRAH71-000861`) | 403 body ✓ | 200 ✓ (chranene masked) | 200 ✓ (chranene full) | 200 ✓ (full) |
| ext_zdroj stav=1 (`BIB-0000052`) | 403 body ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| pian state-matching pr=A (`P-0134-000003`) | 200 ✓ (chranene visible) | — | — | — |
| let (`C-LET-00001`) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| heslo (`HES-000865`), organizace (`ORG-000091`) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| uzivatel other's record (`U-004495` as anon/C) | 403 body ✓ | 200 ✓ full own record | 403 body ✓ other's record | 200 ✓ (full) |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) | 403 body ✓ | 200 ✓ (SN01 owner clause) | 200 ✓ (projekt org matches — rule-conformant) | 200 ✓ |
| SN pr=C stav=4 (`C-202600010-N00085`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| SN pr=D stav=4 (`M-202301371-N00006`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) |
| SN pr=D stav=1 org-match (`C-202009779-N00022`) | 403 body ✓ | — | 200 ✓ (chranene masked per pr=D — org clause works) | 200 ✓ (full) |
| SN pr=D stav=1 projekt-org match (`C-202211308-N00230`) | 403 body ✓ | — | 200 ✓ (chranene masked per pr=D) | 200 ✓ (full) |
| deleted projekt (`C-202402033`) | tombstone ✓ | tombstone ✓ | tombstone ✓ | tombstone ✓ |

Notes: `oai_dc` is derived after filtering — the restricted projekt returns `<oai_dc:dc …>HTTP/1.1 403 Forbidden</oai_dc:dc>` (no leak); ListRecords carries tombstones and per-record 403 bodies like GetRecord (verified on `set=projekt`); the version-less `/oai` endpoint works with Basic auth via redirect (`-L`); identifiers must use the environment's own domain prefix.

### Search API (`/api/search/query`)

Visibility of restricted records in anonymous results is by design (public fields, masked protected fields). The grid records **element masking** on the restricted probe records.

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ no chranene block | ✓ no chranene block | ✓ full chranene block | ✓ full chranene block |
| SN pr=C stav=4 (`C-202600010-N00085`) | ✓ no chranene block | ✓ no chranene block | ✓ full chranene block | ✓ full chranene block |
| adb restricted (`ADB-PRAH71-000861`) | ✓ block not served | ✓ | ✓ | ✓ (chranene not projected on the search path for any role — suffixed-alias projection) |
| pian pr=C (`P-2213-100119`) | ✓ block not served | ✓ | ✓ | ✓ (chranene not projected on the search path for any role — the geometry exposure routes are the map/geometry actions, see D07/VB) |
| dokument pr=C | ✓ (no protected blocks by spec) | ✓ | ✓ | ✓ |
| ext_zdroj stav=1 | ✓ (no protected fields) | ✓ | ✓ | ✓ |
| projekt restricted, stav≠6 (sweep — 38 on test) | ✓ no chranene block | ✓ masked | ✓ block served — pr-legitimate | ✓ block served — pr-legitimate |
| projekt restricted, stav=6 (sweep — 24 on test, `M-200500013` in population) | ✓ no chranene block (D12 fixed) | ✓ masked | ✓ block served — pr-legitimate | ✓ block served — pr-legitimate |
| projekt pr=A stav=6 (`M-201300453`) | ✓ chranene block served (pr-legitimate) | ✓ | ✓ | ✓ |
| komponenta of public SN (`…-K001`) | ✓ public fields only, no chranene block | ✓ masked | ✓ pr-legitimate | ✓ |
| oznamovatel (any type) | ✓ not served by any backend surface | ✓ | ✓ | ✓ |
| uzivatel/heslo/ruian/organizace/osoba | · (search queries the entities core only) | · | · | · |

Notes: legacy `/api/search/id` action removed (500 no-enum, no data served); `entity=dokumentacni_jednotka`/`dokument_cast` are pre-existing stubs; D07 on test — anonymous `entity=vyskovy_bod&q=-stav:3 AND -pristupnost:A` returns 8 restricted-children docs with **no geometry fields**; the restricted-projekt sweep's `lat_A` stats field is aggregate-only (coarse min/max, no coordinates served).

### Handle API (`/api/search/handle?id=`)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ error/masked, no chranene | ✓ masked (no az/akce chranene, no loc_rpt) | ✓ full (`akce_chranene_udaje` + `az_chranene_udaje` + `pian_chranene_udaje`) | ✓ full |
| SN pr=C stav=4 (`C-202600010-N00085`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full chranene) | 200 ✓ |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) | 401 ✓ | 200 ✓ (owner; chranene masked) | 200 ✓ (full) | 200 ✓ |
| SN pr=D stav=1 org-match (`C-202009779-N00022`, `C-202211308-N00230`) | 401 ✓ | 403 ✓ | 200 ✓ (org clauses work — D17 fixed) | 200 ✓ |
| SN stav=3 all-orgs-foreign (`C-202009779-N00031`) | 401 ✓ | 403 ✓ | 403 ✓ (correct — all arms foreign) | 200 ✓ |
| deleted projekt (`C-202402033`) | 410 ✓ | 410 ✓ | 410 ✓ | 410 ✓ |
| DJ/dokument_cast/VB (restricted/unarchived parents) | ✓ matching codes, no data | ✓ | ✓ | ✓ |
| komponenta of public SN (`…-K001`) | ✓ no chranene block | ✓ masked | ✓ 200 (pr-legitimate) | ✓ |
| projekt restricted stav=3/4 (`C-202600010`, `C-202500044`) | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| projekt restricted stav=6 pr=B (`M-200500013`) | ✓ no chranene block (D12 fixed) | ✓ block served (pr-legitimate) | ✓ block served | ✓ |

### File API (`/id/<ident>/file/<uuid>` and variants)

| Rule row / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| projekt stav=6 other-org (`C-202101848`) — orig | 403 ✓ | 403 ✓ | 403 ✓ | 200 ✓ |
| projekt stav=3 other-org (`C-202600010`) — orig | 403 ✓ | — | 403 ✓ | 200 ✓ |
| projekt stav=4 own-org (`C-202500044`) — orig | 403 ✓ | 403 ✓ | 200 ✓ | — |
| projekt stav=6 own-org (`C-202007460`) — orig | 403 ✓ | — | 200 ✓ | — |
| projekt stav=1 (`C-202111855`) — orig | 403 ✓ | 403 ✓ | 200 ✓ | — |
| dokument pr=A stav=3 (`C-TX-192700656`) — orig | 200 ✓ | 200 ✓ | — | — |
| dokument pr=C stav=3 (`M-TX-202100125`) — orig | 403 ✓ | 403 ✓ | 200 ✓ | — |
| dokument pr=D stav=3 own-org (`C-TX-202400188`) — orig | 403 ✓ | 403 ✓ | 200 ✓ (D01 creator-org arm) | — |
| dokument pr=D stav=3 other-org (`M-TX-202100123`) — orig | 403 ✓ | 403 ✓ | 403 ✓ | — |
| dokument pr=D stav=1 B-created (`X-C-TX-000001130`) — orig | 403 ✓ | 200 ✓ (D01 owner clause) | 200 ✓ (D01 creator-org arm) | — |
| └ `/thumb` on `M-TX-202100125` | 200 ✓ (documented ungated) | — | — | — |
| └ `/thumb/page/1` on `M-TX-202100125` | 403 ✓ (follows orig) | — | — | — |
| └ `/thumb-large` on `M-TX-202100125` | 403 ✓ | — | — | — |
| └ `/paradata` on `M-TX-202100125` | 403 ✓ | — | — | — |
| SN pr=C stav=4 (`C-202600010-N00085`) — orig | 200 ✓ (stav=4) | 200 ✓ | 200 ✓ | 200 ✓ |
| SN pr=A stav=4 (`C-202500044-N00001`) — orig | 200 ✓ | — | — | — |
| SN pr=D stav=4, all organisation arms other (`M-202301371-N00006`) — orig | 200 ✓ (stav=4) | 200 ✓ | 200 ✓ | 200 ✓ |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) — orig | 403 ✓ | 200 ✓ (SN01 owner) | 200 ✓ (projekt org) | 200 ✓ |
| SN stav=3, projekt+predano foreign, SN01 author own-org (`C-202009779-N00031`) — orig | 403 ✓ | — | 403 ✓ (owner arm on the author's identity — D16 fixed) | 200 ✓ |
| distributions & paradata follow the original's verdict (#693; `M-TX-202100125` `/paradata`, `C-TX-192700656` `atr/stats-csv` and `paradata/orig`, `…-N00085` `/paradata`) | ✓ 403 with orig on the restricted dokument; 200 on public and stav=4 forms | ✓ matches the original's verdict (403 with orig) | ✓ 200 with orig | — |
| cross-record uuid binding | 404 ✓ | — | — | — |
| rate limiter | 429 on bursts (also across distinct files — documented ≥ 1 s spacing; `Retry-After` observed `0` interval / `500` concurrent) | — | — | — |

Notes: the file path accepts the session cookie as well as Basic auth; the landing `checkId` path treats Basic auth as anonymous. The #693 distribution-gate pattern lives in the [`file-distributions`](file-distributions.md) scenario.

### Images and PDF (`/img/*`, `/pdf`)

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/img/thumb?id=<restricted dokument soubor>` | ✓ 200 (documented ungated small thumb) | — | — | — |
| `/img/full?id=<restricted dokument soubor>` | ✓ 401 (ImageAccess gate holds: imgPr > anon) | — | — | — |
| `/img/full?id=<restricted SN soubor>` | ✓ gate passes anonymous — no 401, SN always allowed (issue #85); serve 500s on this test file (backend path defect, not a gate) | — | — | — |
| `/pdf?id=<restricted dokument soubor id>&page=1` | ✓ 401 (ImageAccess gate) | — | — | — |
| `/pdf?id=<SN soubor id>&page=1` | ✓ gate passes per #85; cold cache serves the 7,525 B placeholder only — no content leak | — | — | — |
| production `/pdf?id=<restricted dokument>&page=1` | ✗D14 200 — real page content of a restricted dokument (live leak; resolved by the upcoming production release) | — | — | — |

Notes: `/img` and `/pdf` gate on the **soubor id** (`soub-XXXXXX`), not the Fedora uuid — a uuid `id` returns 401 as a not-found, which is indistinguishable from a gate refusal on status alone; verify with the soubor id. `/img/thumb-large` is not a valid ImageServlet action (500 no-enum); the large-size variant lives in the File API `/thumb-large` path.

### Export and geometry (`/exp`, `/api/search/gml|wkt|geometrie|pians|mapa|export*`)

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/exp?entity=projekt&q=<ident>` (public and restricted) | ✓ export serves records; restricted projekts carry no chranene block in the export field set | — | — | — |
| `/exp?entity=akce&q=<restricted ident>&format=json` | ✓ served, no chranene in the export field set | — | — | — |
| `/exp?entity=samostatny_nalez&mapa=true&geometrie=GeoJSON` (restricted SN) | ✓ `numFound=0` — record filtered out entirely for anonymous; no chranene, no error | — | — | — |
| `/exp?entity=komponenta&q=<restricted komponenta>&format=csv` | ✓ no katastr column value (masked) | ✓ row served, katastr masked | ✓ row served with katastr — pr-legitimate | — |
| `/exp?entity=pian&mapa=true` (restricted) | ✗ 500 (unhandled error page for anonymous on a restricted pian; no data leaked — robustness defect, not a leak) | — | — | — |
| `/api/search/geometrie?id=<restricted pian>&loc_rpt=<bbox>&format=GeoJSON` | ✓ `{}` — the query runs on the role-suffixed `loc_rpt_A` field, which does not match restricted pians | — | — | ✓ geometry served (pr-legitimate) |
| `/api/search/pians?entity=pian&q=pristupnost:C&rows=200` | ✓ 327 restricted pians served — idents and pian_id only, no coordinates | — | — | — |
| `/api/search/gml`, `/wkt` | — | — | — | — |

### Admin and integration surfaces

| Surface / probe | anon (from this workstation's VPN/local-network position) | B | C | D |
| --- | --- | --- | --- | --- |
| test `/fedora/get_id?id=<restricted projekt>` | n.a. — action removed (500 no-enum; the request passes the network-trusting gate from this position) | — | — | — |
| test `/fedora/request?url=…` | n.a. — action removed likewise (500 no-enum) | — | — | — |
| production `/fedora/get_id`, `/fedora/request` | n.a. — actions removed by the 2026-09-15 hotfix (500 no-enum; no data served) | — | — | — |
| `/mus/predmety_by_id`, `/mus/statistika` | ✓ reachable, empty test data (no demonstrable leak) | — | — | — |
| `/api/search/stats` | ✓ 200 public aggregates | — | — | — |
| `/api/search/stats_index` | ✓ 200 anonymous (index aggregates) | — | — | — |
| `/fav/*` | ✗ 500 anonymous (ungated servlet but errors without a user; no data served) | — | — | — |

### Findings registry

| ID | Severity | Surface | Status | One-line summary |
| --- | --- | --- | --- | --- |
| D01 | Low | bare `/map` | **fixed** | `/map` forwards to `/map2` (200) |
| D02 | — | SN record rules | **changed** | fix-wave rewrite superseded by D06/D09 |
| D03 | High | legacy `/api/search/id` | **fixed — action removed** | anonymous leak of non-searchable records → action removed |
| D04 | Low | file API rate limiter | **closed** (operator decision: reactive handling) | both limiter branches live-verified (interval `Retry-After: 0`; concurrent `Retry-After: 500` — reads as milliseconds in a seconds-defined header) |
| D05 | Low | child-record landing | **fixed** | DJ/dokument_cast/VB inherit parent rules again; search residual = D07 |
| D06 | High | SN record level | **fixed** | the A/B branches' `pristupnost <= userPr` conditions contradicted #237's `stav=4` rule — the public stav=4 SN serves anon/B masked |
| D07 | High | search API (vyskovy_bod) | **fixed on test; open on production — resolved by the upcoming production release** | restricted VB geometry anonymously searchable (prod: 11,533 restricted-children docs) |
| D08 | High | OAI-PMH (SN) | **fixed** | the OAI-side SN owner clause resolves — the B-owner receives the own SN with chranene masked per pristupnost |
| D09 | Medium | file API | **fixed** | file-gate SN rows over-blocked: the A/B/C rows carry `stav=4` without pristupnost conditions, matching the published table |
| D10 | High | file API `/thumb-large` | **fixed** | undocumented endpoint bypassed all permission checks; the variant matrix now follows the original's rules |
| D11 | High | backend surfaces (komponenta) | **fixed** | komponenta docs served the SN chranene block unmasked; now masked below the matching role |
| D12 | High | search + handle API (projekt) | **fixed** | anonymous search/handle served `projekt_chranene_udaje` for restricted stav=6 projekts; the post-filter drops the block whenever pristupnost exceeds the user's role |
| D13 | High | `/fedora/*` | **closed — solved in current state** (operator accepted the network-trusting posture and the mutating actions as-is) | unauthenticated raw-Fedora access from the local network/VPN; read actions removed on both deployments |
| D14 | High | `/pdf` | **fixed on test; open on production — live leak, resolved by the upcoming production release** | PDF page JPEGs served with no permission gate on the production build |
| D15 | High | file API (projekt) | **fixed** | projekt file gate applied the record-level rule instead of the published `//projekt/soubor` row |
| D16 | Medium | file API (SN) | **fixed** | the SN file-gate C-row owner arm compares the SN01 author's identity (`userId.equals(uzivatelSN01)`), matching the published table and the code's rule comment; the organisation-of-author grant is gone |
| D17 | High | landing pages + handle API (SN) | **fixed** | the SN organisation clauses resolve on the entities path (checkId projects projekt/predano organisations) |
| D18 | High | file API (dokument) | **fixed** | the dokument file-gate B and C rows carry the pristupnost conditions again; the D01 owner and creator-organisation arms preserved |
| D19 | Medium | export (`/exp`) | **fixed** | `entity=projekt` export serves records; restricted projekts export without the chranene block |

No open findings on the test build. The production posture (D07, D14) resolves with the fix-wave release there; the production later-check task is recorded on the driving issue.

### Pre-existing observations

- The `dokumentacni_jednotka` and `dokument_cast` entity-search endpoints are stubs and return error bodies — pre-existing, unrelated to permissions, no data exposure.
- Quoted-phrase Solr interpolation (code-verified, low): `HandleServlet.getDocument`'s `soubor_filepath:"<url-path>"` filter and OAIRequest's `ident_cely:"<id>"` interpolate the request path/identifier without escaping query metacharacters (`InitServlet.asSafePath` blocks traversal and backslashes only). No privilege bypass found — the permission gate still runs on whatever document the query returns — but the construction is injection-shaped; escaped terms would remove the class.
- Test-data quality: several records carry placeholder file paths (`"neni"`) and omit `soubor/path`+`soubor/url` elements in OAI output; well-formed records (verified via the D view) do serve both. Probes use well-formed records — do not read the omission as build behaviour.
- Environment-specific file bindings: `M-TX-202100125` binds test file `3a0f7078-…` but production file `fe377f16-…`; always re-read the soubor path from the environment being probed.
- Production index: the production entities core reports komponenta docs in its count surface, but `entity=komponenta` queries error there — komponenta fix verification stays test-side and is not inferred from the count.

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
