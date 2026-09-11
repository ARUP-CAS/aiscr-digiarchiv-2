# Permission model — testing scenario

**Key:** permissions (feature)
**Scope:** the permission behaviour of every backend surface that serves record data — landing pages, OAI-PMH, search/handle/legacy APIs, geometry and export endpoints, the File API, the image and PDF endpoints, and the admin/integration servlets — at record level and element level, for all roles. Issues [ARUP-CAS/aiscr-digiarchiv-2#370](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/370) and [#237](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/237) (rules definition) drove it.
**How to use this record:** the *Durable knowledge* half states the rules and the complete surface inventory; the *Current verification* half is a set of **coverage grids** (record type × role per surface). A grid cell records the observed result for that combination: `✓` = matches the documented rule, `✗` + finding id = deviation, `·` = no surface for that type by design, `—` = **untested on this build — the worklist for the next pass**. Fill or annotate cells only with fresh evidence; never mark a cell from assumption.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | `https://api-test.aiscr.cz/2.2/oai` | has #370 plus the 2026-09 fix wave; the 2026-09-11 deploy adds the D06 C-row fix (doc-derived `stav`/`pristupnost` in `SamostatnyNalez.filterOAI` — C/D now pass on public SNs) and gates `/pdf` via `ImageAccess`; the A/B branches still diverge from #237 (permissions-D06) |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | `https://api.aiscr.cz/2.2/oai` | old build: `/id/` always HTTP 200 shell, gating happens client-side; `/pdf` ungated (200 + placeholder), `/fedora/get_id` serves raw XML to network-trusted clients, VB geometry unsuffixed (see D07/D13/D14) |

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
| `GET /api/search/id` | `SearchServlet` ID | `searchable`-derived (gated since the fix wave) | legacy id lookup |
| `GET /api/search/id_as_child` | `SearchServlet` ID_AS_CHILD | child-doc fetch | child record JSON (verify per parent state) |
| `GET /api/search/gml`, `/wkt`, `/geometrie` | `SearchServlet` GML/WKT/GEOMETRIE | GEOMETRIE queries the role-suffixed `loc_rpt_<pr>` field (masks restricted pians for anonymous — verified `{}` on restricted coordinates); its pristupnost check is commented out in source (latent if the suffixing is dropped); reads `pian_chranene_udaje` blocks | geometry extracts |
| `GET /api/search/pians` | `SearchServlet` PIANS → `PIANSearcher.getMapPians` | fields include unsuffixed `pian:[json]` + `loc_rpt` | map pian points — live-probe candidate |
| `GET /api/search/mapa`, `/export`, `/export_mapa` | `SearchServlet` MAPA/EXPORT/EXPORT_MAPA | per-searcher filter (same as query) | map search + export variants |
| `GET /api/search/stats`, `/stats_index`, `/export_stats_index`, `/obdobi`, `/thesauri`, `/log`, `/check_relations`, `/home` | `SearchServlet` auxiliary actions | aggregate/heslar/analytics | statistics and vocabularies — low sensitivity, spot-check |
| `GET /exp` | `ExportServlet` → `searcher.export(request)` | **no servlet-level gate**; protection only inside `EntitySearcher.export` (per-searcher `filter()`); map path reads `*_chranene_udaje` geometry | csv/xlsx/xml/json export; `mapa=true` serves geometry (GeoJSON/GML) — live-probe candidate |
| `GET /pdf?id=<soubor_id>&page=N` | `PdfServlet` | test build (2026-09-11): `ImageAccess.isAllowed(request, true)` — the gate resolves the dokument by the **soubor id** (`getDokBySoubor`); a record-ident `id` resolves to nothing and returns **401 as a not-found**; SN always allowed (issue #85), dokument by `pristupnost` | PDF page images from the `thumbsDir` cache; production build still has **no gate** (200 + placeholder, D14 there) |
| `GET /img/thumb?id=<soubor_id>`, `/img/full?id=` | `ImageServlet` actions (only `thumb` and `full` exist — `thumb-large` is not a valid action and 500s) | `thumb` ungated (`ImageAccess.isAllowed(…, false)` = true); `full` gated by `ImageAccess.isAllowed(…, true)` + rate limiter | image variants by soubor id; `ImageAccess` full gate: SN → **always allowed** (issue #85), dokument → imgPr=A or userPr ≥ imgPr or same-org (same-org restricted to users ≤ C) |
| `GET /fedora/*` | `FedoraServlet` (test build actions: INDEX_FULL, STOP_INDEX, INDEX_UPDATE, STOP_UPDATE, INDEX_ENTITIES, INDEX_MODEL, INDEX_ID, REINDEX_FILTER, CHECK_DATESTAMP — **GET_ID/REQUEST removed**; production still serves GET_ID/REQUEST) | allowedIP list OR localhost OR `pristupnost >= indexSecLevel` (config, default E) — **the gate trusts the network position: unauthenticated local-network/VPN clients pass (permissions-D13), public-internet anonymous denied (operator-verified 2026-09-10)** | raw Fedora object access + reindex — admin surface; on production the raw record XML is served without any permission layer to trusted-network clients |
| `GET /mus/*` | `MuseionServlet` | pristupnost read (default A when anonymous); FORBIDDEN paths present — gate shape unclear from source | museion predmety integration — probed 2026-09-10: reachable, empty test data, no demonstrable leak |
| `GET /fav/*` | `FavoritesServlet` | session `userid` scoping | per-user favourites only |
| `GET /user/*` | `LoginServlet` | — | login/logout/islogged (credentials, not record data) |
| `GET /texts/*`, `/config/*`, `/i18n/*`, `/feedback` | Texts/Config/I18n/FeedbackServlet | — | UI chrome, no record data |
| JSP shells: `/results`, `/export`, `/export-mapa`, `/print`, `/stats`, `/museion`, `/favorites`, `/home`, `/id2/*`, `/map2/*`, `/registrace`, `/napoveda` | `StaticServlet` (index.jsp) | none (client-side) | all data comes from the API routes above |

`PausedFilter` on `/*` is an availability gate only. `ApiServlet.getSafePath` blocks `..` and backslashes; traversal-shaped input dies as 404.

### Architecture and implementation facts

- **Solr core routing decides landing-page existence.** `HandleServlet.checkId` queries the **`entities`** core. Top-level types there: `projekt`, `archeologicky_zaznam` (akce/lokalita), `dokument` (incl. 3D), `adb`, `pian`, `samostatny_nalez`, `ext_zdroj`, `let`. Child entities there: `dokumentacni_jednotka`, `komponenta`, `komponenta_dokument`, `dokument_cast`, `vyskovy_bod`, `neident_akce`. Types `uzivatel`, `heslo`, `ruian_*`, `organizace`, `osoba` index into their own cores → **always 404 on landing pages** (intended feature). The **`oai`** core serves OAI-PMH for all types, so OAI-PMH can serve records the landing page cannot.
- **filterOAI call sites use fresh model instances.** Both `HandleServlet.checkId` and `OAIRequest.filter` obtain the model via `FedoraModel.getFedoraModel(entity)`, which returns a **new, unpopulated instance**. A `filterOAI` implementation must read record state from the `SolrDocument` argument; model instance fields (e.g. `stav`) are default-valued on these paths. `SamostatnyNalez.filterOAI` violated this and broke SN record-level visibility (permissions-D06); the 2026-09-11 build reads the doc's `stav`/`pristupnost`/`historie`, which fixed the C/D rows — the A/B branch conditions still deviate from #237.
- **Where the rules live:** record-level rules in `FedoraModel.filterOAI` implementations (`web4/fedora/models/*.java`); file rules in `HandleServlet.isAllowed`; image rules in `ImageAccess`; element-level hiding in `OAIRequest.filter` (OAI) and via pristupnost-suffixed secured fields plus per-searcher `filter()` post-removal (search/handle/export).
- **The suffix mechanism has two halves and both must hold:** the query must project role-suffixed variants (`loc_rpt_<pr>`, `lat_<pr>`, suffixed secured JSON fields) **and** the post-filter must remove the unsuffixed block when `doc.pristupnost > userPr`. A searcher whose projected fields omit `pristupnost`, or whose `filter()` throws on a missing field, silently serves the unsuffixed block (permissions-D12 shape on projekt; D11 on komponenta — both fixed on the 2026-09-11 test build).
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

The deployed `SamostatnyNalez.filterOAI` still adds a `pristupnost <= userPr` condition to its A/B branches that #237's record-level rule does not carry, and the C-branch creator-organisation clause still checks `typ_zmeny='D01'` (dokument's creation event) where SN creation is `SN01`; both need reconciliation when the record-level fix lands (see permissions-D06 — the A/B divergence is the live-observed consequence on the 2026-09-11 build).

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

Small thumbnails (`/thumb`) are **always public**; large thumbnails (`/thumb/page/N`, `/thumb-large`) follow the original-file rules on the 2026-09-11 test build. **Image endpoints** (`/img`) apply their own `ImageAccess` rules (see surface inventory) — SN always allowed at full size, dokument by pristupnost. **`/pdf`** shares those rules on the test build (gated since the D14 fix); the production build still serves it ungated.

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
| `M-TX-202100125` | dokument | pr=C, stav=3 — restricted file sample; test file `3a0f7078-e26f-404d-aaa9-2b5abbb5e3d2`, production file `fe377f16-1da6-4912-b8d4-6f3095324879` (soub-340129) | file API, `/pdf`, `/thumb-large` (2026-09-11) |
| `ADB-PRAH71-000861` | adb | restricted (stav≠3), pr=C; carries VB `-V0001` | landing, OAI, search, handle, children |
| `ADB-BERO01-000001-V0001` | vyskovy_bod | production sample of a restricted-children VB with full unsuffixed geometry (EPSG:5514) | production search sweep (2026-09-11, D07) |
| `P-2213-100119` | pian | pr=C (HES-000867) — chranene geometry probe; exact coordinates in the raw Fedora XML | search, geometrie, `/exp` mapa, `/fedora` (D13) |
| `C-202210658` | projekt | **drifted to pr=A (2026-09-11)** — no longer a restricted-projekt sample on test; restricted-projekt candidates come from the sweep recipe (38 on test 2026-09-11); the production D12 sample is `M-202400606` | landing, OAI, search |
| `C-202402033` | projekt | stav=2 — second D12 sample (re-verify before use) | search |
| `C-201122587` | projekt | stav=6 with oznamovatel (PII masking case) | OAI oznamovatel |
| `C-202600010` | projekt | stav=3; file `342b7b35-fdc2-4fc7-b2df-bb3c0eecc524` (projekt file row) | file API |
| `C-202600009-N00014` | samostatny_nalez | pr=C, stav=1, **created by the B test account** (owner clause); file `c74d3136-3cab-468a-8405-3f79b89ab555`; carries komponenta `-K001` | landing, OAI, file, children, backend (2026-09-11: B-owner landing/file 200, OAI 403) |
| `C-202600010-N00085` | samostatny_nalez | pr=C, stav=4 (public-archived SN); files `cafc9a90-3b8b-40f8-ace6-5791b0b6ffd2` (soub-653685), `e5115f1a-72ce-4eaa-a2d9-f52544aaa370` (soub-653686) | landing, OAI, file, backend (2026-09-11: anon/B 403 ✗D06, C/D 200 ✓; file B 403 ✗D09) |
| `C-202204159-N00003` | samostatny_nalez | **deleted** (maintainer-controlled) — 410/tombstone case | landing, handle, OAI tombstone |
| `U-004495` | uzivatel | the B test account — own-record clause candidate (OAI core lag at last attempt) | OAI (idDoesNotExist) |
| let / heslo / ruian / organizace / osoba samples | open types | any state-matching record served to anonymous | OAI (re-derive via recipes) |

Test accounts (role B/C/D): `badatel.ai@arup.cas.cz`, `archeo.ai@arup.cas.cz`, `archivar.ai@arup.cas.cz` — passwords are **operator-provided per session and never recorded in this report** (2026-09-10 operator correction; the credential previously written here was removed and must not be reintroduced). Sessions expire ~30 min.

### Discovery recipes

Do not rely on registry anchors without re-verification; re-discover per session when needed. All steps work anonymously unless stated.

1. **Enumerate and classify records:** `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` (100 records/page, resumptionToken pagination). Classify each record by regex on the body: whole-record restriction `(?s)>\s*HTTP/1\.1 403 Forbidden\s*</amcr:amcr>`; element restriction `<amcr:chranene_udaje>HTTP/1\.1 403 Forbidden`; deleted header `status="deleted"`. Visible records expose `<amcr:stav>` and `<amcr:pristupnost … id="HES-…">` (id → A/B/C/D) for precise classification. `from`/`until` datestamp windows slice the stream.
2. **Predict 401 vs 403:** `GET /api/search/query?entity=<e>&q=ident_cely:"<ident>"` — `numFound: 0` while the record exists in OAI ⇒ not searchable ⇒ anonymous landing returns **401**; `numFound > 0` while OAI shows the record restricted ⇒ **403**.
3. **Visible baselines / element ladder:** from the same OAI scan, take records with the target `stav` and desired pristupnost level (A = open baseline; B/C/D = element hidden up to that role). Direct: `GET /api/search/query?entity=akce&q=pristupnost:<A-E>&rows=5`.
4. **404 candidates:** fabricate idents with valid prefixes but nonexistent numbers (e.g. bump an existing ident's numeric tail far out of range). Expect 404 for all roles.
5. **410 candidates:** scan sets for `status="deleted"` headers — a fresh deletion tops the set listing (tombstone datestamp = deletion time). Deleted **core-type** docs stay in the entities index with `is_deleted=true` and return **410** on landing and handle for every role; deleted `uzivatel`/`osoba` ids 404 (never served on landing by design). Fastest route: maintainer-controlled deletion of a test record, then re-query the set listing.
6. **File-API candidates:** file UUIDs come from the OAI `soubor` elements of **visible** records (`<amcr:url>`, `<amcr:path>`; some records have `soubor` without a Fedora path — skip those). The legacy `/api/search/id` endpoint is gated since the fix wave. Coverage per rule row: dokument and SN files plentiful; projekt files sparse — file a fresh oznámení in webamcr-test if needed. For `/img` and `/pdf` probes, the soubor id and the record ident (resp. page numbers) drive the endpoint.
7. **Child-record candidates:** derive from parent metadata — OAI `archeologicky_zaznam` records list `dokumentacni_jednotka` idents; `adb` records list `vyskovy_bod` idents; cast/komponenta idents follow the fixed suffix patterns. `/api/search/query?entity=komponenta|vyskovy_bod` (with an ident filter) confirms existence (DJ and dokument_cast entity queries are stubs).
8. **Cross-environment comparison:** run the same probes against production; data drifts — never assume the same ident has the same state in both environments. Production probing needs explicit operator approval.
9. **Restricted-children search sweep:** `GET /api/search/query?entity=vyskovy_bod&q=-stav:3&rows=0` (add `AND -pristupnost:A` for elevated children) — a non-zero `numFound` counts children of restricted parents that remain anonymously searchable with unsuffixed fields. Same shape for other child entities where `searchable` is not parent-derived; for komponenta/projekt add `q=...` restricted variants and inspect the chranene blocks.
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
# legacy id lookup
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

## Current verification (2026-09-11 regression pass on the #370 fix wave; production posture probes)

Grid legend: `✓` observed and matches the documented rule · `✗` + finding id = deviation · `·` = no surface for that type by design · `—` = untested on this build (worklist). Codes are the observed HTTP status; element state in parentheses.

**Provenance note:** the 2026-09-11 pass re-verified every cell tied to the D01–D14 defect set, the #370 checklist, and the production posture (fresh evidence below); cells carrying only the 2026-09-10 date were observed on the same fix-wave build family one deploy earlier and were not re-probed this pass.

### 2026-09-11 pass summary — defect outcomes

| Defect | Outcome on test | Production posture |
| --- | --- | --- |
| D06 | **narrowed — still open**: C/D now pass on public stav=4 SNs (landing and OAI); anonymous and B still get 403 because the A/B branches carry `pristupnost <= userPr` conditions absent from #237's `stav=4` rule | same build family as the 2026-09-10 audit (client-side gating there) |
| D07 | **fixed on test**: elevated restricted VBs stay indexed but the anonymous search response no longer carries `vyskovy_bod_geom_wkt`/`geom_gml` | **open**: `entity=vyskovy_bod&q=-stav:3` → numFound 11,530 restricted-children docs with full unsuffixed geometry (sample `ADB-BERO01-000001-V0001`, EPSG:5514 POINT Z) |
| D08 | **narrowed — still open**: C/D now receive SN records on OAI (instance-`stav` root cause fixed); the B-owner clause still returns the 403 body (oai-doc owner fields still not resolvable) and anonymous/B still get 403 bodies for public stav=4 records | — |
| D09 | **still open** (re-verified): B gets 403 on a stav=4 SN file that anonymous fetches (200); B-owner gets 200 on a stav=1 file (SN01 owner clause works) | — |
| D10 | **fixed on test**: `/thumb-large` 403 anonymous on a restricted dokument file (orig 403, `/thumb` 200, `/thumb/page/1` 403, `/paradata` 403 — full variant matrix conforms) | endpoint absent (404 — fix-wave endpoint) |
| D11 | **fixed on test**: komponenta search/handle/legacy serve no chranene block to anonymous; komponenta CSV export — anonymous: record filtered out; B: row served with katastr masked; C: row served pr-legitimately | n.a. (production's old build has no komponenta docs in the entities core) |
| D12 | **fixed on test**: 38 restricted projekts searchable, `projekt_chranene_udaje` absent from anonymous docs (anchor `C-202210658` drifted to pr=A — sweep replaced it) | **fixed**: `M-202400606` (pr=C) serves no chranene block anonymously; `C-202210658` numFound 0 |
| D13 | **posture unchanged — network-trusted gate**: the test build removed the `GET_ID` read action (an invalid action from the trusted position returns the 500 no-enum error *inside* the gate-passed branch; the remaining actions are index-management); the gate still admits this workstation's (VPN/local-network) position without login | **open from the trusted position**: `/fedora/get_id?id=C-202210658` serves the complete raw AMCR XML **including `chranene_udaje`** (≈29 kB) to an unauthenticated client |
| D14 | **fixed on test**: `/pdf` 401 anonymous on a restricted dokument (both the record-ident shape, which resolves nothing, and the soubor-id shape, which the gate denies); the SN soubor-id shape passes the gate per #85 and serves only the placeholder on the cold cache | **open**: `/pdf?id=M-TX-202100125&page=1` → 200 with the 7,525 B `empty_big.png` placeholder (no gate in the old build; warm-cache leak not observable from this position) |

**Issue #370 checklist mapping (2026-09-11):**

1. *Non-archived records visible after login (landing only)* — ✓ verified: `C-202204147A` 200 for B and C, `C-TX-193001369` 200 for B; anonymous gets 401 on both.
2. *Same conditions as the API (#237)* — partially: SN A/B rows still diverge (D06); C/D conform; dokument/AZ/projekt/pian/adb rows verified conformant (2026-09-10 baseline).
3. *404/410/401/403 distinction* — ✓ re-verified anonymous: fabricated ident 404; deleted SN `C-202204159-N00003` 410; unarchived SN `C-202600009-N00014` 401; restricted public SN `C-202600010-N00085` 403.
4. *Correct return codes* — ✓ on the test build across surfaces (401 vs 403 per the `searchable` rule, 410 tombstone, 429 limiter re-observed on a burst); production residual: `/pdf` 200-placeholder instead of 401 (D14 there).

### Landing pages (`/id/`, incl. `/map/` variants)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C stav=3 (`C-202009490A`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| akce unarchived (`C-202204147A`) | 401 ✓ | 200 ✓ (2026-09-11) | 200 ✓ (2026-09-11) | 200 ✓ |
| AZ pr=B (`M-200500013A`) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) | 200 ✓ |
| dokument stav=3 pr=C | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument unarchived (`C-TX-193001369`) | 401 ✓ | 200 ✓ (2026-09-11) | 200 ✓ | 200 ✓ |
| adb restricted (`ADB-PRAH71-000861`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| ext_zdroj stav=1 (`BIB-0000052`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| pian state-matching | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| let (any state) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=6 | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=2 (`C-202210658`) | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| SN pr=C stav=4 (`C-202600010-N00085`) | 403 ✗D06 | 403 ✗D06 | 200 ✓ (2026-09-11 — fixed) | 200 ✓ |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) | 401 ✓ | 200 ✓ (owner; 2026-09-11) | 200 ✓ (2026-09-11) | 200 ✓ (2026-09-11) |
| uzivatel, heslo, ruian_kraj/okres/katastr, organizace, osoba | · 404 | · 404 | · 404 | · 404 |
| DJ of restricted akce (`…-D01`) | 200 ✓ (inherited B+) | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument_cast unarchived (`…-D001`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| VB of restricted ADB (`…-V0001`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| komponenta (SN `…-K001`) | 200 ✓ (masked; 2026-09-11) | 200 ✓ (masked) | 200 ✓ (pr-legitimate content) | 200 ✓ |
| deleted SN (`C-202204159-N00003`) | 410 ✓ (2026-09-11) | 410 ✓ | 410 ✓ | 410 ✓ |
| fabricated ident | 404 ✓ (2026-09-11) | 404 ✓ | 404 ✓ | 404 ✓ |

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
| uzivatel own-record clause | — (oai-core lag: both test accounts `idDoesNotExist`) | — | — | — |
| SN pr=C stav=1 B-owned | 403 body ✓ | 403 body ✗D08 (owner clause dead; 2026-09-11) | 403 body ✓ (org mismatch — rule-conformant; 2026-09-11) | 200 ✓ (full) |
| SN pr=C stav=4 public | 403 body ✗D06 | 403 body ✗D06 | 200 ✓ (full; 2026-09-11 — fixed) | 200 ✓ (full; 2026-09-11) |
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
| projekt restricted (sweep) | ✓ no chranene block — **fixed 2026-09-11** (38 restricted projekts searchable, block absent) | ✓ masked | ✓ block served — pr-legitimate | ✓ block served — pr-legitimate |
| komponenta (SN `…-K001`) | ✓ no chranene block — **fixed 2026-09-11** | ✓ masked | ✓ pr-legitimate (C ≥ record pr) | — |
| oznamovatel (any type) | ✓ not served by any backend surface | ✓ | ✓ | ✓ |
| uzivatel/heslo/ruian/organizace/osoba | · (search queries the entities core only) | · | · | · |

Notes: legacy `/api/search/id` gated since the fix wave (numFound 0 for non-searchable records, anonymous; komponenta legacy-id re-checked 2026-09-11); `entity=dokumentacni_jednotka`/`dokument_cast` are pre-existing stubs; `D07` on test — anonymous `entity=vyskovy_bod&q=-stav:3` no longer serves geometry fields (fixed); production still returns 11,530 restricted-children docs with unsuffixed geometry.

### Handle API (`/api/search/handle?id=`)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ error/masked | ✓ masked (2026-09-11: no az/akce chranene, no loc_rpt) | ✓ full (2026-09-11: chranene + pian geometry) | ✓ full |
| SN pr=C stav=4 | 403 ✗D06 | 403 ✗D06 | 200 ✓ (2026-09-11 — fixed) | 200 ✓ |
| SN pr=C stav=1 B-owned | 401 ✓ | 200 ✓ (owner; 2026-09-11: chranene masked, suffixed `_B` fields empty) | 200 ✓ | 200 ✓ |
| deleted SN | 410 ✓ | 410 ✓ | 410 ✓ | 410 ✓ |
| DJ/dokument_cast/VB (restricted/unarchived parents) | ✓ matching codes, no data | ✓ | ✓ | ✓ |
| komponenta (SN `…-K001`) | ✓ no chranene block — **fixed 2026-09-11** | ✓ masked | ✓ 200 (pr-legitimate: C ≥ record pr) | — |
| projekt stav=2 | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |

### File API (`/id/<ident>/file/<uuid>` and variants)

| Rule row / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| dokument pr=C stav=3 (`M-TX-202300441`, `M-TX-202100125`) — orig | 403 ✓ | 403 ✓ | 200 ✓ | — |
| └ `/thumb` | 200 ✓ (documented ungated) | — | — | — |
| └ `/thumb/page/1` | 403 ✓ | — | — | — |
| └ `/thumb-large` | 403 ✓ — **fixed 2026-09-11** (D10) | — | — | — |
| └ `/paradata` | 403 ✓ | — | — | — |
| SN pr=C stav=4 (`C-202600010-N00085`) — orig | 200 ✓ (A rule; 2026-09-11) | 403 ✗D09 (2026-09-11 — still open) | 200 ✓ (2026-09-11) | 200 ✓ |
| └ `/thumb/page/1` | 200 ✓ | 403 ✗D09 | — | — |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) — orig | 403 ✓ | 200 ✓ (SN01 owner; 2026-09-11 — still works) | 200 ✓ | 200 ✓ |
| └ `/thumb-large` | 403 ✓ — fixed with D10 (was 200 ✗D10) | — | — | — |
| projekt stav=3 (`C-202600010`) — orig | 403 ✓ | 403 ✓ (never) | 403 ✓ (other org) | 200 ✓ |
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
| `/pdf?id=<restricted dokument soubor id>&page=1` | ✓ 401 — **fixed 2026-09-11** (D14: `ImageAccess.isAllowed(…, true)` gate; record-ident shape also 401 — resolves nothing) | — | — | — |
| `/pdf?id=<SN soubor id>&page=1` | ✓ gate passes per #85; cold cache serves the 7,525 B placeholder only — no content leak | — | — | — |
| production `/pdf?id=<restricted dokument>&page=1` | ✗ 200 + placeholder (no gate in the old build — status-code defect; warm-cache leak not observable from this position) | — | — | — |

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
| test `/fedora/get_id?id=<restricted projekt>` | n.a. — action removed in the test build (`No enum constant …GET_ID` 500; the request itself passes the network-trusting gate from this position) | — | — | — |
| production `/fedora/get_id?id=C-202210658` | ✗D13 — **full raw AMCR XML with `chranene_udaje`** (2026-09-11 re-verification from the trusted position) | — | — | — |
| production `/fedora/get_id?id=<restricted SN>` | ✗D13 — full raw XML with chranene (katastr, lokalizace, geometry) — same network-position scope (2026-09-10) | — | — | — |
| production `/fedora/request?url=record/<ident>` | ✗D13 — raw Fedora container listing (walkable; same network-position scope; 2026-09-10) | — | — | — |
| `/mus/predmety_by_id`, `/mus/statistika` | ✓ reachable, empty test data (no demonstrable leak) | — | — | — |
| `/api/search/stats` | ✓ 200 public aggregates | — | — | — |
| `/fav/*` | ✗ 500 anonymous (ungated servlet but errors without a user; no data served) | — | — | — |
| `/api/search/stats_index` | ✓ 200 anonymous (index aggregates) | — | — | — |

### Findings registry

| ID | Severity | Surface | Status | One-line summary |
| --- | --- | --- | --- | --- |
| D01 | minor | bare `/map` | **fixed** | `/map` 500 → 200 forward to `/map2` |
| D02 | — | SN record rules | **changed** | fix-wave rewrite superseded by D06; owner clause verified live (again 2026-09-11) |
| D03 | High | legacy `/api/search/id` | **fixed** | anonymous leak of non-searchable records → numFound 0 |
| D04 | minor | file API rate limiter | partial | interval branch live-verified (re-observed 2026-09-11); concurrent branch code-only |
| D05 | High | child-record landing | **fixed on landing/handle**; search residual = D07 (open on production only) | DJ/dokument_cast/VB inherit parent rules again |
| D06 | High | SN record level | **open — narrowed** (2026-09-11: C/D fixed via doc-derived stav; A/B still 403 on public stav=4 SNs) | the A/B branches' `pristupnost <= userPr` conditions contradict #237's `stav=4` rule |
| D07 | Medium-High | search API (vyskovy_bod) | **fixed on test; open on production** (2026-09-11: 11,530 restricted VB docs with unsuffixed geometry on prod) | restricted VB geometry anonymously searchable |
| D08 | High | OAI-PMH (SN) | **open — narrowed** (2026-09-11: C/D served; B-owner clause still dead) | SN OAI owner clause reads fields the oai doc does not resolve; anon/B still get 403 bodies for public SNs |
| D09 | Medium | file API | **open** (re-verified unchanged 2026-09-11) | authenticated B denied a stav=4 file anonymous fetches |
| D10 | High | file API `/thumb-large` | **fixed on test** (2026-09-11: 403 anonymous); production never had the endpoint (404) | undocumented endpoint bypassed all permission checks |
| D11 | High | backend surfaces (komponenta) | **fixed on test** (2026-09-11: search/handle/legacy/export verified) | komponenta docs served the SN chranene block unmasked |
| D12 | High | search API (projekt) | **fixed on test and production** (2026-09-11) | anonymous search served full `projekt_chranene_udaje` for restricted projekts |
| D13 | High | `/fedora/*` | **open — network-trusted posture unchanged** (2026-09-11: test build removed GET_ID; production still serves raw XML incl. chranene from the trusted position) | unauthenticated raw-Fedora access from the local network/VPN — full raw record XML bypassing every permission layer |
| D14 | High | `/pdf` | **fixed on test** (2026-09-11: 401 anonymous via the ImageAccess gate); **open on production** (200 + placeholder, no gate in the old build) | PDF page JPEGs served with no permission gate |

- **D06 (High, narrowed 2026-09-11):** the 2026-09-11 test deploy reads the doc's `stav`/`pristupnost` in `SamostatnyNalez.filterOAI`, so the instance-field defect is gone and C/D now pass on public stav=4 SNs (landing and OAI both; e.g. `C-202600010-N00085` → C 200, D 200, full record on OAI). The deviation that remains: the A and B branches still carry `pristupnost <= userPr` conditions that #237's record-level rule does not have — a public stav=4 SN with pristupnost above the requester's level returns 403 to anonymous and B on every surface (landing, map, handle, OAI). Still to reconcile when fixing: the C-branch creator-organisation clause checks `typ_zmeny='D01'` where SN creation is `SN01` (not reachable with current anchors — the pr-arm passes first for pr≤C records; a pr>D record with org match would exercise it).
- **D07 (Medium-High; fixed on the 2026-09-11 test build, open on production):** on test, the anonymous search response for restricted VBs no longer carries `vyskovy_bod_geom_wkt`/`geom_gml` (elevated VBs remain indexed and countable, but no geometry is served). On production, `entity=vyskovy_bod&q=-stav:3` still returns 11,530 restricted-children docs and the first doc serves the full unsuffixed geometry (`ADB-BERO01-000001-V0001`, EPSG:5514 `POINT Z (-750588.48 -1042183.7 330.85)`). Fix direction unchanged: parent-derived `searchable` for VB docs and/or pristupnost-suffixed geometry fields — deploy the test-build fix to production.
- **D08 (High, narrowed 2026-09-11):** the instance-`stav` root cause is fixed (C/D now receive SN records on OAI). What remains: (1) the B-owner clause still returns the 403 body on OAI (`C-202600009-N00014` as B-owner: landing 200, OAI 403), so the oai-document shape still does not resolve the fields the owner/organisation clauses read (or the clauses themselves are broken); (2) anonymous and B still receive the 403 body for public stav=4 SNs on OAI — the D06 A/B divergence seen on the landing surface. C-org behaviour is rule-conformant on the records probed (org-match record 200, other-org record 403).
- **D09 (Medium, re-verified open 2026-09-11):** unchanged shape — B gets 403 on the stav=4 SN file (`C-202600010-N00085` orig) that anonymous fetches (200), while the SN01 owner clause works on the file surface (B-owner 200 on `C-202600009-N00014`). The logged-in branch of the file gate still applies a pristupnost comparison without the stav=4 exemption the anonymous branch honors. Fix direction: apply the per-role file rules uniformly regardless of authentication.
- **D10 (High, fixed on test 2026-09-11):** `HandleServlet.isAllowed` no longer lets `thumb-large` through the ungated `contains("thumb") && !contains("page")` branch — the full variant matrix on a restricted dokument file (`M-TX-202100125`, pr=C, stav=3) now conforms: orig 403, `/thumb` 200 (documented ungated), `/thumb/page/1` 403, `/thumb-large` 403, `/paradata` 403 (all anonymous). Production never carried the endpoint (404).
- **D11 (High, fixed on test 2026-09-11):** komponenta docs no longer serve the copied `samostatny_nalez_chranene_udaje` block on any probed surface: search and handle return no block to anonymous, the legacy id endpoint likewise, and the CSV export masks per role — anonymous gets no row (record filtered), B gets the row with the chranene-derived katastr column empty, C gets the katastr (pr-legitimate). `Komponenta.filterOAI` still returns true unconditionally in source — the masking now happens on the search path; the record-level gate remains the komponenta scenario's subject.
- **D12 (High, fixed on test and production 2026-09-11):** anonymous search no longer serves `projekt_chranene_udaje` for restricted projekts — on test, 38 restricted projekts are searchable with the block absent (the former anchor `C-202210658` drifted to pr=A, so the sweep recipe replaced it); on production, the 2026-09-10 sample `M-202400606` (pr=C) now serves no chranene block anonymously and `C-202210658` is not served at all. The unsuffixed projection in `ProjektSearcher`'s field list noted in the 2026-09-10 audit no longer produces a leak on either deployment.
- **D13 (High, posture unchanged, re-verified 2026-09-11):** the gate still trusts the network position. The test build removed the `GET_ID` read action (remaining actions are index-management: INDEX_FULL, INDEX_UPDATE, REINDEX_FILTER, …), and a probe with an invalid action from this workstation's position returns the 500 no-enum error thrown *inside* the gate-passed branch — confirming this VPN/local-network position still passes without login on test. Production still serves `GET_ID`: `/fedora/get_id?id=C-202210658` returned the complete raw AMCR XML including the full `chranene_udaje` block (katastr, lokalizace) to an unauthenticated client from the same position. The 2026-09-10 operator verification stands (public-internet anonymous denied); remaining risk is internal — any VPN/local client without login reads every record's raw XML. Fix direction unchanged: require authentication even from trusted networks, or narrow `allowedIP` and document the exception.
- **D14 (High; fixed on test 2026-09-11, open on production):** the test build gates `/pdf` through `ImageAccess.isAllowed(request, true)`, keyed on the **soubor id**: a restricted dokument's page returns 401 to anonymous in both request shapes (the record-ident shape resolves nothing and 401s; the soubor-id shape is denied by the gate), and an SN soubor id passes the gate per #85 but the cold cache serves only the 7,525 B placeholder — no content leak. Production still runs the ungated build: `/pdf?id=M-TX-202100125&page=1` answers 200 with the placeholder to anonymous — the status-code defect persists there and a warm page cache would serve restricted content; the warm-cache case is not observable from this position. Fix direction: deploy the gated build.

### Pre-existing observations

- The `dokumentacni_jednotka` and `dokument_cast` entity-search endpoints are stubs and return error bodies — pre-existing, unrelated to permissions, no data exposure.
- Test-data note: the `archivar.ai@arup.cas.cz` test account initially reported pristupnost B; the operator corrected it to D mid-run (2026-09-10).
- Quoted-phrase Solr interpolation (code-verified, low): `HandleServlet.getDocument`'s `soubor_filepath:"<url-path>"` filter and OAIRequest's `ident_cely:"<id>"` interpolate the request path/identifier without escaping query metacharacters (`InitServlet.asSafePath` blocks traversal and backslashes only). No privilege bypass found — the permission gate still runs on whatever document the query returns — but the construction is injection-shaped; escaped terms would remove the class.
- The `oai` core lags the `entities` core for freshly created records: a user record created days before the run returns `idDoesNotExist` on OAI while being present in entities — freshness note for harvesters, and the reason the uzivatel my-record clause could not be live-checked for the test accounts.
- Test-data quality: several records carry placeholder file paths (`"neni"`) and omit `soubor/path`+`soubor/url` elements in OAI output; well-formed records (verified via the D view) do serve both. Probes used well-formed records — do not read the omission as build behaviour.
- Environment-specific file bindings (2026-09-11): `M-TX-202100125` binds test file `3a0f7078-…` but production file `fe377f16-…`; always re-read the soubor path from the environment being probed.

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
