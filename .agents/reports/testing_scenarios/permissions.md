# Permission model — testing scenario

**Key:** permissions (feature)
**Scope:** the permission behaviour of persistent landing pages (`/id/`), the map variant (`/map/`), child-record landing pages, the File API, and OAI-PMH parity; issues [ARUP-CAS/aiscr-digiarchiv-2#370](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/370) and [#237](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/237) (rules definition) drove it.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills. Never reuse an id across environments or sessions without re-verifying it.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Landing page | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz/id/<ident_cely>` | `https://api-test.aiscr.cz/2.2/oai` | has #370 (status codes, login-gated non-archived records) |
| Production | `https://digiarchiv.aiscr.cz/id/<ident_cely>` | `https://api.aiscr.cz/2.2/oai` | old build: `/id/` always HTTP 200 shell, gating happens client-side |

OAI quirks: on the test instance, OAI identifiers use the **`https://api-test.aiscr.cz/id/…`** prefix (GetRecord with the production prefix silently fails with `idDoesNotExist`); set names containing a colon must be percent-encoded (`set=dokument%3A3d`).

### Architecture and implementation facts

- **Solr core routing decides landing-page existence.** `HandleServlet.checkId` queries the **`entities`** core. Top-level types there: `projekt`, `archeologicky_zaznam` (akce/lokalita), `dokument` (incl. 3D), `adb`, `pian`, `samostatny_nalez`, `ext_zdroj`, `let`. Child entities there: `dokumentacni_jednotka`, `komponenta`, `komponenta_dokument`, `dokument_cast`, `vyskovy_bod`, `neident_akce` (see the model below). Types `uzivatel`, `heslo`, `ruian_*`, `organizace`, `osoba` index into their own cores → **always 404 on landing pages** (intended feature). The **`oai`** core serves OAI-PMH for all types, so OAI-PMH can serve records the landing page cannot.
- **Landing-page decision tree** (`HandleServlet.checkId`, `HandleServlet.java` L491 on `dev`): no doc in entities → **404**; `is_deleted` → **410**; `!searchable` and anonymous → **401**; per-model `filterOAI(user, doc)` fails → **403**; else 200. The rendered page is a client-side shell (~43 KB, no data); the browser then fetches `…/api/search/handle?id=<ident>`, which returns `{"error":"<code>"}` when gated. Bilingual error pages (CS+EN) are returned directly by the servlet for non-200 codes.
- **Where the rules live:** record-level rules in `FedoraModel.filterOAI` implementations (`web4/fedora/models/*.java`); file rules in `HandleServlet.isAllowed`; element-level hiding in `OAIRequest.filter` (OAI) and via pristupnost-suffixed secured fields (search/page).
- **`/map/<ident>`** is mapped to the same `HandleServlet` (web.xml) and applies the identical 401/403/404 gating before forwarding to `/map2`. Bare `/map` 500s (see the current verification's defects).
- **Rate limiting:** File API requests are tracked per IP + file; keep **≥ 1 s between requests**; a concurrent duplicate request gets **429** (the "still in progress" branch sends no `Retry-After` header — minor gap).
- **Page cache:** on test (`isTestEnv=true`) the landing-page cache is skipped entirely; on production a 1-day anonymous cache exists but only affects the rendered shell (record data is always fetched live per user), so it has no permission impact.

### Feature or entity model

**Roles / pristupnost:** A anonym, B badatel, C archeolog, D archivář, E admin/ARÚ (E is also granted synthetically to users with `cteni_dokumentu`). Heslar ids: `HES-000865`=A … `HES-000868`=D. Comparison is lexicographic on the letter (A < B < C < D < E).

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

**`searchable` flag (drives anonymous 401 vs 403):** AZ = stav 3; dokument = stav 3; samostatny_nalez = stav 4; pian = ident does not start with `N`; projekt = has related dokument/samostatny_nalez/akce; adb, let, ext_zdroj = always searchable; uzivatel = field not set (moot, 404 anyway); **child entities** — komponenta inherits the parent's state; the other child entities were indexed `searchable=true` at the time of the verified run (see the child-record bypass defect).

**Element-level (inside a 200 response):**

- `chranene_udaje` (katastr, lokalizace, geometrie) hidden when record pristupnost > user pristupnost. Applies to projekt, archeologicky_zaznam, adb, pian, samostatny_nalez; **dokument and ext_zdroj have no element-level restriction** by spec.
- `projekt/oznamovatel`: A-B never; C only stav=1 or own organisation; D-E always.
- In OAI-PMH responses, restricted elements are replaced by the literal `HTTP/1.1 403 Forbidden`; restricted whole records get `<amcr:amcr…>HTTP/1.1 403 Forbidden</amcr:amcr>`; deleted records carry `status="deleted"` headers.
- An entity's deviation from this mechanism is recorded in that entity's scenario (for komponenta, see the [`komponenta`](komponenta.md) scenario).

**File API** (`https://…/id/<ident>/file/<uuid>[/thumb][/thumb/page/N]`, Basic auth only; see <https://arup-cas.github.io/aiscr-api-home/file-api/>):

| XPath | A | B | C |
| --- | --- | --- | --- |
| `//projekt/soubor` | never | never | stav=1 OR (stav 2-6 AND own org) |
| `//dokument/soubor` | pr=A AND stav=3 | (pr<=B AND stav=3) OR own (D01) | (pr<=C AND stav=3) OR own org |
| `//samostatny_nalez/soubor` | stav=4 | stav=4 OR own (SN01) | + org (predano/projekt) |

Small thumbnails (`/thumb`) are **always public**; large thumbnails (`/thumb/page/N`) follow the original-file rules; PDF page thumbnails additionally require the pre-generated page-JPEG cache (cold on test at the time of the verified run — images work, PDFs 404 after passing the gate; production serves them).

**Child records:** child entities live in the entities core with their own idents derived from the parent: `dokumentacni_jednotka` = `<AZ>-D01…`; `komponenta` (under AZ or SN) = `<parent>-K001…`; `dokument_cast` = `<dokument>-D001…`; `vyskovy_bod` = `<ADB>-V0001…`.

### Discovery recipes

Do not rely on ids noted by an earlier session — re-discover per session. All steps work anonymously.

1. **Enumerate and classify records:** `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` (100 records/page, resumptionToken pagination). Classify each record by regex on the body: whole-record restriction `(?s)>\s*HTTP/1\.1 403 Forbidden\s*</amcr:amcr>` (record-level restricted for anonymous); element restriction `<amcr:chranene_udaje>HTTP/1\.1 403 Forbidden`; deleted header `status="deleted"`. Visible records expose `<amcr:stav>` and `<amcr:pristupnost … id="HES-…">` (id → A/B/C/D) for precise classification. `from`/`until` datestamp windows slice the stream; on production the sort puts newly deleted records early.
2. **Predict 401 vs 403:** `GET /api/search/query?entity=<e>&q=ident_cely:"<ident>"` — `numFound: 0` while the record exists in OAI ⇒ not searchable ⇒ anonymous landing returns **401**; `numFound > 0` while OAI shows the record restricted ⇒ **403**.
3. **Visible baselines / element ladder:** from the same OAI scan, take records with the target `stav` (rule-satisfying) and the desired pristupnost level (A = open baseline; B/C/D = element hidden up to that role). For the pr=D element case pick a record whose pristupnost id is `HES-000868`.
4. **404 candidates:** fabricate idents with valid prefixes but nonexistent numbers (e.g. bump an existing ident's numeric tail far out of range). Expect 404 for all roles.
5. **410 candidates:** deleted records are rare and environment-specific; scan sets for `status="deleted"` headers (`uzivatel` and `osoba` sets had them on test — but those types 404 on landing), or delete a disposable record in webamcr-test and wait for reindex.
6. **File-API candidates:** file UUIDs come from the OAI `soubor` elements of **visible** records (`<amcr:url>`, `<amcr:path>`; some records have `soubor` without a Fedora path — skip those). For files of restricted records, the legacy endpoint listed under the current verification's defects returns their metadata (including file paths) to anonymous callers. Coverage per access-rule row: dokument files are plentiful; SN files are plentiful; projekt files may not exist in the test data at all — file a fresh oznámení in webamcr-test if needed.
7. **Child-record candidates:** derive from parent metadata — OAI `archeologicky_zaznam` records list `dokumentacni_jednotka` idents; `adb` records list `vyskovy_bod` idents; `dokument` records do not list casts, but cast/komponenta idents follow the fixed suffix patterns (see the model above). Also `/api/search/query?entity=dokumentacni_jednotka|komponenta|dokument_cast|vyskovy_bod` (with an ident filter) confirms existence.
8. **Cross-environment comparison:** run the same probes against production; remember data drift — never assume the same ident has the same state in both environments.
9. **Child-record gating checks:** take a restricted parent, append the child suffixes, compare the parent's code with each child's; verify data exposure via `/api/search/handle?id=<child>`.

### Verification commands

```bash
# landing-page status code
curl -s -o /dev/null -w "%{http_code}\n" https://digiarchiv-test.aiscr.cz/id/<IDENT>
# map variant (same gating)
curl -s -o /dev/null -w "%{http_code}\n" https://digiarchiv-test.aiscr.cz/map/<IDENT>
# OAI-PMH record (anonymous = restricted view; add -u user:pwd for role view)
curl "https://api-test.aiscr.cz/2.2/oai?verb=GetRecord&identifier=https%3A%2F%2Fapi-test.aiscr.cz%2Fid%2F<IDENT>&metadataPrefix=oai_amcr"
# searchability / search behaviour
curl "https://digiarchiv-test.aiscr.cz/api/search/query?entity=<ENTITY>&q=ident_cely%3A%22<IDENT>%22&rows=1"
# data actually served to the landing page (element-level / child-leak checks)
curl "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<IDENT>"
# File API (>= 1s between requests; Basic auth for role tests)
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb"
curl "https://digiarchiv-test.aiscr.cz/id/<IDENT>/file/<UUID>/thumb/page/1"
```

## Current verification (2026-08-28, `digiarchiv-test` build with #370; production build without #370)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| `/results` search regression | page 200; all nine entity queries return sensible counts; facets populated |
| Restricted records absent for anonymous (spot checks) | holds; visible records present with correct `stav` |
| Full text via `q=text_all_A:<term>` | works (the raw `text=` API param is ignored by the backend in both builds — the UI builds `q` itself; verify full text once in the browser) |
| Export endpoint | hand-made params return 200 (`text/csv` header, JSON body) on test vs 500 on production — verify the export button in the UI rather than the raw endpoint |
| komponenta children gated like their parent | works — searchable inherited from the parent's state (401 observed for a restricted parent; 200 for an open one; the entity's own deviations are in the [`komponenta`](komponenta.md) scenario) |
| `dokumentacni_jednotka`, `dokument_cast`, `vyskovy_bod` children gated like their parent | **fails** — child-record bypass, see permissions-D05 |

### Known defects

- permissions-D01: bare `/map` → 500 (null `pathInfo` NPE in `HandleServlet.processRequest`; web.xml maps `/map/*` there). Production returns 200.
- permissions-D02: SN own-record/org rules never fire on landing pages: `checkId` fetches only `entity,is_deleted,searchable,stav` (`HandleServlet.java:491`) while `SamostatnyNalez.filterOAI` reads absent fields; additionally `historie_typ_zmeny`/`historie_uzivatel` index the **latest** history entry, not the SN01 entry. The File-API path (`getDocument`, L548) fetches the right fields and parses the full history, so file rules work there.
- permissions-D03: legacy `/api/search/id` returns non-archived records with `chranene_udaje` to anonymous callers (production build returned `numFound: 0`). The frontend's `/api/search/handle` is correctly gated.
- permissions-D04 (minor): 429 "still in progress" response lacks the `Retry-After` header documented for the File API; PDF page-thumbnail cache cold on test (images fine); test-data gaps (projekt files without indexed paths; many dokument `soubor` entries without Fedora paths).
- permissions-D05 (**child-record bypass**): `dokumentacni_jednotka`, `dokument_cast`, and `vyskovy_bod` landing pages return 200 with data (incl. PIAN coordinates, VB geometry) for children of 401/403-restricted parents. Their model `filterOAI` implementations return `true` unconditionally and their docs are indexed with `searchable=true`. `komponenta` is gated correctly; the parity fix should propagate the parent's state/searchability to all child entities. Child idents must not be treated as a safe surface until fixed.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-28 | `digiarchiv-test` (build with #370), production (build without #370) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
