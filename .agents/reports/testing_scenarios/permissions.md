# Permission model — testing scenario

**Key:** permissions (feature)
**Scope:** the permission behaviour of every backend surface that serves record data — landing pages, OAI-PMH, search/handle/legacy APIs, geometry and export endpoints, the File API, the image and PDF endpoints, and the admin/integration servlets — at record level and element level, for all roles. Issues [ARUP-CAS/aiscr-digiarchiv-2#370](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/370) and [#237](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/237) (rules definition) drove it.
**How to use this record:** the *Durable knowledge* half states the rules and the complete surface inventory; the *Current verification* half is a set of **coverage grids** (record type × role per surface). A grid cell records the observed result for that combination: `✓` = matches the documented rule, `✗` + finding id = deviation, `·` = no surface for that type by design, `—` = **untested on this build — the worklist for the next pass**. Fill or annotate cells only with fresh evidence; never mark a cell from assumption.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | `https://api-test.aiscr.cz/2.2/oai` | has #370 plus the 2026-09 fix wave (return-code fixes, login-gated non-archived records, child-record gating); carries the permissions-D06 regression |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | `https://api.aiscr.cz/2.2/oai` | old build: `/id/` always HTTP 200 shell, gating happens client-side |

OAI quirks: on the test instance, OAI identifiers use the **`https://api-test.aiscr.cz/id/…`** prefix (GetRecord with the production prefix silently fails with `idDoesNotExist`); set names containing a colon must be percent-encoded (`set=dokument%3A3d`). Versioned OAI endpoints (`/2.1/oai`, `/2.2/oai`) serve the same filtered core and differ only by XSLT.

### Surface inventory (routes and gates)

Complete enumeration from `web.xml` + `@WebServlet` annotations (2026-09-10 source audit). `ApiServlet` (`/api`, also `/api/*` in web.xml) is a pure path-normalizing forwarder, so every action servlet is also directly reachable under its own path (e.g. `/search/query` ≡ `/api/search/query`).

| Route | Servlet / action | Gate mechanism | Permission-relevant behaviour |
| --- | --- | --- | --- |
| `GET /id/<ident>`, `GET /map/<ident>` | `HandleServlet` `checkId` | 404 (no doc in `entities`) → 410 (`is_deleted`) → 401 (`!searchable` + anonymous) → 403 (per-model `filterOAI`) → 200 | landing / map gating; rendered page is a shell, data comes from the handle API |
| `GET /id/<ident>/file/<uuid>` (+ `/thumb`, `/thumb/page/N`, `/thumb-large`, `/paradata`, `/paradata/<distribution>`, distribution suffixes per #693) | `HandleServlet` file path | `HandleServlet.isAllowed` per XPath rule row; #693 rule: distributions and paradata follow the original's rules; Basic auth accepted here (not on `checkId`); rate limiter per (ip, file) | File API; `/thumb` documented ungated; `/thumb-large` bypasses (D10); #693 test pattern below |
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
| `GET /pdf?id=<ident>&page=N` | `PdfServlet` | **no permission gate in source** — serves pre-generated page JPEGs (watermarked) from the `thumbsDir` cache if the file exists | PDF page images — the same cache the gated `/thumb/page/N` uses — live-probe candidate |
| `GET /img/thumb?id=<soubor_id>`, `/img/full?id=` | `ImageServlet` actions (only `thumb` and `full` exist — `thumb-large` is not a valid action and 500s) | `thumb` ungated (`ImageAccess.isAllowed(…, false)` = true); `full` gated by `ImageAccess.isAllowed(…, true)` + rate limiter | image variants by soubor id; `ImageAccess` full gate: SN → **always allowed** (issue #85), dokument → imgPr=A or userPr ≥ imgPr or same-org (same-org restricted to users ≤ C) |
| `GET /fedora/*` | `FedoraServlet` (REQUEST_RAW, REQUEST, GET_ID, …) | allowedIP list OR localhost OR `pristupnost >= indexSecLevel` (config, default E) — **the gate trusts the network position: unauthenticated local-network/VPN clients pass (permissions-D13), public-internet anonymous denied (operator-verified 2026-09-10)** | raw Fedora object access + reindex — admin surface; serves raw record XML without any permission layer to trusted-network clients |
| `GET /mus/*` | `MuseionServlet` | pristupnost read (default A when anonymous); FORBIDDEN paths present — gate shape unclear from source | museion predmety integration — probed 2026-09-10: reachable, empty test data, no demonstrable leak |
| `GET /fav/*` | `FavoritesServlet` | session `userid` scoping | per-user favourites only |
| `GET /user/*` | `LoginServlet` | — | login/logout/islogged (credentials, not record data) |
| `GET /texts/*`, `/config/*`, `/i18n/*`, `/feedback` | Texts/Config/I18n/FeedbackServlet | — | UI chrome, no record data |
| JSP shells: `/results`, `/export`, `/export-mapa`, `/print`, `/stats`, `/museion`, `/favorites`, `/home`, `/id2/*`, `/map2/*`, `/registrace`, `/napoveda` | `StaticServlet` (index.jsp) | none (client-side) | all data comes from the API routes above |

`PausedFilter` on `/*` is an availability gate only. `ApiServlet.getSafePath` blocks `..` and backslashes; traversal-shaped input dies as 404.

### Architecture and implementation facts

- **Solr core routing decides landing-page existence.** `HandleServlet.checkId` queries the **`entities`** core. Top-level types there: `projekt`, `archeologicky_zaznam` (akce/lokalita), `dokument` (incl. 3D), `adb`, `pian`, `samostatny_nalez`, `ext_zdroj`, `let`. Child entities there: `dokumentacni_jednotka`, `komponenta`, `komponenta_dokument`, `dokument_cast`, `vyskovy_bod`, `neident_akce`. Types `uzivatel`, `heslo`, `ruian_*`, `organizace`, `osoba` index into their own cores → **always 404 on landing pages** (intended feature). The **`oai`** core serves OAI-PMH for all types, so OAI-PMH can serve records the landing page cannot.
- **filterOAI call sites use fresh model instances.** Both `HandleServlet.checkId` and `OAIRequest.filter` obtain the model via `FedoraModel.getFedoraModel(entity)`, which returns a **new, unpopulated instance**. A `filterOAI` implementation must read record state from the `SolrDocument` argument; model instance fields (e.g. `stav`) are default-valued on these paths. `SamostatnyNalez.filterOAI` violated this and broke SN record-level visibility (permissions-D06).
- **Where the rules live:** record-level rules in `FedoraModel.filterOAI` implementations (`web4/fedora/models/*.java`); file rules in `HandleServlet.isAllowed`; image rules in `ImageAccess`; element-level hiding in `OAIRequest.filter` (OAI) and via pristupnost-suffixed secured fields plus per-searcher `filter()` post-removal (search/handle/export).
- **The suffix mechanism has two halves and both must hold:** the query must project role-suffixed variants (`loc_rpt_<pr>`, `lat_<pr>`, suffixed secured JSON fields) **and** the post-filter must remove the unsuffixed block when `doc.pristupnost > userPr`. A searcher whose projected fields omit `pristupnost`, or whose `filter()` throws on a missing field, silently serves the unsuffixed block (permissions-D12 shape on projekt; D11 on komponenta).
- **`/map/<ident>`** applies the identical gating before forwarding to `/map2`. Bare `/map` forwards to `/map2` since the 2026-09 fix wave (was 500, permissions-D01).
- **Rate limiting:** File API and `/img/full` requests are tracked per IP + file; keep **≥ 1 s between requests**; a duplicate inside the window returns **429** with `Retry-After: 0` (interval branch, live-verified); the concurrent-in-progress branch (`Retry-After: -1`) is code-verified only. The limiter keys on client IP, so proxy buffering or egress-IP rotation can defeat observation from outside.
- **Page cache:** on test (`isTestEnv=true`) the landing-page cache is skipped entirely; on production a 1-day anonymous cache exists but only affects the rendered shell (record data is always fetched live per user), so it has no permission impact.
- **Child-entity indexing (since the 2026-09 fix wave):** `dokumentacni_jednotka` docs stay `searchable=true` and are gated in `filterOAI` by the parent-propagated `stav` (anonymous gets 403, not 401, for a non-archived parent); `dokument_cast` inherits the parent dokument's `searchable` (401 shape); `vyskovy_bod` inherits `stav`/`pristupnost`/`searchable` from the ADB at index time. VB geometry fields (`vyskovy_bod_geom_wkt`, `vyskovy_bod_geom_gml`) are indexed **without** pristupnost suffixes (see permissions-D07).
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

The deployed `SamostatnyNalez.filterOAI` adds a `pristupnost <= userPr` condition to its A/B branches that #237's record-level rule does not carry, and the C-branch creator-organisation clause checks `typ_zmeny='D01'` (dokument's creation event) where SN creation is `SN01`; both need reconciliation when the record-level fix lands (see permissions-D06).

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

Small thumbnails (`/thumb`) are **always public**; large thumbnails (`/thumb/page/N`) follow the original-file rules; PDF page thumbnails additionally require the pre-generated page-JPEG cache (cold on test at the time of the 2026-08-28 run — images work, PDFs 404 after passing the gate; production serves them). **Image endpoints** (`/img`) apply their own `ImageAccess` rules (see surface inventory) — SN always allowed at full size, dokument by pristupnost.

**Child records:** child entities live in the entities core with their own idents derived from the parent: `dokumentacni_jednotka` = `<AZ>-D01…`; `komponenta` (under AZ or SN) = `<parent>-K001…`; `dokument_cast` = `<dokument>-D001…`; `vyskovy_bod` = `<ADB>-V0001…`.

### Probe-record registry

Anchors from the 2026-09-10 passes on `digiarchiv-test`. **Drift rule:** records change state; re-verify each record's current state (OAI or search) before using it as a probe, and re-derive replacements via the recipes when it has drifted. File UUIDs come from the record's `soubor` elements.

| Ident | Type | Properties that make it a probe | Verified on |
| --- | --- | --- | --- |
| `C-202009490A` | akce | pristupnost C, archived (stav=3), restricted element case; carries DJ `-D01` and komponenta `-K001` | landing, OAI, search, handle, file (children) |
| `M-200500013A` | archeologicky_zaznam | pristupnost B — element ladder one step up | landing, element ladder |
| `C-202204147A` | akce | unarchived → anonymous 401 case | landing (B/C/D 200) |
| `C-TX-193001369` | dokument | unarchived → anonymous 401; carries dokument_cast `-D001` | landing, children |
| `M-TX-202300441` | dokument | pr=C, stav=3; file `3a0f7078-e26f-404d-aaa9-2b5abbb5e3d2` (real PNG; soubor id `soub-607938` for `/img` probes — the uuid binding drifted to the `"neni"` placeholder mid-pass 2026-09-10, see file-grid notes) | file API all endpoints, `/img` |
| `M-TX-202100125` | dokument | pr=C, stav=3 — second restricted file sample | file API |
| `ADB-PRAH71-000861` | adb | restricted (stav≠3), pr=C; carries VB `-V0001` | landing, OAI, search, handle, children |
| `P-2213-100119` | pian | pr=C (HES-000867) — chranene geometry probe; exact coordinates in the raw Fedora XML | search, geometrie, `/exp` mapa, `/fedora` (D13) |
| `C-202210658` | projekt | stav=2 → A/B 403 vs C/D 200 (D12 search leak sample) | landing, OAI, search |
| `C-202402033` | projekt | stav=2 — second D12 sample | search |
| `C-201122587` | projekt | stav=6 with oznamovatel (PII masking case) | OAI oznamovatel |
| `C-202600010` | projekt | stav=3; file `342b7b35-fdc2-4fc7-b2df-bb3c0eecc524` (projekt file row) | file API |
| `C-202600009-N00014` | samostatny_nalez | pr=C, stav=1, **created by the B test account** (owner clause); file `c74d3136-3cab-468a-8405-3f79b89ab555`; carries komponenta `-K001` | landing, OAI, file, children, backend |
| `C-202600010-N00085` | samostatny_nalez | pr=C, stav=4 (public-archived SN); file `cafc9a90-3b8b-40f8-ace6-5791b0b6ffd2` | landing, OAI, file, backend |
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
10. **Role sessions:** `curl -s -D - "https://digiarchiv-test.aiscr.cz/user/login?user=<email>&pwd=<pwd>" -o NUL` captures `Set-Cookie: JSESSIONID=…`; verify with `/user/islogged?wantsUser=true` replaying the cookie; replay on any surface for role views. OAI also accepts Basic auth directly.
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
# PDF page images (no gate in source — live-probe candidate)
curl -s -o /dev/null -w "%{http_code}\n" "https://digiarchiv-test.aiscr.cz/pdf?id=<IDENT>&page=1"
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

## Current verification (2026-09-10, `digiarchiv-test` build with the #370 fix wave; production probe for provenance)

Grid legend: `✓` observed and matches the documented rule · `✗` + finding id = deviation · `·` = no surface for that type by design · `—` = untested on this build (worklist). Codes are the observed HTTP status; element state in parentheses.

### Landing pages (`/id/`, incl. `/map/` variants)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C stav=3 (`C-202009490A`) | 200 ✓ (masked) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) |
| akce unarchived (`C-202204147A`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| AZ pr=B (`M-200500013A`) | 200 ✓ (masked) | 200 ✓ (full) | 200 ✓ (full) | 200 ✓ |
| dokument stav=3 pr=C | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument unarchived (`C-TX-193001369`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| adb restricted (`ADB-PRAH71-000861`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| ext_zdroj stav=1 (`BIB-0000052`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| pian state-matching | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| let (any state) | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=6 | 200 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| projekt stav=2 (`C-202210658`) | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |
| SN pr=C stav=4 (`C-202600010-N00085`) | 403 ✗D06 | 403 ✗D06 | 403 ✗D06 | 200 ✓ |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) | 401 ✓ | 200 ✓ (owner) | 200 ✓ | 200 ✓ |
| uzivatel, heslo, ruian_kraj/okres/katastr, organizace, osoba | · 404 | · 404 | · 404 | · 404 |
| DJ of restricted akce (`…-D01`) | 200 ✓ (inherited B+) | 200 ✓ | 200 ✓ | 200 ✓ |
| dokument_cast unarchived (`…-D001`) | 401 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| VB of restricted ADB (`…-V0001`) | 403 ✓ | 200 ✓ | 200 ✓ | 200 ✓ |
| komponenta (SN `…-K001`) | 200 ✗D11 | 200 ✗D11 | 200 ✗D11 (pr-legitimate content) | 200 ✗D11 |
| deleted SN (`C-202204159-N00003`) | 410 ✓ | 410 ✓ | 410 ✓ | 410 ✓ |
| fabricated ident | 404 ✓ | 404 ✓ | 404 ✓ | 404 ✓ |

Notes: `/map/<ident>` shares the gate (verified with the SN matrix); bare `/map` 200 since the fix wave (D01 fixed). Landing pages are SPA shells — status codes are gates; the element masking cells reflect the handle API data underneath (see handle grid).

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
| SN pr=C stav=1 B-owned | 403 body ✗D08 | 403 body ✗D08 (owner clause dead) | 403 body ✗D08 | 200 ✓ (full) |
| SN pr=C stav=4 public | 403 body ✗D08 | 403 body ✗D08 | 403 body ✗D08 | 200 ✓ (full) |
| deleted SN | tombstone ✓ | tombstone ✓ | tombstone ✓ | tombstone ✓ |

Notes: `oai_dc` is derived by XSLT after filtering — no leak (verified on pr=C akce); ListRecords carries tombstones and per-record 403 bodies like ListIdentifiers/GetRecord; versioned endpoints differ by XSLT only; Basic auth works on the OAI endpoint (invalid credentials → HTTP 401); identifiers must use the environment's own domain prefix.

### Search API (`/api/search/query`)

Visibility of restricted records in anonymous results is by design (public fields, masked protected fields). The grid records **element masking** on the restricted probe records.

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ no chranene block | ✓ no chranene block | ✓ full chranene block | ✓ full chranene block |
| adb restricted (`ADB-PRAH71-000861`) | ✓ block not served | ✓ block not served | ✓ block not served | ✓ block not served (chranene not projected on the search path for any role — suffixed-alias projection) |
| SN pr=C stav=4 | ✓ no chranene block | ✓ no chranene block | ✓ full chranene block | ✓ full chranene block |
| dokument pr=C | ✓ (no protected blocks by spec) | ✓ | ✓ | ✓ |
| ext_zdroj stav=1 | ✓ (no protected fields) | ✓ | ✓ | ✓ |
| pian pr=C (`P-2213-100119`) | ✓ block not served | ✓ | ✓ | ✓ (chranene not projected on the search path for any role — the geometry exposure routes are the map/geometry actions, see D07/VB) |
| projekt restricted (stav=2) | ✗D12 — **full `projekt_chranene_udaje` block served** | ✗D12 (block served to B as well) | ✓ block served — pr-legitimate (record pr=C) | ✓ block served — pr-legitimate |
| komponenta (SN `…-K001`) | ✗D11 — full `samostatny_nalez_chranene_udaje` served | ✗D11 (identical) | — (pr-legitimate: C ≥ record pr) | — |
| oznamovatel (any type) | ✓ not served by any backend surface | ✓ | ✓ | ✓ |
| uzivatel/heslo/ruian/organizace/osoba | · (search queries the entities core only) | · | · | · |

Notes: legacy `/api/search/id` gated since the fix wave (numFound 0 for non-searchable records, anonymous); `entity=dokumentacni_jednotka`/`dokument_cast` are pre-existing stubs; `D07` — anonymous `entity=vyskovy_bod&q=-stav:3` returns 2259 restricted-children docs with unsuffixed geometry (8 above pr=A).

### Handle API (`/api/search/handle?id=`)

| Type / probe state | anon | B | C | D |
| --- | --- | --- | --- | --- |
| akce pr=C (`C-202009490A`) | ✓ error/masked | ✓ masked | ✓ full | ✓ full |
| SN pr=C stav=4 | 403 ✗D06 | 403 ✗D06 | 403 ✗D06 | 200 ✓ |
| SN pr=C stav=1 B-owned | 401 ✓ | 200 ✓ (owner) | 200 ✓ | 200 ✓ |
| deleted SN | 410 ✓ | 410 ✓ | 410 ✓ | 410 ✓ |
| DJ/dokument_cast/VB (restricted/unarchived parents) | ✓ matching codes, no data | ✓ | ✓ | ✓ |
| komponenta (SN `…-K001`) | ✗D11 — chranene block served | ✗D11 | ✓ 200 (pr-legitimate: C ≥ record pr) | — |
| projekt stav=2 | 403 ✓ | 403 ✓ | 200 ✓ | 200 ✓ |

### File API (`/id/<ident>/file/<uuid>` and variants)

| Rule row / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| dokument pr=C stav=3 (`M-TX-202300441`, `M-TX-202100125`) — orig | 403 ✓ | 403 ✓ | 200 ✓ | — |
| └ `/thumb` | 200 ✓ (documented ungated) | — | — | — |
| └ `/thumb/page/1` | 403 ✓ | — | — | — |
| └ `/thumb-large` | **200 ✗D10** (full-size PNG) | — | — | — |
| └ `/paradata` | 403 ✓ | — | — | — |
| SN pr=C stav=4 (`C-202600010-N00085`) — orig | 200 ✓ (A rule) | 403 ✗D09 | 200 ✓ | 200 ✓ |
| └ `/thumb/page/1` | 200 ✓ | 403 ✗D09 | — | — |
| SN pr=C stav=1 B-owned (`C-202600009-N00014`) — orig | 403 ✓ | 200 ✓ (SN01 owner) | 200 ✓ | 200 ✓ |
| └ `/thumb-large` | 200 ✗D10 (restricted SN large thumb) | — | — | — |
| projekt stav=3 (`C-202600010`) — orig | 403 ✓ | 403 ✓ (never) | 403 ✓ (other org) | 200 ✓ |
| └ `/thumb` | 200 ✓ | — | — | — |
| cross-record uuid binding | 404 ✓ | — | — | — |
| rate limiter | 429 `Retry-After: 0` ✓ (interval branch) | — | — | — |

Notes: Basic auth accepted on the file path but **not** on the landing `checkId` path; landing-path requests under Basic auth are treated as anonymous; concurrent branch (`Retry-After: -1`) code-verified only. Test-data drift: the `M-TX-202300441` file uuid's `soubor_filepath` drifted to the `"neni"` placeholder mid-pass (recheck returns 404 for every role, including D) — the anon/B/C cells above were verified before the drift; the D cell is untestable on this record until the file binding is restored.

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
| `/pdf?id=<restricted dokument>&page=1` | — (no gate in source; test page-JPEG cache is cold — the file API's `/thumb/page/1` 404s even for D, so no real content is servable on test; see **D14**) | — | — | — |

Notes: `/img` keys on the **soubor id** (`soub-XXXXXX`), not the Fedora uuid — a uuid `id` returns 401 as a not-found, which is indistinguishable from a gate refusal on status alone; verify with the soubor id.

### Export and geometry (`/exp`, `/api/search/gml|wkt|geometrie|pians|mapa|export*`)

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/exp?entity=projekt&q=<restricted ident>` | ✓ no chranene block in the export field set (record discoverable — by-design visibility; export fields are public-only) | — | — | — |
| `/exp?entity=samostatny_nalez&mapa=true&geometrie=GeoJSON` (restricted) | ✓ `numFound=0` — record filtered out entirely for anonymous; no chranene, no error | — | — | — |
| `/exp?entity=pian&mapa=true` (restricted) | ✗ 500 (unhandled error page for anonymous on a restricted pian; no data leaked — robustness defect, not a leak) | — | — | — |
| `/api/search/geometrie` (restricted pian) | ✓ `{}` at the restricted pian's exact coordinates — the query runs on the role-suffixed `loc_rpt_A` field, which does not match restricted pians (the commented-out pristupnost check in source remains a latent risk if the suffixing is ever dropped) | — | — | — |
| `/api/search/pians` (restricted pian, `q=pristupnost:C`) | ✓ 193 restricted pians served — **idents and pian_id only, no coordinates** (location fields correctly absent) | — | — | — |
| `/api/search/gml`, `/wkt` | — | — | — | — |

### Admin and integration surfaces

| Surface / probe | anon | B | C | D |
| --- | --- | --- | --- | --- |
| `/fedora/get_id?id=<restricted projekt>` | ✗D13 — **full raw AMCR XML with `chranene_udaje`** (local-network/VPN position; public anonymous denied — operator-verified) | — | — | — |
| `/fedora/get_id?id=<restricted SN>` | ✗D13 — full raw XML with chranene (katastr, lokalizace, geometry) — same network-position scope | — | — | — |
| `/fedora/request?url=record/<ident>` | ✗D13 — raw Fedora container listing (walkable; same network-position scope) | — | — | — |
| `/mus/predmety_by_id`, `/mus/statistika` | ✓ reachable, empty test data (no demonstrable leak) | — | — | — |
| `/api/search/stats` | ✓ 200 public aggregates | — | — | — |
| `/fav/*` | ✗ 500 anonymous (ungated servlet but errors without a user; no data served) | — | — | — |
| `/api/search/stats_index` | ✓ 200 anonymous (index aggregates) | — | — | — |

### Findings registry

| ID | Severity | Surface | Status | One-line summary |
| --- | --- | --- | --- | --- |
| D01 | minor | bare `/map` | **fixed** | `/map` 500 → 200 forward to `/map2` |
| D02 | — | SN record rules | **changed** | fix-wave rewrite superseded by D06; owner clause verified live |
| D03 | High | legacy `/api/search/id` | **fixed** | anonymous leak of non-searchable records → numFound 0 |
| D04 | minor | file API rate limiter | partial | interval branch live-verified; concurrent branch code-only |
| D05 | High | child-record landing | **fixed on landing/handle**; search residual = D07 | DJ/dokument_cast/VB inherit parent rules again |
| D06 | High | SN record level | **open** | every SN landing/map/handle/OAI 403 for A/B/C |
| D07 | Medium-High | search API (vyskovy_bod) | **open** (pre-dates fix wave; confirmed on production) | restricted VB geometry anonymously searchable |
| D08 | High | OAI-PMH (SN) | **open** | SN OAI records D-only; owner/org clauses read absent oai-doc fields |
| D09 | Medium | file API | **open** | authenticated B denied a stav=4 file anonymous fetches |
| D10 | High | file API `/thumb-large` | **open** (test build only — production 404s the endpoint, 2026-09-10) | undocumented endpoint bypasses all permission checks |
| D11 | High | backend surfaces (komponenta) | **open** (test build only — production's old build has no komponenta docs in the entities core, 2026-09-10) | komponenta docs serve SN chranene block unmasked on search/handle/legacy |
| D12 | High | search API (projekt) | **open — active on production** (2026-09-10: 58 restricted projekts serve the chranene block anonymously on `digiarchiv.aiscr.cz`, sample `M-202400606`) | anonymous search serves full `projekt_chranene_udaje` for restricted projekts (B too) |
| D13 | High (reclassified from Critical) | `/fedora/*` | **open — local network/VPN only** (operator-verified 2026-09-10: the unauthenticated access is tied to the VPN/local-network position — the gate trusts the network; public-internet anonymous access denied) | unauthenticated raw-Fedora access from the local network — full raw record XML bypassing every permission layer |
| D14 | High | `/pdf` | **open** (code-verified; live demo blocked by cold page cache on test **and production** — production probed 2026-09-10, placeholder served for every probed dokument) | PDF page JPEGs served with no permission gate |

- **D06 (High, introduced by the 2026-09 fix wave):** every samostatny_nalez landing page (and its map variant, handle API, and OAI-PMH record body) returns 403 for A/B/C users, including public archived SNs (stav=4, any pristupnost); only D-E pass. Root cause: `SamostatnyNalez.filterOAI` compares the model **instance field** `stav` (default 0 on the fresh `getFedoraModel()` instances used by both `HandleServlet.checkId` and `OAIRequest.filter`) instead of the doc-derived value, so every `stav == 4` clause fails and A/B/C fall to the final `userPr >= D` else-branch. Regression against the 2026-08-28 verification. Also to reconcile when fixing: the C-branch creator-organisation clause checks `typ_zmeny='D01'` where SN creation is `SN01`, and the B branch carries a `pristupnost<=B` condition absent from #237's record-level rule.
- **D07 (Medium-High, pre-dates the fix wave — production shows the same exposure):** the anonymous search endpoint serves restricted výškový_bod documents with full data — 2259 VBs of restricted (non-archived-state) ADBs are `searchable=true` (inherited from the always-searchable ADB) and expose the **unsuffixed** `vyskovy_bod_geom_wkt`/`vyskovy_bod_geom_gml` fields, including 8 with pristupnost above anonymous level. The landing page for the same VBs is correctly 403; only the search surface leaks. Fix direction: parent-derived `searchable` for VB docs and/or pristupnost-suffixed geometry fields.
- **D08 (High, OAI-PMH surface):** OAI-PMH serves **every** samostatny_nalez record as the whole-record 403-Forbidden body to A/B/C — including record owners and public stav=4 records; only D-E receive content. Two independent root causes: (1) the `oai` core documents (`FedoraHarvester.createOAIDocument`) do not carry the `historie` JSON array, `samostatny_nalez_projekt`, or `samostatny_nalez_predano_organizace` fields that `SamostatnyNalez.filterOAI`'s B/C clauses read, so on the OAI path the owner and organization clauses can never pass; (2) the same instance-`stav` defect as D06 kills the `stav=4` branches. Live-verified: a maintainer-owned SN returns 200-for-owner on the landing but 403-for-owner and 403-for-C on OAI (D gets the full record); a public stav=4 SN returns the 403 body to anonymous on OAI. The landing fix must not stop at swapping `stav` for the doc value — the OAI document shape needs the fields the clauses read, or `filterOAI` must be given the entities-core doc on the OAI path.
- **D09 (Medium, file API):** an authenticated B user is denied a stav=4 SN file whose pristupnost exceeds B (403) while anonymous fetches the same file (200) — the file rule's B row (`stav=4 OR own (SN01)`) is not honored for logged-in users below the record's pristupnost. The SN01 owner clause does work on the file surface (B-owner 200 on a stav=1 pr=C file), and the same denial reproduces on the documented gated large-thumbnail endpoint. Likely cause: the logged-in branch of the file gate applies a pristupnost comparison without the stav=4 exemption the anonymous branch honors. Fix direction: apply the per-role file rules uniformly regardless of authentication.
- **D10 (High, file API, live-verified):** the undocumented `/thumb-large` endpoint (`/id/{ident_cely}/file/{file_id}/thumb-large`) bypasses **all** permission checks — `HandleServlet.isAllowed` returns true for any path containing `thumb` but not `page`, intended for the documented ungated small thumbnails, and the large-thumbnail variant also matches. Live-verified on a restricted dokument file (pristupnost C, stav=3): anonymous receives 403 on the original and on the documented gated large thumbnail `/thumb/page/{page}`, but **200 with the full-size PNG** on `/thumb-large`; the same bypass serves the restricted B-owned SN's large thumbnail anonymously. The public documentation describes only three endpoints (`/thumb`, `/thumb/page/{page}`, original) with only small thumbnails ungated. Fix direction: gate `thumb-large` like `thumb/page` (the `!contains("page")` early-return must not cover it) and document or remove the `paradata` variant (verified gated).
- **D11 (High, backend search/handle/legacy surfaces and landing gate, live-verified):** komponenta child documents bypass all pristupnost masking. `Komponenta.filterOAI` returns true unconditionally, and the search API, the handle API, and the legacy `/api/search/id` endpoint serve the komponenta doc including the copied `samostatny_nalez_chranene_udaje` block (lokalizace, katastr, geometry in EPSG:4326 and 5514) with **no pristupnost masking** — the parent SN masks that block on every other surface, including the same endpoints' parent responses (verified on the same record). Live-verified on two SN komponentas (pr=C, stav=4): full chranene_udaje to anonymous, identical for B. Scale: 3,397 SN-derived komponenta docs with pristupnost above A are anonymously visible with the chranene block (4,802 komponenta docs above A in total; DJ-derived komponentas carry no chranene block). Same defect family as D07 (child doc inherits `searchable`, secured JSON block unsuffixed) but a distinct entity and surface set, and the exposed payload is the full protected block. The landing gate is consistent with the leak: komponenta pages return 200 for every role. Fix direction: pristupnost-gate the komponenta entity in the search/handle paths (or stop copying `samostatny_nalez_chranene_udaje` into komponenta docs / suffix it), and give `Komponenta.filterOAI` parent-derived rules like the other child types.
- **D12 (High, search API, live-verified on test and production):** the search API serves the full `projekt_chranene_udaje` block (lokalizace, parcelni_cislo, exact geometry, katastr) for restricted projekt records — a stav=2 projekt whose landing page is 403 for anonymous and B (A/B rule: stav=6) returns `numFound=1` with the complete unsuffixed chranene JSON block, **for anonymous and B alike** (live-verified on two restricted projekts; C/D see the same block legitimately — record pr=C — so the defect is confined to below-pr leakage). Oznamovatel is **not** served (verified absent). **Active on production from the public internet** (2026-09-10: 58 restricted projekts anonymously serve the block on `digiarchiv.aiscr.cz`, sample `M-202400606`) — the defect pre-dates the fix wave. Scale on test: 62 projekts with pristupnost above A are anonymously visible with the chranene block. The projekt state rule is not applied in search at all (record discoverable regardless of stav). Root cause narrowed by the source audit: the projekt search field list projects `projekt_chranene_udaje:[json]` **unsuffixed** (alongside suffixed aliases like `projekt_chranene_udaje_hlavni_katastr_A`), and although `ProjektSearcher.filter` contains the removal clause (`docPr > pristupnost → doc.remove("projekt_chranene_udaje")` with `pristupnost` present in the projected fields), the block is still served on the query path — the filter is evidently not effective there (compare akce/adb/SN, whose blocks are correctly absent for anonymous/B). Fix direction: stop projecting the unsuffixed block on the search path, or make the filter actually run/effect on `QUERY`, like the other entities.
- **D13 (High, reclassified from Critical, `/fedora/*` admin surface, live-verified on test and production — from the local-network/VPN position):** the FedoraServlet admin surface serves the **complete raw AMCR XML** of any record to **unauthenticated clients on the local network / VPN** — the operator verified 2026-09-10 that the exposure is tied to the network position (VPN): the servlet's gate (`allowedIP || localhost || pristupnost >= indexSecLevel`, default E) trusts the source address, and public-internet anonymous access is denied. Probed evidence (all from the trusted network position): `GET /fedora/get_id?id=<ident>` returned the complete raw AMCR XML on test (restricted projekt stav=2; restricted SN pr=C stav=4 — full `chranene_udaje`: katastr, lokalizace, exact geometry; records with oznamovatel would expose the raw PII equally) and on production (restricted projekt pr=C, ≈29 kB with the full chranene block) — no record-level rule, no element masking, no state rule applies, because this is the raw Fedora object beneath every filtered surface. `GET /fedora/request?url=record/<ident>` walks raw Fedora containers; the servlet also exposes reindex actions. Remaining risk is internal: any VPN/local client without login reads every record's raw XML. Fix direction (defense in depth): require authentication even from trusted networks (or narrow `allowedIP` and document the exception as a deliberate decision); verify `allowedIP` contains neither proxy addresses nor broad VPN client ranges.
- **D14 (High, `/pdf`, code-verified; probed on test and production):** `PdfServlet` (`GET /pdf?id=<ident>&page=N`) serves the pre-generated, watermarked page-JPEG cache with **no permission gate of any kind** — no pristupnost, no `filterOAI`, no `ImageAccess`. Live demonstration is blocked only because the page-JPEG cache is cold on every probed record: on test the gated file-API `/thumb/page/1` 404s even for D, and on production (probed 2026-09-10) `/pdf` returns the identical 7,525 B `empty_big.png` placeholder for restricted and public, old and fresh dokuments alike (M-TX-202600597, M-TX-202600434, C-TX-202600748, M-TX-193001369) — the endpoint answers 200 anonymously but no real content was servable. The same cache serves the gated endpoint's content, so any deployment with a warm cache would serve restricted dokument pages to anonymous. Fix direction: apply the file-API rules (or at least the dokument XPath rule) in `PdfServlet` before reading the cache. Warm-cache recheck stands as an operator follow-up.

### Pre-existing observations

- The `dokumentacni_jednotka` and `dokument_cast` entity-search endpoints are stubs and return error bodies — pre-existing, unrelated to permissions, no data exposure.
- Test-data note: the `archivar.ai@arup.cas.cz` test account initially reported pristupnost B; the operator corrected it to D mid-run.
- Quoted-phrase Solr interpolation (code-verified, low): `HandleServlet.getDocument`'s `soubor_filepath:"<url-path>"` filter and OAIRequest's `ident_cely:"<id>"` interpolate the request path/identifier without escaping query metacharacters (`InitServlet.asSafePath` blocks traversal and backslashes only). No privilege bypass found — the permission gate still runs on whatever document the query returns — but the construction is injection-shaped; escaped terms would remove the class.
- The `oai` core lags the `entities` core for freshly created records: a user record created days before the run returns `idDoesNotExist` on OAI while being present in entities — freshness note for harvesters, and the reason the uzivatel my-record clause could not be live-checked for the test accounts.
- Test-data quality: several records carry placeholder file paths (`"neni"`) and omit `soubor/path`+`soubor/url` elements in OAI output; well-formed records (verified via the D view) do serve both. Probes used well-formed records — do not read the omission as build behaviour.

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
