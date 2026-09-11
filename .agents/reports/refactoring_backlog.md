# Refactoring backlog — Digitální archiv AMČR (aiscr-digiarchiv-2)

> Review prose follows the canonical English-default rule; verbatim Czech is preserved where field names or AIS CR domain identifiers matter.
> Structural improvements discovered during the audit.
>
> **Note on priority vs. severity:** The priority sections (High / Medium / Low)
> reflect the **refactoring priority** (impact on architecture and business logic) and
> may differ from the **bug severity** in `bugs.md`, which reflects the technical risk
> of the defect itself. Both values are intentional and use a different evaluation frame.

---

## High Priority

<!-- Architectural problems, security debt, Solr performance -->

### [T03] SOLR-003: BoolField without docValues — slower filtering on is_deleted, searchable, akce_je_nz
- **Description:** The `BoolField` type in the entities, uzivatel, and organizations managed-schema has no `docValues=true`. Boolean filters (`-is_deleted:true`, `searchable:true`) are invoked in every query (SearchUtils.java, LogAnalytics.java, ImageServlet.java). Without docValues, Solr uses the slower FieldCache.
- **Impact:** Increased filtering latency; especially noticeable on collections with millions of records.
- **Recommendation:** Add `docValues="true"` to `<fieldType name="boolean">` in all affected managed-schema files. Requires a schema reload, not a full reindex (docValues will be updated incrementally).

### [T03] SOLR-004: FedoraHarvester.indexModels() without cursor pagination — OOM risk
- **Description:** `max_results=10000000` without pagination loads the entire repository into memory. `checkDatestamp()` in the same class correctly uses CursorMark.
- **Impact:** A full reindex may cause an OOM and crash the application server.
- **Recommendation:** Refactor `indexModels()` to cursor-based iteration (the pattern from `checkDatestamp()`).

### [T03] SOLR-001: Typo multiValued="fslse" in the entities schema
- **Description:** `solr/entities/conf/managed-schema:157` — the field `samostatny_nalez_projekt` has a typo in a boolean attribute.
- **Impact:** Undefined behavior when indexing a samostatný nález record.
- **Recommendation:** Fix to `multiValued="false"` and reindex.

### [T03] SOLR-002: luceneMatchVersion=9.4 vs. solr-solrj 9.10.1 — configuration mismatch
- **Description:** All 14 `solrconfig.xml` files have `luceneMatchVersion=9.4`, the client is 9.10.1.
- **Impact:** Older Lucene behavior; the 9.5–9.10 version optimizations are missing.
- **Recommendation:** Synchronize `luceneMatchVersion` with the Solr server version; full reindex.

### [T02] ARCH-02: Saxon 8.7 — critically outdated XSLT processor
- **Description:** `net.sf.saxon:saxon:8.7` (released 2006) — the current version is Saxon-HE 12.x. About 18 years of security patches are missing.
- **Impact:** Security risk; Saxon 8.7 does not support XSLT 3.0 (conflict with AGENTS.md).
- **Recommendation:** Upgrade to `net.sf.saxon:Saxon-HE:12.5`; verify compatibility of the existing XSLT.

### [T02] ARCH-01: God classes: SolrSearcher (1,048 lines) and FedoraHarvester (1,024 lines)
- **Description:** Both classes have excessive responsibilities (query building, access control, HTTP communication, data transformation all together).
- **Impact:** Low testability, high change complexity, regression risk.
- **Recommendation:** Extract responsibilities into specialized classes (Strategy/Command pattern).

### [T02] ARCH-10: Absence of automated tests
- **Description:** The `web/src/test` directory does not exist. The project has no automated Java tests.
- **Impact:** Regressions cannot be detected automatically; the CI pipeline cannot verify code correctness.
- **Recommendation:** Add unit tests for SearchUtils, IndexUtils, FormatUtils, FedoraUtils. Consider integration tests for Solr queries.

### [T03] SOLR-005: SolrSearcher.getAllKatastry() — setRows(100000) without overflow verification
- **Files:** `web/src/main/java/cz/inovatika/arup/digiarchiv/web4/index/SolrSearcher.java:566`
- **Description:** Silent truncation with >100000 cadastres. The entire HashMap in memory without TTL.
- **Recommendation:** Verify numFound <= rows; add a cursor or explicit limit with logging
- **Severity:** High

## Medium Priority

<!-- Optimizations, XSLT refactoring, Docker build -->

### [T03] SOLR-009: setRows(10000) without pagination in the Searcher classes
- **Description:** DokumentSearcher (2×), AkceSearcher, ProjektSearcher (5×), PIANSearcher, LokalitaSearcher — all call `setRows(10000)` without an overflow check.
- **Impact:** Memory problems; silent truncation of results with a larger database.
- **Recommendation:** Cursor-based iteration or explicit pagination.

### [T03] SOLR-006: rdate (DateRangeField) without docValues on project dates
- **Description:** projekt_datum_zahajeni, projekt_datum_ukonceni, projekt_datum_provedeni are `rdate`. DateRangeField does not support docValues.
- **Impact:** Cannot sort or facet by these dates.
- **Recommendation:** Add duplicate pdate fields for sort/facet; keep rdate for range queries.

### [T03] SOLR-008: oai/conf/managed-schema.xml — non-standard file extension
- **Description:** The other collections have `managed-schema` without an extension, oai has `managed-schema.xml`.
- **Impact:** Potential problem with the managed schema API.
- **Recommendation:** Rename to `managed-schema`.
- **Supersedes:** Missing managed-schema for the Solr collection `oai`

