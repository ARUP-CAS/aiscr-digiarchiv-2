# Permission model — testing scenario

**Key:** permissions (feature)

**Scope:** the permission behaviour of every backend surface that serves record data — landing pages, OAI-PMH, search/handle/legacy APIs, geometry and export endpoints, the File API, the image and PDF endpoints, and the admin/integration servlets — at record level and element level, for all roles. Issues [ARUP-CAS/aiscr-digiarchiv-2#370](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/370) and [#237](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/237) (rules definition) drove it.

**How to use this record:** the *Durable knowledge* half states the rules and the complete surface inventory; the *Current verification* half is a set of **coverage grids** (record type × role per surface). A grid cell records the observed result for that combination: `✓` = matches the documented rule, `✗` + finding id = deviation, `·` = no surface for that type by design, `—` = **untested on this build — the worklist for the next pass**. Fill or annotate cells only with fresh evidence; never mark a cell from assumption.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | `https://api-test.aiscr.cz/2.2/oai` | has #370 plus the 2026-09 fix wave; the 2026-09-17 deploy completes the D06 fix (A/B branches follow #237's `stav=4` rule), removes the legacy `/api/search/id` action (500 no-enum), gates `/pdf` via `ImageAccess`, fixes the OAI SN creator-organisation clauses (D08 narrowed to the B-owner clause), and unifies the file gate across authenticated/anonymous branches — the unified SN A/B rows apply pristupnost conditions the published File API rule table does not state (permissions-D09, changed shape); remaining divergences: D08 (OAI owner clause), D09 (file-gate SN rows), D12 (restricted stav=6 projekts, reopened 2026-09-23), D15 (projekt file gate, 2026-09-23). Build identity (2026-09-23): the served bundle embeds `v4.0.3-225-g45b9e5b1` (clean) while the backend behaves as the later `749f7ec0` (its SN A-row `pristupnost` condition is live) — the footer hash does not identify the backend deploy; establish the backend by behaviour |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | `https://api.aiscr.cz/2.2/oai` | re-probed anonymously 2026-09-23 (operator-approved): still the pre-fix-wave build — `/pdf` serves real page JPEGs of restricted dokumenty anonymously (D14 live leak persists), restricted-children VB geometry served unsuffixed (D07; 11,533 docs), `/thumb-large` absent (404), SN file gate and projekt file gate follow the published rule table (D09 and D15 absent), no D12 stav=6 exposure. Earlier state (2026-09-15): partial hotfix deployed — the FedoraServlet read actions `GET_ID`/`REQUEST` are removed (500 no-enum) — the D13 raw-read leak is closed; the rest of the fix wave is **not** deployed there: `/pdf` ungated **and serving real page JPEGs of restricted dokumenty to anonymous clients** (warm cache — permissions-D14 live leak), VB geometry unsuffixed (D07), `/thumb-large` endpoint absent (404) |

OAI quirks: on the test instance, OAI identifiers use the **`https://api-test.aiscr.cz/id/…`** prefix (GetRecord with the production prefix silently fails with `idDoesNotExist`); set names containing a colon must be percent-encoded (`set=dokument%3A3d`). Versioned OAI endpoints (`/2.1/oai`, `/2.2/oai`) serve the same filtered core and differ only by XSLT. The **version-less** endpoint `https://api-test.aiscr.cz/oai` 302-redirects to the current version — a convenient Basic-auth role-probe shape.

### Surface inventory (routes and gates)

Complete enumeration from `web.xml` + `@WebServlet` annotations (2026-09-10 source audit). `ApiServlet` (`/api`, also `/api/*` in web.xml) is a pure path-normalizing forwarder, so every action servlet is also directly reachable under its own path (e.g. `/search/query` ≡ `/api/search/query`).

| Route | Servlet / action | Gate mechanism | Permission-relevant behaviour |
| --- | --- | --- | --- |
| `GET /id/<ident>`, `GET /map/<ident>` | `HandleServlet` `checkId` | 404 (no doc in `entities`) → 410 (`is_deleted`) → 401 (`!searchable` + anonymous) → 403 (per-model `filterOAI`) → 200 | landing / map gating; rendered page is a shell, data comes from the handle API |
| `GET /id/<ident>/file/<uuid>` (+ `/thumb`, `/thumb/page/N`, `/thumb-large`, `/paradata`, `/paradata/<distribution>`, distribution suffixes per #693) | `HandleServlet` file path | `HandleServlet.isAllowed` per XPath rule row; #693 rule: distributions and paradata follow the original's rules; Basic auth accepted here (not on `checkId`); rate limiter per (ip, file) | File API; `/thumb` documented ungated; `/thumb-large` gated on the test build since the D10 fix (2026-09-11 verified); #693 test pattern below |
| `GET /oai/*` (verbs GetRecord/ListRecords/ListIdentifiers; oai_amcr, oai_dc) | `OAIServlet` → `OAIRequest.filter` | per-model `filterOAI` (record level) + element string surgery (chranene/oznamovatel) | OAI-PMH; Basic auth logs in; oai_dc derived after filtering |
| `GET /api/search/query` | `SearchServlet` QUERY → per-entity `EntitySearcher` | pristupnost-suffixed projected fields + `searcher.filter(jo, pristupnost, org)` post-filter | search visibility + element masking; E downgraded to D |
| `GET /api/search/handle?id=` | `SearchServlet` HANDLE | per-model `filterOAI` on the entities doc | record JSON for landing pages |
| `GET /api/search/id` | `SearchServlet` ID | action **removed** in the 2026-09-15 builds (500 no-enum error, no data served); was `searchable`-derived (gated since the fix wave) | legacy id lookup — gone; do not use for probing |
| `GET /api/search/id_as_child` | `SearchServlet` ID_AS_CHILD | child-doc fetch | child record JSON (verify per parent state) |
| `GET /api/search/gml`, `/wkt`, `/geometrie` | `SearchServlet` GML/WKT/GEOMETRIE | GEOMETRIE queries the role-suffixed `loc_rpt_<pr>` field (masks restricted pians for anonymous — verified `{}` on restricted coordinates); its pristupnost check is commented out in source (latent if the suffixing is dropped); reads `pian_chranene_udaje` blocks | geometry extracts |
| `GET /api/search/pians` | `SearchServlet` PIANS → `PIANSearcher.getMapPians` | fields include unsuffixed `pian:[json]` + `loc_rpt` | map pian points — live-probe candidate |
| `GET /api/search/mapa`, `/export`, `/export_mapa` | `SearchServlet` MAPA/EXPORT/EXPORT_MAPA | per-searcher filter (same as query) | map search + export variants |
| `GET /api/search/stats`, `/stats_index`, `/export_stats_index`, `/obdobi`, `/thesauri`, `/log`, `/check_relations`, `/home` | `SearchServlet` auxiliary actions | aggregate/heslar/analytics | statistics and vocabularies — low sensitivity, spot-check |
| `GET /exp` | `ExportServlet` → `searcher.export(request)` | **no servlet-level gate**; protection only inside `EntitySearcher.export` (per-searcher `filter()`); map path reads `*_chranene_udaje` geometry | csv/xlsx/xml/json export; `mapa=true` serves geometry (GeoJSON/GML) — live-probe candidate |
| `GET /pdf?id=<soubor_id>&page=N` | `PdfServlet` | test build (2026-09-11): `ImageAccess.isAllowed(request, true)` — the gate resolves the dokument by the **soubor id** (`getDokBySoubor`); a record-ident `id` resolves to nothing and returns **401 as a not-found**; SN always allowed (issue #85), dokument by `pristupnost` | PDF page images from the `thumbsDir` cache; production build still has **no gate** — and its page cache has warmed: real page JPEGs of restricted dokumenty are served to anonymous clients (permissions-D14, live leak 2026-09-15) |
| `GET /img/thumb?id=<soubor_id>`, `/img/full?id=` | `ImageServlet` actions (only `thumb` and `full` exist — `thumb-large` is not a valid action and 500s) | `thumb` ungated (`ImageAccess.isAllowed(…, false)` = true); `full` gated by `ImageAccess.isAllowed(…, true)` + rate limiter | image variants by soubor id; `ImageAccess` full gate: SN → **always allowed** (issue #85), dokument → imgPr=A or userPr ≥ imgPr or same-org (same-org restricted to users ≤ C) |
| `GET /fedora/*` | `FedoraServlet` (test build actions: INDEX_FULL, STOP_INDEX, INDEX_UPDATE, STOP_UPDATE, INDEX_ENTITIES, INDEX_MODEL, INDEX_ID, REINDEX_FILTER, CHECK_DATESTAMP — **GET_ID/REQUEST removed**; production removed GET_ID/REQUEST in the 2026-09-15 hotfix) | allowedIP list OR localhost OR `pristupnost >= indexSecLevel` (config, default E) — **the gate trusts the network position: unauthenticated local-network/VPN clients pass the gate** (the 500 no-enum error for an invalid action is raised inside the gate-passed branch); public-internet anonymous denied (operator-verified 2026-09-10) | raw Fedora object access + reindex — admin surface; the raw record XML read paths are closed on both deployments (read actions removed, permissions-D13 closed 2026-09-17 — the network-trusting posture and mutating actions accepted as-is by operator decision) |
| `GET /mus/*` | `MuseionServlet` | pristupnost read (default A when anonymous); FORBIDDEN paths present — gate shape unclear from source | museion predmety integration — probed 2026-09-10: reachable, empty test data, no demonstrable leak |
| `GET /fav/*` | `FavoritesServlet` | session `userid` scoping | per-user favourites only |
| `GET /user/*` | `LoginServlet` | — | login/logout/islogged (credentials, not record data) |
| `GET /texts/*`, `/config/*`, `/i18n/*`, `/feedback` | Texts/Config/I18n/FeedbackServlet | — | UI chrome, no record data |
| JSP shells: `/results`, `/export`, `/export-mapa`, `/print`, `/stats`, `/museion`, `/favorites`, `/home`, `/id2/*`, `/map2/*`, `/registrace`, `/napoveda` | `StaticServlet` (index.jsp) | none (client-side) | all data comes from the API routes above |

`PausedFilter` on `/*` is an availability gate only. `ApiServlet.getSafePath` blocks `..` and backslashes; traversal-shaped input dies as 404.

### Architecture and implementation facts

- **Solr core routing decides landing-page existence.** `HandleServlet.checkId` queries the **`entities`** core. Top-level types there: `projekt`, `archeologicky_zaznam` (akce/lokalita), `dokument` (incl. 3D), `adb`, `pian`, `samostatny_nalez`, `ext_zdroj`, `let`. Child entities there: `dokumentacni_jednotka`, `komponenta`, `komponenta_dokument`, `dokument_cast`, `vyskovy_bod`, `neident_akce`. Types `uzivatel`, `heslo`, `ruian_*`, `organizace`, `osoba` index into their own cores → **always 404 on landing pages** (intended feature). The **`oai`** core serves OAI-PMH for all types, so OAI-PMH can serve records the landing page cannot.
- **filterOAI call sites use fresh model instances.** Both `HandleServlet.checkId` and `OAIRequest.filter` obtain the model via `FedoraModel.getFedoraModel(entity)`, which returns a **new, unpopulated instance**. A `filterOAI` implementation must read record state from the `SolrDocument` argument; model instance fields (e.g. `stav`) are default-valued on these paths. `SamostatnyNalez.filterOAI` violated this and broke SN record-level visibility (permissions-D06); the 2026-09-11 build reads the doc's `stav`/`pristupnost`/`historie`, which fixed the C/D rows — and the 2026-09-15 build completes the fix: the A/B branch conditions follow #237's `stav=4` rule (all roles conform; D06 fixed).
- **Where the rules live:** record-level rules in `FedoraModel.filterOAI` implementations (`web4/fedora/models/*.java`); file rules in `HandleServlet.isAllowed`; image rules in `ImageAccess`; element-level hiding in `OAIRequest.filter` (OAI) and via pristupnost-suffixed secured fields plus per-searcher `filter()` post-removal (search/handle/export).
- **The suffix mechanism has two halves and both must hold:** the query must project role-suffixed variants (`loc_rpt_<pr>`, `lat_<pr>`, suffixed secured JSON fields) **and** the post-filter must remove the unsuffixed block when `doc.pristupnost > userPr`. A searcher whose projected fields omit `pristupnost`, or whose `filter()` throws on a missing field, silently serves the unsuffixed block (permissions-D12 shape on projekt; D11 on komponenta — both fixed on the 2026-09-11 test build).
- **Restricted `stav=6` projekts are a distinct D12 population.** A stav=6 projekt is anonymously visible at record level, so its search and handle documents reach anonymous clients and rely entirely on the post-filter to drop `projekt_chranene_udaje`. On the test deploy (2026-09-23) that block is served **unsuffixed** to anonymous for restricted stav=6 projekts (search and handle), while OAI masks it; restricted projekts at other stav are masked. The earlier D12 sweeps counted 38 restricted projekts — exactly the non-stav-6 population — so a D12 sweep must include `stav:6` explicitly (recipe 9).
- **`/map/<ident>`** applies the identical gating before forwarding to `/map2`. Bare `/map` forwards to `/map2` since the 2026-09 fix wave (was 500, permissions-D01).
- **Rate limiting:** File API and `/img/full` requests are tracked per IP + file; keep **≥ 1 s between requests**; a duplicate inside the window returns **429** with `Retry-After: 0` (interval branch, live-verified 2026-09-10 and re-observed 2026-09-11); the concurrent-in-progress branch (`Retry-After: -1`) is code-verified only. The limiter keys on client IP, so proxy buffering or egress-IP rotation can defeat observation from outside.
- **Page cache:** on test (`isTestEnv=true`) the landing-page cache is skipped entirely; on production a 1-day anonymous cache exists but only affects the rendered shell (record data is always fetched live per user), so it has no permission impact.
- **Child-entity indexing (since the 2026-09 fix wave):** `dokumentacni_jednotka` docs stay `searchable=true` and are gated in `filterOAI` by the parent-propagated `stav` (anonymous gets 403, not 401, for a non-archived parent); `dokument_cast` inherits the parent dokument's `searchable` (401 shape); `vyskovy_bod` inherits `stav`/`pristupnost`/`searchable` from the ADB at index time. VB geometry fields (`vyskovy_bod_geom_wkt`, `vyskovy_bod_geom_gml`) remain indexed **without pristupnost suffixes**: the 2026-09-11 test build no longer serves them anonymously (D07 fixed there), production still does (D07 open there).
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

The deployed `SamostatnyNalez.filterOAI` follows the table's A/B rows on the 2026-09-15 build (the `pristupnost <= userPr` divergence is gone — permissions-D06 fixed); the C-branch organisation clauses (project organisation and receiving organisation, `predano_organizace`) work on the entities path and — since the 2026-09-17 deploy — on OAI as well (both org arms; see D08), while the B-owner clause remains dead on OAI (permissions-D08 residual). Source reading 2026-09-23 (`origin/dev`): the `filterOAI` C branch is SN01 owner OR project organisation OR `predano_organizace`; the SN01 creator's organisation (`organizaceUzivatele`) is computed but not used, so OAI has no creator-organisation arm.

**`searchable` flag (drives anonymous 401 vs 403):** AZ = stav 3; dokument = stav 3; samostatny_nalez = stav 4; pian = ident does not start with `N`; projekt = has related dokument/samostatny_nalez/akce; adb, let, ext_zdroj = always searchable; uzivatel = field not set (moot, 404 anyway); **child entities** — komponenta and dokument_cast inherit the parent's state; dokumentacni_jednotka and vyskovy_bod are `searchable=true` at index time and rely on `filterOAI` for gating (DJ reads the parent-propagated `stav`; VB inherits the ADB's full state triple).

**Element-level (inside a 200 response):**

- `chranene_udaje` (katastr, lokalizace, geometrie) hidden when record pristupnost > user pristupnost. Applies to projekt, archeologicky_zaznam, adb, pian, samostatny_nalez; **dokument and ext_zdroj have no element-level restriction** by spec.
- `projekt/oznamovatel`: A-B never; C only stav=1 or own organisation; D-E always. The handle API does not serve the element at all (see architecture facts).
- In OAI-PMH responses, restricted elements are replaced by the literal `HTTP/1.1 403 Forbidden`; restricted whole records get `<amcr:amcr…>HTTP/1.1 403 Forbidden</amcr:amcr>`; deleted records carry `status="deleted"` headers.
- An entity's deviation from this mechanism is recorded in that entity's scenario (for komponenta, see the [`komponenta`](komponenta.md) scenario).

**File API** (`https://…/id/<ident>/file/<uuid>[/thumb][/thumb/page/N]`, Basic auth only; see <https://arup-cas.github.io/aiscr-api-home/file-api/>):

| XPath | A | B | C |
| --- | --- | --- | --- |
| `//projekt/soubor` | never | never | stav=1 OR (stav 2-6 AND own org) |
| `//dokument/soubor` | pr=A AND stav=3 | (pr<=B AND stav=3) OR own (D01) | (pr<=C AND stav=3) OR own org |
| `//samostatny_nalez/soubor` | stav=4 | stav=4 OR own (SN01) | + org (predano/projekt) |

Source reading and live probe 2026-09-23: since `a0bb69f2` (#370, 2026-09-11) the `projekt` case of `HandleServlet.isFileAllowed` applies the record-level projekt rule — A/B `stav=6`, C `stav>=1`, no organisation condition — instead of the `//projekt/soubor` row above, which `origin/main` still implements. Confirmed on test 2026-09-23: anonymous and B download a stav=6 projekt's file, and C downloads another organisation's stav=3 projekt file; production still implements the published row. The operator confirmed 2026-09-23 that the published row is intended — permissions-D15, defect (fix direction: restore the organisation-scoped projekt file gate before the fix wave reaches production).

Deployed divergence (2026-09-17 test build): `HandleServlet.isFileAllowed` applies pristupnost-qualified SN rows — A: `pristupnost=A AND stav=4`; B: `(pristupnost<=B AND stav=4) OR SN01 owner`; C: `(pristupnost<=C AND stav=4) OR predano_organizace OR SN01 author's organisation OR projekt organisation` — stricter than the published rule table above for the A and B rows (the dokument rows match the published table). Anonymous and B are denied files of stav=4 SNs with elevated pristupnost (3,397 test records), where the published table promises `stav=4` alone suffices for A/B — permissions-D09, operator-confirmed defect (the published table is authoritative; fix direction: drop the SN A/B pristupnost conditions in the code). Probed 2026-09-23: the C row carries the same shape — C is denied the file of a stav=4 pr=D find whose projekt, receiving organisation and SN01 author all belong to another organisation, where the published table grants it (`stav=4`) — so D09 covers the A, B **and** C rows. Its organisation clauses also include the SN01 author's organisation, which the published table does not state (the arm predates #370); that arm could not be isolated with the test accounts, all of which belong to one organisation (permissions-D16 candidate).

Small thumbnails (`/thumb`) are **always public**; large thumbnails (`/thumb/page/N`, `/thumb-large`) follow the original-file rules on the test build (re-verified 2026-09-17). **Image endpoints** (`/img`) apply their own `ImageAccess` rules (see surface inventory) — SN always allowed at full size, dokument by pristupnost. **`/pdf`** shares those rules on the test build (gated since the D14 fix; 401 re-verified 2026-09-17); the production build still served it ungated as of 2026-09-15 (not examined 2026-09-17).

**Child records:** child entities live in the entities core with their own idents derived from the parent: `dokumentacni_jednotka` = `<AZ>-D01…`; `komponenta` (under AZ or SN) = `<parent>-K001…`; `dokument_cast` = `<dokument>-D001…`; `vyskovy_bod` = `<ADB>-V0001…`.

### Probe-record registry

Anchors from the 2026-09-10/11 passes on `digiarchiv-test`. **Drift rule:** records change state; re-verify each record's current state (OAI or search) before using it as a probe, and re-derive replacements via the recipes when it has drifted. File UUIDs come from the record's `soubor` elements and are **environment-specific** (the same record can bind different file UUIDs on test and production — re-read the soubor path per environment).

| Ident | Type | Properties that make it a probe | Verified on |
| --- | --- | --- | --- |
| `C-202009490A` | akce | pristupnost C, archived (stav=3), restricted element case; carries DJ `-D01` and komponenta `-K001` | landing, OAI, search, handle, file (children), element ladder (2026-09-11: B masked / C full) |
| `M-200500013A` | archeologicky_zaznam | pristupnost B — element ladder one step up | landing, element ladder |
| `C-202204147A` | akce | unarchived → anonymous 401 case | landing (B/C/D 200 — 2026-09-11: B/C re-verified) |
| `C-TX-193001369` | dokument | unarchived → anonymous 401; carries dokument_cast `-D001` | landing, children (2026-09-11: B re-verified) |
| `M-TX-202300441` | dokument | pr=C, stav=3; file `3a0f7078-e26f-404d-aaa9-2b5abbb5e3d2` (real PNG; soubor id `soub-607938` for `/img` probes — the uuid binding drifted to the `"neni"` placeholder mid-pass 2026-09-10, see file-grid notes) | file API all endpoints, `/img` |
| `M-TX-202100125` | dokument | pr=C, stav=3 — restricted file sample; test file `3a0f7078-e26f-404d-aaa9-2b5abbb5e3d2`, production file `fe377f16-1da6-4912-b8d4-6f3095324879` (soub-340129 — the soubor id is the same in both environments) | file API, `/pdf`, `/thumb-large` (2026-09-17: test variant matrix + `/pdf` 401 re-verified; production `/pdf` last observed 2026-09-15 — 200 with a real 115,946 B page JPEG, D14 live leak) |
| `ADB-PRAH71-000861` | adb | restricted (stav≠3), pr=C; carries VB `-V0001` | landing, OAI, search, handle, children |
| `ADB-BERO01-000001-V0001` | vyskovy_bod | production sample of a restricted-children VB with full unsuffixed geometry (EPSG:5514) | production search sweep (2026-09-11, D07) |
| `P-2213-100119` | pian | pr=C (HES-000867) — chranene geometry probe; exact coordinates in the raw Fedora XML | search, geometrie, `/exp` mapa, `/fedora` (D13) |
| `C-202210658` | projekt | **drifted to pr=A (2026-09-11)** — no longer a restricted-projekt sample on test; restricted-projekt candidates come from the sweep recipe (38 on test 2026-09-11); the production D12 sample is `M-202400606` | landing, OAI, search |
| `C-202402033` | projekt | **deleted (2026-09-23)** — tombstone (410) case: landing and handle 410 for all roles, OAI `status="deleted"` | search |
| `C-201122587` | projekt | stav=6 with oznamovatel (PII masking case) | OAI oznamovatel |
| `C-202600010` | projekt | stav=3, organizace ORG-000210 (other than the test accounts'); file `342b7b35-fdc2-4fc7-b2df-bb3c0eecc524` (projekt file row) | file API (2026-09-23: C 200 ✗D15 — other organisation's projekt file served) |
| `C-202101848` | projekt | stav=6, organizace ORG-000030; PDF file `06fbe1a2-62cc-4d5b-9674-abbd8c9754a4` — D15 A/B probe | file API (2026-09-23: anon/B 200 ✗D15) |
| `M-200500013` | projekt | pr=B, stav=6 — D12 stav=6 sample | search, handle (2026-09-23: unsuffixed `projekt_chranene_udaje` anonymously ✗D12; OAI masked) |
| `C-202600009-N00014` | samostatny_nalez | pr=C, stav=1, **created by the B test account** (owner clause); file `c74d3136-3cab-468a-8405-3f79b89ab555`; carries komponenta `-K001` | landing, OAI, file, children, backend (2026-09-17: B-owner landing 200, OAI 403 body ✗D08 — owner clause still dead on OAI; file anon 403 / B-owner 200 — SN01 clause works) |
| `C-202600010-N00085` | samostatny_nalez | pr=C, stav=4 (public-archived SN); files `cafc9a90-3b8b-40f8-ace6-5791b0b6ffd2` (soub-653685), `e5115f1a-72ce-4eaa-a2d9-f52544aaa370` (soub-653686) | landing, OAI, handle, file, backend (2026-09-17: anon/B landing 200 with masked chranene ✓D06 holds; file anon 403 ✗D09 / B 403 ✗D09 / C 200 / D 200 — the gate is now uniform but stricter than the documented A/B rows) |
| `C-202500044-N00001` | samostatny_nalez | pr=A, stav=4 — the deployed file-gate A-row sample (`pristupnost=A AND stav=4` passes); file `04d822bb-2706-430e-9ef2-720f8dcb5b8a` | file API (2026-09-17: anon orig 200 — the A row as the deployed gate reads it) |
| `M-202301371-N00006` | samostatny_nalez | pr=D, stav=4; projekt, `predano_organizace` and SN01 author all ORG-000066; file `0449f273-2436-457e-b118-c1067062eb58` — D09 C-row probe | file API (2026-09-23: anon/B/C 403 ✗D09, D 200) |
| `C-202009779-N00022` | samostatny_nalez | pr=D, stav=1, maintainer-provided (2026-09-15) creator-org clause probe — both arms match: `predano_organizace` ORG-000091, projekt `C-202009779` org ORG-000091; created SN01 by U-001975 | landing, OAI (2026-09-17: anon 401/403 body; C OAI 200 ✓ with chranene masked — org clause works on OAI now; D 200 full) |
| `C-202211308-N00230` | samostatny_nalez | pr=D, stav=1, maintainer-provided (2026-09-15) creator-org clause probe — projekt arm only: `predano_organizace` ORG-000099 (Archaia Brno, mismatch), projekt `C-202211308` org ORG-000091 (match); created SN01 by U-004219 | landing, OAI (2026-09-17: anon 401/403 body; C OAI 200 ✓ with chranene masked — projekt-arm org clause works on OAI now; D 200 full) |
| `C-202204159-N00003` | samostatny_nalez | **drifted (2026-09-23)** — no longer deleted: stav=1 on test (D 200, anonymous 401); was the 410/tombstone case — use `C-202402033` or recipe 5 | landing, handle, OAI tombstone |
| `U-004495` | uzivatel | the B test account — own-record clause | OAI (2026-09-15: own-record B 200 with full user record, C-other 403 body, anon 403 body — harvest lag resolved) |
| `U-004496` | uzivatel | the C test account (org ORG-000091) — counterpart for the creator-org clause probes | session/islogged, landing, OAI (2026-09-15) |
| `M-202400606` | projekt | production D12 sample — pr=C, stav=4, no chranene block served anonymously | production search (2026-09-15: fixed) |
| `M-TX-202600601` | dokument | production D14 sample — restricted, soubor id `soub-847150` | production `/pdf` (2026-09-15: 200 with a real 55,523 B page JPEG anonymously) |
| `M-202500301` | projekt | production stav=6 projekt with a file — D15 production control | production file API (2026-09-23: anon 403 — published row holds) |
| `M-202600319-N00016` | samostatny_nalez | production pr=D, stav=4 find with a file — D09 production control | production file API (2026-09-23: anon 200 — published row holds) |
| let / heslo / ruian / organizace / osoba samples | open types | any state-matching record served to anonymous | OAI (re-derive via recipes) |

Test accounts (role B/C/D): `badatel.ai@arup.cas.cz`, `archeo.ai@arup.cas.cz`, `archivar.ai@arup.cas.cz` — passwords are **operator-provided per session and never recorded in this report** (2026-09-10 operator correction; the credential previously written here was removed and must not be reintroduced). Sessions expire ~30 min.

### Discovery recipes

Do not rely on registry anchors without re-verification; re-discover per session when needed. All steps work anonymously unless stated.

1. **Enumerate and classify records:** `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` (100 records/page, resumptionToken pagination). Classify each record by regex on the body: whole-record restriction `(?s)>\s*HTTP/1\.1 403 Forbidden\s*</amcr:amcr>`; element restriction `<amcr:chranene_udaje>HTTP/1\.1 403 Forbidden`; deleted header `status="deleted"`. Visible records expose `<amcr:stav>` and `<amcr:pristupnost … id="HES-…">` (id → A/B/C/D) for precise classification. `from`/`until` datestamp windows slice the stream.
2. **Predict 401 vs 403:** `GET /api/search/query?entity=<e>&q=ident_cely:"<ident>"` — `numFound: 0` while the record exists in OAI ⇒ not searchable ⇒ anonymous landing returns **401**; `numFound > 0` while OAI shows the record restricted ⇒ **403**.
3. **Visible baselines / element ladder:** from the same OAI scan, take records with the target `stav` and desired pristupnost level (A = open baseline; B/C/D = element hidden up to that role). Direct: `GET /api/search/query?entity=akce&q=pristupnost:<A-E>&rows=5`.
4. **404 candidates:** fabricate idents with valid prefixes but nonexistent numbers (e.g. bump an existing ident's numeric tail far out of range). Expect 404 for all roles.
5. **410 candidates:** scan sets for `status="deleted"` headers — a fresh deletion tops the set listing (tombstone datestamp = deletion time). Deleted **core-type** docs stay in the entities index with `is_deleted=true` and return **410** on landing and handle for every role; deleted `uzivatel`/`osoba` ids 404 (never served on landing by design). Fastest route: maintainer-controlled deletion of a test record, then re-query the set listing.
6. **File-API candidates:** file UUIDs come from the OAI `soubor` elements of **visible** records (`<amcr:url>`, `<amcr:path>`; some records have `soubor` without a Fedora path — skip those). The legacy `/api/search/id` endpoint is removed in the 2026-09-15 builds — do not use it; read file elements from OAI or the search response. Coverage per rule row: dokument and SN files plentiful; projekt files sparse — file a fresh oznámení in webamcr-test if needed. For `/img` and `/pdf` probes, the soubor id and the record ident (resp. page numbers) drive the endpoint. On the search side, `soubor_filepath:rest*` excludes the `"neni"` placeholder files; production search does not project `soubor_filepath` for projekt — read the path from the anonymous OAI record of a stav=6 projekt.
7. **Child-record candidates:** derive from parent metadata — OAI `archeologicky_zaznam` records list `dokumentacni_jednotka` idents; `adb` records list `vyskovy_bod` idents; cast/komponenta idents follow the fixed suffix patterns. `/api/search/query?entity=komponenta|vyskovy_bod` (with an ident filter) confirms existence (DJ and dokument_cast entity queries are stubs).
8. **Cross-environment comparison:** run the same probes against production; data drifts — never assume the same ident has the same state in both environments. Production probing needs explicit operator approval.
9. **Restricted-children search sweep:** `GET /api/search/query?entity=vyskovy_bod&q=-stav:3&rows=0` (add `AND -pristupnost:A` for elevated children) — a non-zero `numFound` counts children of restricted parents that remain anonymously searchable with unsuffixed fields. Same shape for other child entities where `searchable` is not parent-derived; for komponenta/projekt add `q=...` restricted variants and inspect the chranene blocks — for projekt, sweep `-pristupnost:A AND stav:6` separately (the anonymously visible population, see architecture facts).
10. **Role sessions:** `curl -s -D - "https://digiarchiv-test.aiscr.cz/user/login?user=<email>&pwd=<pwd>" -o NUL` captures `Set-Cookie: JSESSIONID=…`; verify with `/user/islogged?wantsUser=true` replaying the cookie; replay on any surface for role views. OAI also accepts Basic auth directly. On a Windows allowlisted shell, put the URL first and the flags after (`curl.exe "https://…/id/<IDENT>" -H "Cookie: JSESSIONID=<SID>" -o NUL -w "%{http_code}\n"`) — flag-first variants may not match the permitted shapes.
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
# legacy id lookup (removed in the 2026-09-15 builds — 500 no-enum, no data; kept for the historical pass shapes)
curl "https://digiarchiv-test.aiscr.cz/api/search/id/<IDENT>"
# geometry actions (live-probe candidates)
curl "https://digiarchiv-test.aiscr.cz/api/search/pians?entity=<ENTITY>"
curl "https://digiarchiv-test.aiscr.cz/api/search/geometrie?loc_rpt=<lat,lng>"
# export (csv/xlsx/xml/json; mapa=true serves geometry)
curl "https://digiarchiv-test.aiscr.cz/exp?entity=<ENTITY>&format=json&mapa=true&geometrie=GeoJSON"
# image endpoints by soubor id
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/img/thumb?id=<SOUBOR_ID>"
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/img/thumb-large?id=<SOUBOR_ID>"
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/img/full?id=<SOUBOR_ID>"
# PDF page images (gated via ImageAccess on the 2026-09-11 test build)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/pdf?id=<SOUBOR_ID>&page=1"
# admin/integration surfaces (expect 403 anonymously)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/fedora/request"
# role-view on any surface (see recipe 10 for login)
curl -s -o /dev/null -w "%{http_code}\n" -H "Cookie: JSESSIONID=<SID>" https://digiarchiv-test.aiscr.cz/id/<IDENT>
# File API (>= 1s between requests; Basic auth for role tests)
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb/page/1"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb-large"
```

## Current verification (2026-09-23 regression pass on the test deploy; production re-probed anonymously with operator approval)

Grid legend: `✓` observed and matches the documented rule · `✗` + finding id = deviation · `·` = no surface for that type by design · `—` = untested on this build (worklist). Codes are the observed HTTP status; element state in parentheses.

**Provenance note:** the 2026-09-23 pass re-probed with fresh B/C/D sessions (operator-provided per-session credentials, not recorded here) and anonymous probes on test — the D08/D09 open cells, the D15/D16 candidates, the #370 checklist codes, and the D06/D07/D10/D11/D12/D13/D14 holdouts — and probed production anonymously (operator-approved) for D07, D09, D10, D12, D14 and D15. Cells marked (2026-09-23) are this pass's observations; every other cell carries its earlier observation (dated, or 2026-09-17 where undated) and was **not re-probed** this pass.

**Corrections recorded this pass:** (1) the 2026-09-17 File API grid recorded `projekt stav=3` (`C-202600010`) C-other-organisation → 403 ✓; this pass observes **200** on the same record and file — the earlier cell is superseded and the behaviour is recorded as D15. (2) D12 was carried as fixed on test since 2026-09-11 on the strength of a 38-projekt sweep; that sweep did not include the restricted stav=6 population, which this pass finds served unsuffixed — D12 is reopened (changed), and whether this is a regression or was never covered cannot be told from the record. (3) The registry's deleted-SN anchor has drifted back to a live stav=1 record; the 410 case now rests on `C-202402033`.

**Correction recorded 2026-09-17:** the run's first reading of the unified file gate was "D09 fixed — the per-role rules now apply uniformly regardless of authentication"; fetching the published File API rule table (<https://arup-cas.github.io/aiscr-api-home/file-api/>) then showed the SN A/B rows carry no pristupnost conditions, so the unified-but-stricter gate is a deviation, not a fix — the operator confirmed 2026-09-17 it is a defect, not desired behaviour. The original conclusion is preserved here per the corrections policy.

### 2026-09-23 pass summary — defect outcomes

| Defect | Outcome on test | Production posture |
| --- | --- | --- |
| D06 | **still fixed** — public stav=4 SN landing anon 200, B 200 (2026-09-23) | not examined — pre-fix-wave build |
| D07 | **still fixed** — 8 restricted VBs, no geometry fields anonymously (2026-09-23) | **open — deferred**: 11,533 restricted-children VB docs, geometry served in all 8 sampled (2026-09-23) |
| D08 | **still open** — B-owner clause dead on OAI (`C-202600009-N00014`: B 403 body on OAI, landing 200); the organisation clauses work on OAI (C record served for both org-match probes and for the B-owned SN via its projekt organisation) (2026-09-23) | not examined |
| D09 | **still open — extended to the C row**: anon/B 403 on a stav=4 pr=C find file; anon/B/**C** 403 on a stav=4 pr=D find file whose organisation arms are all other (2026-09-23) | **absent** — anon 200 on a stav=4 pr=D find file (2026-09-23); the published rule holds on the old build |
| D10 | **still fixed** — variant matrix conforms anonymously (2026-09-23) | endpoint absent (404, 2026-09-23) |
| D11 | **still fixed** — komponenta not served anonymously (2026-09-23) | not examined |
| D12 | **reopened — changed**: restricted stav=6 projekts served with unsuffixed `projekt_chranene_udaje` (geometry, lokalizace, parcelni_cislo) to anonymous on search (23 of 40 sampled restricted projekts — all the stav=6 ones) and handle; OAI masks it; non-stav-6 restricted projekts masked (2026-09-23) | **absent** — 0 of 40 restricted stav=6 projekts carry the block anonymously (2026-09-23) |
| D13 | closed — `/fedora/request` 500 no-enum from this position (2026-09-23) | not examined |
| D14 | **still fixed** — `/pdf` 401 anonymously (2026-09-23) | **open — live leak, deferred**: real 55,523 B page JPEG of a restricted dokument served anonymously (2026-09-23) |
| D15 | **new — confirmed defect** (operator 2026-09-23: the published row is intended): anon/B download a stav=6 projekt's file (6.7 MB PDF); C downloads another organisation's stav=3 projekt file (2026-09-23) | **absent** — anon 403 on a stav=6 projekt file (2026-09-23); introduced by `a0bb69f2` in the fix wave |
| D16 | **candidate** — the C row's `pristupnost` condition is confirmed and folded into D09; the SN01 author's-organisation arm could not be isolated (all test accounts share one organisation) | not examined |

**Issue #370 checklist mapping (2026-09-23):**

1. *Non-archived records visible after login (landing only)* — ✓ `C-202204147A` and `C-TX-193001369`: anon 401, B 200 (2026-09-23).
2. *Same conditions as the API (#237)* — ✓ at record level with open deviations: D08 (OAI B-owner clause), D09 (file-gate SN rows), D12 (restricted stav=6 projekt blocks on search/handle), D15 (projekt file gate).
3. *404/410/401/403 distinction* — ✓ anonymous (2026-09-23): fabricated ident 404; deleted projekt `C-202402033` 410 (also for C, and for D on handle); unarchived akce/dokument 401; DJ of an unarchived akce (`C-202204147A-D01`) and VB of a restricted ADB 403.
4. *Correct return codes* — ✓ on the test build across the probed surfaces (2026-09-23); production residuals D07 and D14 remain until the fix wave is deployed there — and D15 must be fixed before it is.

### Landing pages (`/id/`, incl. `/map/` variants)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C stav=3 (`C-202009490A`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| akce unarchived (`C-202204147A`) | 401 ✓ (2026-09-23) | 200 ✓ (2026-09-23) | 200 ✓ (2026-09-11) | 200 ✓ |
| AZ pr=B (`M-200500013A`) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) | 200 ✓ |
| dokument stav=3 pr=C | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument unarchived (`C-TX-193001369`) | 401 ✓ (2026-09-23) | 200 ✓ (2026-09-23) | 200 ✓ | 200 ✓ |
| adb restricted (`ADB-PRAH71-000861`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| ext_zdroj stav=1 (`BIB-0000052`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| pian state-matching | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| let (any state) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=6 | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=2 (`C-202210658`) | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| SN pr=C stav=4 (`C-202600010-N00085`) | 200 ✓ (masked; 2026-09-23) | 200 ✓ (masked; 2026-09-23) | 200 ✓ (2026-09-11) | 200 ✓ |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) | 401 ✓ (2026-09-23) | 200 ✓ (owner; 2026-09-23) | 200 ✓ (2026-09-11) | 200 ✓ (2026-09-11) |
| SN pr=D stav=1 org-match (`C-202009779-N00022`) | 401 ✓ (2026-09-15) | — | 200 ✓ (org match; 2026-09-15 — creator-org clause works on the entities path) | 200 ✓ |
| SN pr=D stav=1 projekt-org match (`C-202211308-N00230`) | 401 ✓ (2026-09-15) | — | 200 ✓ (projekt-arm org match, `predano_organizace` mismatches; 2026-09-15) | 200 ✓ |
| uzivatel, heslo, ruian_kraj/okres/katastr, organizace, osoba | · 404 | · 404 | · 404 | · 404 |
| DJ of restricted akce (`…-D01`) | 200 ✓ (inherited B+) | 200 ✓ | 200 ✓ | 200 ✓ |
| DJ of unarchived akce (`C-202204147A-D01`) | 403 ✓ (2026-09-23) | — | — | — |
| dokument_cast unarchived (`…-D001`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| VB of restricted ADB (`…-V0001`) | 403 ✓ (2026-09-23) | 200 ✓ | 200 ✓ | 200 ✓ |
| komponenta (SN `…-K001`) | 200 ✓ (masked; 2026-09-11) | 200 ✓ (masked) | 200 ✓ (pr-legitimate content) | 200 ✓ |
| deleted projekt (`C-202402033`) | 410 ✓ (2026-09-23) | — | 410 ✓ (2026-09-23) | 410 ✓ (handle, 2026-09-23) |
| fabricated ident | 404 ✓ (2026-09-23) | 404 ✓ | 404 ✓ | 404 ✓ |

Notes: `/map/<ident>` shares the gate (verified with the SN matrix 2026-09-11); bare `/map` 200 since the fix wave (D01 fixed). Landing pages are SPA shells — status codes are gates; the element masking cells reflect the handle API data underneath (see handle grid).

### OAI-PMH (GetRecord, `oai_amcr`)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| projekt stav=6 (oznamovatel case, `C-201122587`) | 200 ✓ (chranene/oznamovatel masked) | 200 ✓ (masked) | 200 ✓ (oznamovatel masked: stav≠1, other org) | 200 ✓ (full incl. oznamovatel) |
| projekt stav=2 | 403 body ✓ | 403 body ✓ | 200 ✓ (full) | 200 ✓ (full) |
| akce pr=C stav=3 | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| akce state-mismatched | 403 body ✓ | 200 ✓ | 200 ✓ | 200 ✓ (full) |
| adb restricted | 403 body ✓ | 200 ✓ | 200 ✓ | 200 ✓ (full, chranene) |
| adb state-matching pr=A | 200 ✓ | — | — | — |
| dokument pr=C stav=3 | 200 ✓ (no chranene element by spec) | 200 ✓ | 200 ✓ | 200 ✓ |
| ext_zdroj stav=1 | 403 body ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| pian state-matching pr=A | 200 ✓ (chranene visible) | — | — | — |
| let, heslo, ruian_kraj, organizace, osoba | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| uzivatel (other's record) | 403 body ✓ | 403 body ✓ | 403 body ✓ | 200 ✓ (PII) |
| uzivatel own-record clause | 403 body ✓ (2026-09-15) | 200 ✓ full own record (2026-09-15 — harvest lag resolved) | 403 body ✓ other's record (2026-09-15) | — |
| SN pr=C stav=1 B-owned | 403 body ✓ (2026-09-23) | 403 body ✗D08 (owner clause dead on OAI; 2026-09-23) | 200 ✓ (projekt organisation matches C's — rule-conformant; 2026-09-23) | 200 ✓ (2026-09-23) |
| SN pr=C stav=4 public | 200 ✓ (masked chranene; 2026-09-15 — fixed) | 200 ✓ (masked chranene; 2026-09-15 — fixed) | 200 ✓ (full; 2026-09-15 — re-verified) | 200 ✓ (full; 2026-09-11) |
| SN pr=D stav=1 org-match (`C-202009779-N00022`, both arms match) | 403 body ✓ | — | 200 ✓ (chranene masked per pr=D; 2026-09-23 — org clause works on OAI) | 200 ✓ (full; 2026-09-15) |
| SN pr=D stav=1 projekt-org match (`C-202211308-N00230`, predano arm mismatches) | 403 body ✓ | — | 200 ✓ (chranene masked per pr=D; 2026-09-23 — projekt-arm org clause works on OAI) | 200 ✓ (full; 2026-09-15) |
| deleted SN | tombstone ✓ | tombstone ✓ | tombstone ✓ | tombstone ✓ |

Notes: `oai_dc` is derived by XSLT after filtering — no leak (verified on pr=C akce); ListRecords carries tombstones and per-record 403 bodies like ListIdentifiers/GetRecord; versioned endpoints differ by XSLT only; Basic auth works on the OAI endpoint, including the version-less `/oai` form via redirect (`-L`); identifiers must use the environment's own domain prefix.

### Search API (`/api/search/query`)

Visibility of restricted records in anonymous results is by design (public fields, masked protected fields). The grid records **element masking** on the restricted probe records.

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ no chranene block | ✓ no chranene block | ✓ full chranene block | ✓ full chranene block |
| adb restricted (`ADB-PRAH71-000861`) | ✓ block not served | ✓ block not served | ✓ block not served | ✓ block not served (chranene not projected on the search path for any role — suffixed-alias projection) |
| SN pr=C stav=4 | ✓ no chranene block | ✓ no chranene block (lokalizace masked; soubor metadata public by design) | ✓ full chranene block | ✓ full chranene block |
| dokument pr=C | ✓ (no protected blocks by spec) | ✓ | ✓ | ✓ |
| ext_zdroj stav=1 | ✓ (no protected fields) | ✓ | ✓ | ✓ |
| pian pr=C (`P-2213-100119`) | ✓ block not served | ✓ | ✓ | ✓ (chranene not projected on the search path for any role — the geometry exposure routes are the map/geometry actions, see D07/VB) |
| projekt restricted, stav≠6 (sweep — 38 on test) | ✓ no chranene block (2026-09-23) | ✓ masked | ✓ block served — pr-legitimate | ✓ block served — pr-legitimate |
| projekt restricted, stav=6 (sweep, `M-200500013`) | ✗D12 unsuffixed `projekt_chranene_udaje` served (23 of 40 sampled restricted projekts; 2026-09-23) | — | — | — |
| komponenta (SN `…-K001`) | ✓ not served anonymously (2026-09-23) | ✓ masked | ✓ pr-legitimate (C ≥ record pr) | — |
| oznamovatel (any type) | ✓ not served by any backend surface | ✓ | ✓ | ✓ |
| uzivatel/heslo/ruian/organizace/osoba | · (search queries the entities core only) | · | · | · |

Notes: legacy `/api/search/id` **action removed** in the 2026-09-15 builds (500 no-enum, no data served; re-observed 2026-09-17); `entity=dokumentacni_jednotka`/`dokument_cast` are pre-existing stubs; `D07` on test — anonymous `entity=vyskovy_bod&q=-stav:3` serves no geometry fields (re-verified 2026-09-17); production not examined 2026-09-17 — last observed 2026-09-15 returning 11,530 restricted-children docs with unsuffixed geometry. Search-side stats note (2026-09-15): the restricted-projekt sweep's `lat_A` stats field counts 5 populated docs, but no served field list projects coordinates or katastr for them — the aggregate min/max is the only anonymous-visible signal (coarse, country-scale); likely pian-derived, unconfirmed.

### Handle API (`/api/search/handle?id=`)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ error/masked | ✓ masked (2026-09-11: no az/akce chranene, no loc_rpt) | ✓ full (2026-09-11: chranene + pian geometry) | ✓ full |
| SN pr=C stav=4 | 200 ✓ (2026-09-15 — fixed) | — | 200 ✓ (2026-09-11) | 200 ✓ |
| SN pr=C stav=1 B-owned | 401 ✓ | 200 ✓ (owner; 2026-09-11: chranene masked, suffixed `_B` fields empty) | 200 ✓ | 200 ✓ |
| deleted SN | 410 ✓ | 410 ✓ | 410 ✓ | 410 ✓ |
| DJ/dokument_cast/VB (restricted/unarchived parents) | ✓ matching codes, no data | ✓ | ✓ | ✓ |
| komponenta (SN `…-K001`) | ✓ no chranene block — **fixed 2026-09-11** | ✓ masked | ✓ 200 (pr-legitimate: C ≥ record pr) | — |
| projekt stav=2 | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| projekt restricted stav=6 (`M-200500013`, pr=B) | ✗D12 unsuffixed `projekt_chranene_udaje` served (2026-09-23) | — | — | — |

### File API (`/id/<ident>/file/<uuid>` and variants)

| Rule row / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| dokument pr=C stav=3 (`M-TX-202300441`, `M-TX-202100125`) — orig | 403 ✓ (2026-09-23) | 403 ✓ | 200 ✓ | — |
| └ `/thumb` | 200 ✓ (documented ungated; 2026-09-23) | — | — | — |
| └ `/thumb/page/1` | 403 ✓ (2026-09-23) | — | — | — |
| └ `/thumb-large` | 403 ✓ — **still fixed 2026-09-23** (D10) | — | — | — |
| └ `/paradata` | 403 ✓ (2026-09-23) | — | — | — |
| SN pr=C stav=4 (`C-202600010-N00085`) — orig | 403 ✗D09 (2026-09-23) | 403 ✗D09 (2026-09-23) | 200 ✓ (2026-09-23) | 200 ✓ (2026-09-23) |
| SN pr=D stav=4, all organisation arms other (`M-202301371-N00006`) — orig | 403 ✗D09 (2026-09-23) | 403 ✗D09 (2026-09-23) | 403 ✗D09 — C row, published `stav=4` (2026-09-23) | 200 ✓ (2026-09-23) |
| └ `/thumb/page/1` | 403 ✗D09 (2026-09-17 — follows orig now) | — | — | — |
| SN pr=A stav=4 (`C-202500044-N00001`) — orig | 200 ✓ (2026-09-17 — the deployed A row `pristupnost=A AND stav=4` passes) | — | — | — |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) — orig | 403 ✓ (2026-09-17 — re-verified) | 200 ✓ (SN01 owner; 2026-09-17 — still works) | 200 ✓ | 200 ✓ |
| └ `/thumb-large` | 403 ✓ — fixed with D10 (was 200 ✗D10) | — | — | — |
| projekt stav=3, other organisation (`C-202600010`) — orig | 403 ✓ (2026-09-23) | 403 ✓ (never; 2026-09-23) | 200 ✗D15 (2026-09-23; was 403 ✓ on 2026-09-17 — superseded) | 200 ✓ (2026-09-23) |
| projekt stav=6, other organisation (`C-202101848`) — orig | 200 ✗D15 (6.7 MB PDF; 2026-09-23) | 200 ✗D15 (2026-09-23) | 200 ✗D15 (published: own organisation required; 2026-09-23) | 200 ✓ (2026-09-23) |
| └ `/thumb` | 200 ✓ | — | — | — |
| cross-record uuid binding | 404 ✓ | — | — | — |
| rate limiter | 429 `Retry-After: 0` ✓ (interval branch; re-observed 2026-09-11 on a burst, then clean 403 on the isolated re-probe) | — | — | — |

Notes: Basic auth accepted on the file path but **not** on the landing `checkId` path; landing-path requests under Basic auth are treated as anonymous; concurrent branch (`Retry-After: -1`) code-verified only. Test-data drift: the `M-TX-202300441` file uuid's `soubor_filepath` drifted to the `"neni"` placeholder mid-pass 2026-09-10 (recheck returns 404 for every role, including D) — the anon/B/C cells above were verified before the drift; the D cell is untestable on this record until the file binding is restored. The `/pdf` endpoint resolves by **soubor id** on the 2026-09-11 build (see surface inventory).

**#693 test pattern (ATRIUM alternative file distributions — implemented per the 2026-09-10 operator note; milestone v4.1.0).** The File API treats **any suffix** after `/id/{ident_cely}/file/{file_id}/` (except reserved `thumb/page/*`) as an alternative-distribution request (e.g. `/atr/alto-xml`) and serves per-distribution paradata under `/paradata/{distribution}` (`/paradata` bare maps to the `orig` paradata). Stated rule: **distributions and paradata apply the same permission rules as the original file**; small `thumb` stays the only ungated exception.

Verification pattern (per rule row of the File-API table, per role):

1. **Discover a file carrying a distribution:** the file's history must contain `DIST01` (insert) without a later `DIST10` (delete) sharing the same label — read from OAI `soubor/historie` or the search-side history fields; the label names the distribution path segment (e.g. `atr/alto-xml`).
2. **Baselines:** `GET …/file/<uuid>` (orig) and `…/thumb` per role — the documented XPath rule row.
3. **Distribution gate:** `GET …/file/<uuid>/<distribution>` must return the **same verdict as orig** for every role — any role that gets 403/401/404 on orig must get the same on the distribution, and any role that gets 200 on orig may get 200 or 404 (distribution may not exist) — a 200 for a role that is denied on orig is the D10-family defect.
4. **Paradata gate:** `GET …/paradata` and `…/paradata/<distribution>` and `…/paradata/orig` must likewise match the orig verdict per role.
5. **Suffix-shape sweep:** repeat 3 with suffixes containing `thumb` (e.g. a hypothetical distribution named `*thumb*`) — the `isAllowed` `contains("thumb") && !contains("page")` early-return must not make them ungated.

Partial verification 2026-09-10 (test, `M-TX-202100125`, pr=C, file `3a0f7078-…`, **no DIST01 on this file**): orig 403 anonymous, `/thumb` 200, `/paradata` 403, `/paradata/orig` 403 — **paradata paths correctly gated for anonymous**; `/atr/alto-xml` and `/paradata/atr/alto-xml` return 404 for anonymous — no content served, but inconclusive for the distribution gate itself (unknown-distribution resolution happens without a demonstrable permission check; a `DIST01`-carrying file is the worklist — none was found via search facets, which do not expose a distribution field).

### Images and PDF (`/img/*`, `/pdf`)

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/img/thumb?id=<restricted dokument soubor>` | ✓ 200 (documented ungated small thumb) | — | — | — |
| `/img/full?id=<restricted dokument soubor>` | ✓ 401 (ImageAccess gate holds: imgPr > anon) | — | — | — |
| `/img/thumb-large` | n.a. — not a valid action (500 `No enum constant …THUMB-LARGE`; the large-size `writeImg("thumb-large")` lives inside the gated `FULL` action) | · | · | · |
| `/img/full?id=<restricted SN soubor>` | ✓ gate passes anonymous — no 401, SN always allowed (issue #85); serve 500s on this test file (backend path defect, not a gate) | — | — | — |
| `/pdf?id=<restricted dokument soubor id>&page=1` | ✓ 401 — **still fixed 2026-09-23** (D14: `ImageAccess.isAllowed(…, true)` gate; re-verified on `soub-340129`) | — | — | — |
| `/pdf?id=<SN soubor id>&page=1` | ✓ gate passes per #85; cold cache serves the 7,525 B placeholder only — no content leak | — | — | — |
| production `/pdf?id=<restricted dokument>&page=1` | ✗D14 200 with **real page JPEG content** (55,523 B for `soub-847150`; 2026-09-23 — live leak persists) | — | — | — |

Notes: `/img` and the 2026-09-11 `/pdf` gate key on the **soubor id** (`soub-XXXXXX`), not the Fedora uuid — a uuid `id` returns 401 as a not-found, which is indistinguishable from a gate refusal on status alone; verify with the soubor id.

### Export and geometry (`/exp`, `/api/search/gml|wkt|geometrie|pians|mapa|export*`)

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/exp?entity=projekt&q=<restricted ident>` | ✓ no chranene block in the export field set (record discoverable — by-design visibility; export fields are public-only) | — | — | — |
| `/exp?entity=samostatny_nalez&mapa=true&geometrie=GeoJSON` (restricted) | ✓ `numFound=0` — record filtered out entirely for anonymous; no chranene, no error | — | — | — |
| `/exp?entity=komponenta&q=<restricted komponenta>&format=csv` | ✓ no data row — record filtered out (2026-09-11) | ✓ row served, katastr column masked (2026-09-11) | ✓ row served with katastr — pr-legitimate (2026-09-11) | — |
| `/exp?entity=pian&mapa=true` (restricted) | ✗ 500 (unhandled error page for anonymous on a restricted pian; no data leaked — robustness defect, not a leak) | — | — | — |
| `/api/search/geometrie` (restricted pian) | ✓ `{}` at the restricted pian's exact coordinates — the query runs on the role-suffixed `loc_rpt_A` field, which does not match restricted pians (the commented-out pristupnost check in source remains a latent risk if the suffixing is ever dropped) | — | — | — |
| `/api/search/pians` (restricted pian, `q=pristupnost:C`) | ✓ 193 restricted pians served — **idents and pian_id only, no coordinates** (location fields correctly absent) | — | — | — |
| `/api/search/gml`, `/wkt` | — | — | — | — |

### Admin and integration surfaces

| Surface / probe | anon (from this workstation's VPN/local-network position) | B | C | D |
| --- | --- | --- | --- | --- |
| test `/fedora/get_id?id=<restricted projekt>` | n.a. — action removed (`No enum constant …GET_ID` 500; re-observed 2026-09-17 — the request passes the network-trusting gate from this position) | — | — | — |
| test `/fedora/request?url=…` | n.a. — action removed likewise (500 no-enum, 2026-09-15) | — | — | — |
| production `/fedora/get_id?id=C-202210658` | ✓ **fixed 2026-09-15** — the hotfix removed the `GET_ID` action (500 no-enum, Tomcat 11.0.15; no data served) | — | — | — |
| production `/fedora/request?url=record/<ident>` | ✓ **fixed 2026-09-15** — `REQUEST` action removed likewise (500 no-enum; no data served) | — | — | — |
| `/mus/predmety_by_id`, `/mus/statistika` | ✓ reachable, empty test data (no demonstrable leak) | — | — | — |
| `/api/search/stats` | ✓ 200 public aggregates | — | — | — |
| `/fav/*` | ✗ 500 anonymous (ungated servlet but errors without a user; no data served) | — | — | — |
| `/api/search/stats_index` | ✓ 200 anonymous (index aggregates) | — | — | — |

### Findings registry

| ID | Severity | Surface | Status | One-line summary |
| --- | --- | --- | --- | --- |
| D01 | minor | bare `/map` | **fixed** | `/map` 500 → 200 forward to `/map2` |
| D02 | — | SN record rules | **changed** | fix-wave rewrite superseded by D06; owner clause verified live (again 2026-09-11) |
| D03 | High | legacy `/api/search/id` | **fixed — action removed** (2026-09-15: 500 no-enum, no data served) | anonymous leak of non-searchable records → numFound 0 → action removed |
| D04 | minor | file API rate limiter | **closed** (operator decision 2026-09-15: no further live verification; any reported issue is handled reactively) | interval branch live-verified; concurrent branch code-only |
| D05 | minor | child-record landing | **fixed on landing/handle**; search residual = D07 (open on production only) | DJ/dokument_cast/VB inherit parent rules again |
| D06 | High | SN record level | **fixed on test** (2026-09-17: anon/B 200 with chranene masked on the public stav=4 SN, landing re-verified) | the A/B branches' `pristupnost <= userPr` conditions contradicted #237's `stav=4` rule |
| D07 | Medium-High | search API (vyskovy_bod) | **fixed on test** (2026-09-23: re-verified); **open on production — deferred** (2026-09-23: 11,533 restricted-children docs, geometry served) | restricted VB geometry anonymously searchable |
| D08 | High | OAI-PMH (SN) | **open — owner clause** (2026-09-23: B-owner 403 body on OAI, landing 200; organisation clauses work on OAI) | SN OAI owner clause reads fields the oai doc does not resolve |
| D09 | Medium | file API | **open — confirmed defect, extended to the C row** (2026-09-23: anon/B 403 on stav=4 pr=C, anon/B/C 403 on stav=4 pr=D other-organisation find files; production absent) | file-gate SN rows over-block: pristupnost conditions the published rule table does not state |
| D10 | High | file API `/thumb-large` | **fixed on test** (2026-09-17: 403 anonymous re-verified); production never had the endpoint (404; not examined 2026-09-17) | undocumented endpoint bypassed all permission checks |
| D11 | High | backend surfaces (komponenta) | **fixed on test** (2026-09-17: re-verified) | komponenta docs served the SN chranene block unmasked |
| D12 | High | search + handle API (projekt) | **reopened — changed on test** (2026-09-23: restricted stav=6 projekts served with unsuffixed `projekt_chranene_udaje` anonymously; OAI masks it; production absent) | anonymous search/handle serve `projekt_chranene_udaje` for restricted stav=6 projekts |
| D13 | High | `/fedora/*` | **closed — solved in current state** (operator 2026-09-17: read actions removed by hotfix on production and test — test re-observed 2026-09-17; the network-trusting gate posture and the unprobed mutating actions are accepted as-is) | unauthenticated raw-Fedora access from the local network/VPN |
| D14 | High | `/pdf` | **fixed on test** (2026-09-23: 401 anonymous); **open on production — live leak, deferred** (2026-09-23: real 55,523 B page JPEG served anonymously) | PDF page JPEGs served with no permission gate |
| D15 | High | file API (projekt) | **open — confirmed defect** (operator 2026-09-23: the published row is intended; test only — production follows the published row) | projekt file gate applies the record-level rule: anon/B receive stav=6 projekt files, C receives other organisations' projekt files |
| D16 | Low (if confirmed) | file API (SN) | **candidate — not examinable with the test accounts** (2026-09-23: the C-row pristupnost half confirmed and folded into D09) | SN file gate C row adds an SN01 author's-organisation clause the published row does not state |

- **D03 (High, fixed — action removed 2026-09-15):** the 2026-09-11 fix wave first gated the endpoint (numFound 0 for non-searchable records); the 2026-09-15 builds remove the `id` read action from the search servlet entirely — `/api/search/id` now returns the 500 no-enum error and serves no data. No leak path remains on either deployment.
- **D04 (minor, closed by operator decision 2026-09-15):** the interval branch was live-verified (re-observed 2026-09-11) and the concurrent branch (`Retry-After: -1`) remained code-verified only. The operator closed the item: no further live verification will be performed; any rate-limiter issue is handled reactively when reported.
- **D06 (High, fixed on test 2026-09-15):** the A/B branches no longer 403 on public stav=4 SNs — the 2026-09-11 deploy read the doc's `stav`/`pristupnost` in `SamostatnyNalez.filterOAI` (fixing C/D), and the 2026-09-15 build serves the public SN to anonymous and B with `chranene_udaje` masked, on landing, handle, and OAI (`C-202600010-N00085`: anon 200 masked, B 200 masked, C 200 full). The `pristupnost <= userPr` divergence from #237 is gone on the probed surfaces. The creator-organisation clause was exercised 2026-09-15 with maintainer-provided records and works on the entities path (see D08 for the OAI-side residual).
- **D07 (Medium-High; fixed on the test build, open on production — deferred 2026-09-15 by operator decision: the upcoming production release deploys the fix):** on test, the anonymous search response for restricted VBs serves no geometry fields (8 restricted VB docs indexed, countable, no `vyskovy_bod_geom_wkt`/`geom_gml`). On production, `entity=vyskovy_bod&q=-stav:3` still returns 11,530 restricted-children docs and the first doc serves the full unsuffixed geometry (`ADB-BERO01-000001-V0001`, EPSG:5514 `POINT Z (-750588.48 -1042183.7 330.85)`). Fix direction unchanged: parent-derived `searchable` for VB docs and/or pristupnost-suffixed geometry fields.
- **D08 (High, narrowed to the owner clause 2026-09-17):** the C creator-organisation clauses now work on OAI as well — re-verified 2026-09-17 with the maintainer-provided records: `C-202009779-N00022` (pr=D, stav=1, both arms match: `predano_organizace` ORG-000091 and projekt `C-202009779` org ORG-000091) and `C-202211308-N00230` (pr=D, stav=1, projekt arm only: `predano_organizace` ORG-000099 mismatches, projekt `C-202211308` org ORG-000091 matches) — C (U-004496, ORG-000091) receives the record body on OAI for both, with `chranene_udaje` masked per pr=D (rule-conformant). What remains open is the **B-owner clause**: `C-202600009-N00014` as B-owner still gets the 403 body on OAI GetRecord while the landing page serves it (200, re-verified 2026-09-17). The owner arm of the oai-document clause resolution is the residual.
- **D09 (Medium, open — confirmed defect 2026-09-17; extended to the C row 2026-09-23: `M-202301371-N00006`, pr=D stav=4, all organisation arms ORG-000066 — anon/B/C 403, D 200; production absent):** the 2026-09-17 deploy rewrote `HandleServlet.isFileAllowed` to apply per-role XPath rule rows uniformly regardless of authentication — the earlier inconsistency (B 403 while anonymous 200 on a stav=4 SN file) is gone. But the deployed SN rows are stricter than the published File API rule table (<https://arup-cas.github.io/aiscr-api-home/file-api/>): the code's A row is `pristupnost=A AND stav=4` and B row `(pristupnost<=B AND stav=4) OR SN01 owner`, while the published table states `stav=4` alone for A and `stav=4 OR my record` for B (the dokument rows in code and table do match each other). The operator confirmed 2026-09-17 this is a defect, not desired behaviour — the published table is authoritative. Verified live on `C-202600010-N00085` (pr=C, stav=4, file `cafc9a90-3b8b-40f8-ace6-5791b0b6ffd2`): anonymous 403, B 403, C 200, D 200; `/thumb/page/1` anonymous 403 (follows orig now). The SN01 owner clause still works (B-owner 200 on the own stav=1 SN file), and the deployed A row passes its own reading (`C-202500044-N00001`, pr=A, stav=4 — anonymous 200). Impact: 3,397 test SN records are stav=4 with pristupnost>A — their files were anonymously downloadable per the published table (and were on the 2026-09-15 build) and now return 403 to anonymous and B. Fix direction: drop the pristupnost conditions from the SN A/B rows in `isFileAllowed`, restoring the documented rule.
- **D10 (High, fixed on test — re-verified 2026-09-15):** `HandleServlet.isAllowed` no longer lets `thumb-large` through the ungated `contains("thumb") && !contains("page")` branch — the variant matrix on a restricted dokument file (`M-TX-202100125`, pr=C, stav=3) conforms: orig 403, `/thumb` 200 (documented ungated), `/thumb/page/1` 403, `/thumb-large` 403, `/paradata` 403 (all anonymous, re-probed 2026-09-15). Production never carried the endpoint (404).
- **D11 (High, fixed on test — re-verified 2026-09-15):** komponenta docs no longer serve the copied `samostatny_nalez_chranene_udaje` block on any probed surface: search and handle return no block to anonymous, and the CSV export masks per role — anonymous gets no row (record filtered), B gets the row with the chranene-derived katastr column empty, C gets the katastr (pr-legitimate). `Komponenta.filterOAI` still returns true unconditionally in source — the masking happens on the search path; the record-level gate remains the komponenta scenario's subject.
- **D12 (High, reopened — changed on test 2026-09-23):** anonymous search and handle serve the unsuffixed `projekt_chranene_udaje` block — geometry (`geom_wkt`, `geom_gml`), `lokalizace`, `parcelni_cislo`, cadastral fields — for restricted projekts at stav=6 (sample `M-200500013`, pr=B; 23 of 40 sampled restricted projekts carried it, all of them stav=6), while OAI masks the same element. Restricted projekts at other stav are masked. The earlier fixed verdict (2026-09-11, re-verified 2026-09-15/17) rested on a 38-projekt sweep that is exactly the non-stav-6 population. Production shows no such exposure (2026-09-23: 0 of 40). Fix direction: the projekt searcher's post-filter must drop the unsuffixed block whenever record pristupnost exceeds the user's, independent of stav.
- **D13 (High, closed — solved in current state 2026-09-17):** production `/fedora/get_id?id=C-202210658` and `/fedora/request?url=…` returned the 500 no-enum error (`FedoraServlet.Actions.GET_ID`/`REQUEST` no longer exist, Tomcat 11.0.15) — the raw-XML read paths that served full `chranene_udaje` unauthenticated from the trusted position are gone on both deployments (test re-observed 2026-09-17). The operator closed the item 2026-09-17 in the current state: the network-trusting gate posture and the remaining unprobed mutating index-management actions (INDEX_FULL, INDEX_UPDATE, REINDEX_FILTER, …) are accepted as-is; no further verification will be performed.
- **D14 (High; fixed on test — re-verified 2026-09-15; open on production — live leak, deferred 2026-09-15 by operator decision: the upcoming production release deploys the gate):** the test build gates `/pdf` through `ImageAccess.isAllowed(request, true)`, keyed on the **soubor id**: a restricted dokument's page returns 401 to anonymous (re-probed 2026-09-15), and an SN soubor id passes the gate per #85 with the cold cache serving only the placeholder. Production still runs the ungated build **and the page cache has warmed**: `/pdf?id=soub-340129&page=1` serves the real page JPEG (115,946 B) and `soub-847150` (restricted dokument `M-TX-202600601`) likewise (55,523 B), both to anonymous — restricted document content is directly downloadable, not merely a status-code defect. The neighbouring gated surfaces still refuse (`/thumb/page/1` 403, `/img/full` 401) while `/pdf` serves. The production release will deploy the gated build; a page-cache flush there is still advisable.
- **D15 (High, open — confirmed defect 2026-09-23):** since `a0bb69f2` (#370, 2026-09-11) the `projekt` case of `HandleServlet.isFileAllowed` applies the record-level projekt rule (A/B `stav=6`, C `stav>=1`) instead of the published `//projekt/soubor` row (A/B never; C `stav=1` or stav 2–6 of the user's own organisation). On test, anonymous and B download the file of a stav=6 projekt of ORG-000030 (`C-202101848`, a 6.7 MB PDF — content not opened by the run), and C of ORG-000091 downloads the file of a stav=3 projekt of ORG-000210 (`C-202600010`). Projekt attachments can carry personal data of the notifier, so the exposure is treated as High. Production follows the published row (2026-09-23: anonymous 403 on `M-202500301`), so the defect must be fixed before the fix wave is released there.
- **D16 (Low, candidate):** the SN file gate's C row includes an SN01 author's-organisation clause the published row does not state (source reading; the arm predates #370). It could not be isolated live: all test accounts belong to ORG-000091, and no find was available whose author belongs to ORG-000091 while its projekt and receiving organisation do not. The row's `pristupnost` condition, the other half of the original candidate, is confirmed and recorded under D09.

### Pre-existing observations

- The `dokumentacni_jednotka` and `dokument_cast` entity-search endpoints are stubs and return error bodies — pre-existing, unrelated to permissions, no data exposure.
- Test-data note: the `archivar.ai@arup.cas.cz` test account initially reported pristupnost B; the operator corrected it to D mid-run (2026-09-10).
- Quoted-phrase Solr interpolation (code-verified, low): `HandleServlet.getDocument`'s `soubor_filepath:"<url-path>"` filter and OAIRequest's `ident_cely:"<id>"` interpolate the request path/identifier without escaping query metacharacters (`InitServlet.asSafePath` blocks traversal and backslashes only). No privilege bypass found — the permission gate still runs on whatever document the query returns — but the construction is injection-shaped; escaped terms would remove the class.
- The `oai` core lags the `entities` core for freshly created records: a user record created days before the run returns `idDoesNotExist` on OAI while being present in entities — freshness note for harvesters, and the reason the uzivatel my-record clause could not be live-checked for the test accounts.
- Test-data quality: several records carry placeholder file paths (`"neni"`) and omit `soubor/path`+`soubor/url` elements in OAI output; well-formed records (verified via the D view) do serve both. Probes used well-formed records — do not read the omission as build behaviour.
- Environment-specific file bindings (2026-09-11): `M-TX-202100125` binds test file `3a0f7078-…` but production file `fe377f16-…`; always re-read the soubor path from the environment being probed.
- Production index observation (2026-09-15): the production entities core reports 250,496 indexed komponenta docs (the count surface answers), but `entity=komponenta` queries error — the 2026-09-11 komponenta fix verification is test-side; production komponenta behaviour remains unprobed and is not inferred from the count.
- Production `/thumb-large` 404 (2026-09-15): the old build carries neither the endpoint nor the fix wave — do not read the 404 as the D10 gate working there.

## Verification log

| Date | Pass | Scope / outcome |
| --- | --- | --- |
| 2026-08-28 | initial | point-in-time report consolidated into this scenario when the corpus moved to this repository |
| 2026-09-10 | 1 — regression | D01/D03 fixed; D05 fixed on landing (search residual D07); D06 introduced; D02 changed; #370 items verified with D06/D07 exceptions; credentialed recipes added |
| 2026-09-10 | 2 — completion | 429 interval + 410 live-verified; OAI Basic auth + SN role comparison (D08); file API SN matrix (D09) |
| 2026-09-10 | 3 — coverage | record-type matrix for OAI/landing/file (D10); oznamovatel; oai_dc; versioned endpoints; landing no-surface types |
| 2026-09-10 | 4 — children + backend | child-record matrix (D11); backend search/handle/legacy on komponenta |
| 2026-09-10 | 5 — backend matrix | anonymous search for all restricted kinds; suffix mechanism verified on akce; **D12** (projekt chranene in search) |
| 2026-09-10 | 6 — source audit | full route/gate enumeration from `web.xml` + servlets; record refactored into coverage grids; new surfaces identified for the full pass (`/pdf`, `/img`, `/exp`, geometry actions, `/fedora`, `/mus`) |
| 2026-09-10 | 7 — full grid pass | new-surface anonymous probes + role fills: **D13 found** (anonymous raw-Fedora access with full chranene in raw XML); **D14 found** (`/pdf` has no gate; cold cache blocks the live demo); D12 extended to B, C/D cells filled (block served pr-legitimately — defect confined to below-pr leakage); `/img` re-verified with soubor ids (uuid-keyed 401s are not-found, not gate refusals): thumb ungated, full gated, SN gate passes anonymous (issue #85) with a 500 serve defect on the test file; `/exp` projekt serves no chranene (export fields public-only), SN map `numFound=0`, pian map 500 (robustness); `geometrie` `{}` on restricted coords (suffixed query masks; commented-out check stays latent); `/api/search/pians` serves restricted-pian idents without coordinates; OAI D-column and B/C open-type cells filled; adb/pian chranene never projected on the search path (any role); komponenta handle/landing 200 for C/D (pr-legitimate); file-grid dokument uuid drifted to the `"neni"` placeholder mid-pass (D cell untestable); own-record uzivatel clause still harvest-lagged |
| 2026-09-10 | 8 — production probes | operator-directed anonymous production checks of the open leaks: **D13 verified on production, then reclassified to local-network/VPN scope after operator verification** (the gate trusts the network position; public-internet anonymous denied; from VPN the raw XML of restricted projekt `M-202400606` was served unauthenticated); **D12 active on production from the public internet** (58 restricted projekts, chranene served anonymously); D07 previously confirmed active; **D14 reachable but placeholder-only** (`/pdf` 200 with the 7,525 B `empty_big.png` for every probed dokument — restricted, public, old, fresh); **D10 absent on production** (`/thumb-large` 404 — fix-wave endpoint); **D11 absent on production** (no komponenta docs in the old build's entities core). #693 implemented per operator note — test pattern recorded in the file-API notes with partial verification (paradata gated 403 anonymous on `M-TX-202100125`; distribution gate open pending a `DIST01`-carrying file); test-account password removed from this record per operator correction; production repro URLs de-linked (host + path patterns only) per operator correction |
| 2026-09-11 | 9 — #370 regression re-verification | fresh role sessions (B/C/D) and anonymous/OAI probes on the 2026-09-11 test deploy: **D06 narrowed** (C/D fixed via doc-derived stav — landing and OAI 200 on the public SN; anon/B still 403 — the `pristupnost<=userPr` divergence from #237 remains); **D08 narrowed** (C/D served on OAI; B-owner clause still 403 there); **D09 re-verified open** (B 403 on the stav=4 SN file, anon 200, B-owner 200); **D10 fixed** (thumb-large 403 anonymous, full variant matrix conforms); **D11 fixed** (search/handle/legacy/export: anonymous filtered/masked, B masked, C pr-legitimate — komponenta CSV export katastr ladder verified); **D12 fixed on test and production** (restricted-projekt sweep clean; `M-202400606` masked on prod; anchor `C-202210658` drifted to pr=A — replaced by the sweep); **D13 posture unchanged** (test build removed GET_ID but the network-trusting gate still passes this position; production GET_ID still serves full raw XML incl. chranene from the trusted position); **D14 fixed on test** (`/pdf` 401 anonymous on a restricted dokument via the ImageAccess gate keyed on the soubor id; SN passes the gate per #85, placeholder only), open on production (200 + placeholder). #370 checklist: items 1, 3, 4 verified; item 2 partial (D06 A/B divergence). Issue points 1 role cells re-verified (`C-202204147A` B/C 200; `C-TX-193001369` B 200); element ladder re-verified on `C-202009490A` (B masked / C full); 404/410/401/403 anonymous distinction re-verified; 429 limiter re-observed on a burst. Anchor registry updated (drifted projekt anchor, SN file UUIDs, production VB sample, environment-specific file bindings) |
| 2026-09-15 | 10 — #370 regression re-verification | anonymous + B/C sessions on the 2026-09-15 test build, anonymous production probes: **D06 fixed on test** (anon/B 200 with chranene masked on the public stav=4 SN — landing, handle, OAI); **D08 narrowed further** (B-owner clause still dead on OAI; the anon/B public-SN arm resolved by the D06 fix; uzivatel own-record clause now verified working — the oai-core harvest lag resolved); **D09 re-verified open** (B 403 on the stav=4 SN file, anon 200); **D03 fixed — action removed** (`/api/search/id` 500 no-enum, no data); **D13 fixed by hotfix on production** (GET_ID and REQUEST actions removed — 500 no-enum on both deployments; residual: network-trusting gate design and unprobed mutating index actions); **D14 escalated to a live leak on production** (real restricted page JPEGs served anonymously from the warm cache: `soub-340129` 115,946 B, `soub-847150` 55,523 B; test still 401); D07/D10/D11/D12 re-verified in their prior states (D07 prod open, D11/D12 fixed, D10 test fixed / prod 404). #370 checklist: items 1, 3, 4 verified; item 2 now verified (D06 fixed on test). New observations: restricted-projekt sweep `lat_A` stats count is aggregate-only (no coordinates served, coarse min/max); production komponenta count present but entity queries error (fix verification stays test-side). Probe registry extended (`M-TX-202600601`, `soub-847150`). **Operator decisions (2026-09-15):** D07 and D14 deferred — the upcoming production release deploys the fixes; D04 closed — no further live verification, reactive handling of reported issues; #693 distribution-gate verification owned by the file-distributions scenario. Creator-org clause exercised with the maintainer-provided records `C-202009779-N00022` (both arms) and `C-202211308-N00230` (projekt arm): works on the landing/entities path (C 200), dead on OAI (403 body) — folded into D08; the `typ_zmeny='D01'`/`SN01` source concern does not break the entities path |
| 2026-09-17 | 11 — #370 regression re-verification | fresh B/C/D sessions (operator-provided per-session credentials) and anonymous/OAI probes on the 2026-09-17 test deploy; production **not examined** (operator declined production probes): **D08 narrowed to the owner clause** (C-org clauses work on OAI now — both arms incl. projekt arm; B-owner clause still dead on OAI); **D09 open — confirmed defect** (file gate uniform across authentication, but the deployed SN A/B rows apply pristupnost conditions the published File API table does not state — operator: not desired behaviour; anon/B 403 on stav=4 SN files with elevated pristupnost — 3,397 test records; a correction of this run's first "fixed" reading is recorded in the provenance note); **D13 closed — solved in current state** (operator accepted the network-trusting gate posture and the mutating actions as-is); D06/D07/D10/D11/D12/D14 holdouts re-verified fixed on test; #370 checklist items 1-4 re-verified on test; `/api/search/id` removal and 404/410/401 distinction re-observed. Probe registry extended (`C-202500044-N00001` as the deployed A-row sample) |
| 2026-09-23 | 12 — #370 regression re-verification | fresh B/C/D sessions and anonymous probes on test, anonymous production probes (operator-approved): **D15 found and confirmed** (projekt file gate on the record-level rule — anon/B stav=6 files, C other-organisation files; operator: defect; production absent); **D12 reopened** (restricted stav=6 projekts unsuffixed on search/handle; production absent); **D09 extended** to the C row; **D16** reduced to the author's-organisation arm (not examinable); D08 still open; D06/D07/D10/D11/D13/D14 hold on test; production D07 and D14 still open; registry drift recorded (deleted-SN anchor live again, `C-202402033` now the 410 case); backend identity differs from the bundle hash |