### [T03] SOLR-010: copyField source="*" in the heslar and soubor collections
- **Description:** The wildcard copyField copies numeric fields and dates into the fulltext field as well.
- **Impact:** Unnecessary data duplication in the index; higher memory usage.
- **Recommendation:** Explicit list of source fields.

### [T02] ARCH-03: javax.mail in a Jakarta EE 11 project
- **Description:** `javax.mail:mail:1.4.7` uses the old `javax` namespace; Jakarta EE 11 expects `jakarta.mail`.
- **Impact:** Possible classloading conflict on a modern application server (Tomcat 11+, WildFly 27+).
- **Recommendation:** Replace with `jakarta.mail:jakarta.mail-api` + the Angus Mail implementation.

### [T02] ARCH-04: EOL dependency: Apache XML-RPC
- **Description:** `org.apache.xmlrpc:xmlrpc-client:3.1.3` — end-of-life since 2016.
- **Impact:** No security updates; unnecessary dependency.
- **Recommendation:** Identify usages in the code and replace with `java.net.http`.

### [T02] ARCH-09: No parent POM or BOM
- **Description:** The versions of shared dependencies (`pdfbox`, `commons-io`, `org.json`, `solr-solrj`) are managed separately in both `web/pom.xml` and `ThumbnailsGenerator/pom.xml`.
- **Impact:** Manual version synchronization; risk of inconsistent versions during upgrades.
- **Recommendation:** Add a root parent POM with a `dependencyManagement` section.

### [T02] ARCH-05: ThumbnailsGenerator targets Java 1.8
- **Description:** `maven.compiler.source/target = 1.8`, the main application targets Java 17.
- **Impact:** Inconsistent runtime; Java 1.8 is EOL (no security patches from Oracle).
- **Recommendation:** Upgrade to Java 17.

### [T02] ARCH-11: Duplicate HTTP clients in the main application
- **Description:** `org.apache.httpcomponents:httpclient:4.5.14` in Maven + `java.net.http` in the code.
- **Impact:** Unnecessary dependency inflates the WAR; two different HTTP communication paradigms.
- **Recommendation:** Standardize on `java.net.http`.

### [T01] SOLR-012: Duplicated solrconfig.xml in 10 collections
- **Description:** 10 of the 14 Solr collections share an identical `solrconfig.xml` (SHA-256: `5ce00b28...`).
- **Impact:** Every change must be propagated to 10 files manually.
- **Recommendation:** Consider Solr configset inheritance or generating the configuration in CI.

### [T01] DOCKER-01: Missing Docker/infrastructure configuration in the repository
- **Description:** No Dockerfile or docker-compose files in the repository.
- **Impact:** Running the application locally is not possible without external resources; complicates onboarding.
- **Recommendation:** Add a development `docker-compose.yml` or document a link to the infrastructure repository.

### [T03] SOLR-007: az_dj_nazev: commented out in the schema (type=string) vs. active field (type=text_general)
- **Files:** `solr/entities/conf/managed-schema:116 vs. :342`
- **Description:** Suggests incomplete refactoring — a commented-out string field and an active text_general field.
- **Recommendation:** Remove the commented-out fragment; document the type change
- **Severity:** Medium

## Low Priority

<!-- Cosmetic changes, documentation, minor code quality -->

### [T02] ARCH-06: Axios redundant alongside Angular HttpClient
- **Description:** `axios:^1.11.0` in the npm dependencies — duplicates Angular HttpClient.
- **Recommendation:** Verify usage; migrate to Angular HttpClient and remove axios.

### [T02] ARCH-07: Moment.js in maintenance mode
- **Description:** `moment:^2.30.1` — Moment.js has been in maintenance mode since 2020.
- **Recommendation:** Migrate to `date-fns` or the native Angular `DatePipe`.

### [T02] ARCH-08: Version range for thumbnailator
- **Description:** `net.coobird:thumbnailator:[0.4,0.5)` — non-deterministic build.
- **Recommendation:** Pin a specific version (e.g. `0.4.20`).

### [T02] ARCH-12: @ngu/carousel version for Angular 20 in an Angular 21 application
- **Description:** `@ngu/carousel:^20.0.1` — carousel for Angular 20, the application is Angular 21.
- **Recommendation:** Upgrade to a version compatible with Angular 21 or replace it.

### [T02] ARCH-13: Duplicate path validation in ApiServlet and InitServlet
- **Description:** Both classes implement their own path normalization.
- **Recommendation:** Extract into a shared utility class `PathUtils`.

### [T01] ARCH-14: Typo in a Java package name: `imagging`
- **Description:** The package `cz.inovatika.arup.digiarchiv.web4.imagging` contains a typo.
- **Impact:** Confusing for developers, visible in imports and documentation.
- **Recommendation:** Rename the package to `imaging` (IDE refactor → rename package).

### [T01] ARCH-15: Inconsistent base package in ThumbnailsGenerator
- **Description:** ThumbnailsGenerator uses `cz.incad.arup`, the main application uses `cz.inovatika.arup`.
- **Impact:** Makes it harder to navigate the codebase; suggests the module has a historically different origin.
- **Recommendation:** Unify the base package (on the occasion of a larger refactoring of the module).

### [T03] SOLR-011: Sensitive fields email and telefon stored as plaintext in the uzivatel collection
- **Files:** `solr/uzivatel/conf/managed-schema:125-126`
- **Description:** The email and telefon fields are indexed=true, stored=true. Access to the Solr admin interface allows extraction of all contacts.
- **Recommendation:** Restrict access to the uzivatel collection; consider field-level security
- **Severity:** Low
