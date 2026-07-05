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

### [T03] BoolField without docValues — slower filtering on is_deleted, searchable, akce_je_nz
- **Finding:** The `BoolField` type in the entities, uzivatel, and organizations managed-schema has no `docValues=true`. Boolean filters (`-is_deleted:true`, `searchable:true`) are invoked in every query (SearchUtils.java, LogAnalytics.java, ImageServlet.java). Without docValues, Solr uses the slower FieldCache.
- **Impact:** Increased filtering latency; especially noticeable on collections with millions of records.
- **Proposal:** Add `docValues="true"` to `<fieldType name="boolean">` in all affected managed-schema files. Requires a schema reload, not a full reindex (docValues will be updated incrementally).

### [T03] FedoraHarvester.indexModels() without cursor pagination — OOM risk
- **Finding:** `max_results=10000000` without pagination loads the entire repository into memory. `checkDatestamp()` in the same class correctly uses CursorMark.
- **Impact:** A full reindex may cause an OOM and crash the application server.
- **Proposal:** Refactor `indexModels()` to cursor-based iteration (the pattern from `checkDatestamp()`).

### [T03] Typo multiValued="fslse" in the entities schema
- **Finding:** `solr/entities/conf/managed-schema:157` — the field `samostatny_nalez_projekt` has a typo in a boolean attribute.
- **Impact:** Undefined behavior when indexing a samostatný nález record.
- **Proposal:** Fix to `multiValued="false"` and reindex.

### [T03] luceneMatchVersion=9.4 vs. solr-solrj 9.10.1 — configuration mismatch
- **Finding:** All 14 `solrconfig.xml` files have `luceneMatchVersion=9.4`, the client is 9.10.1.
- **Impact:** Older Lucene behavior; the 9.5–9.10 version optimizations are missing.
- **Proposal:** Synchronize `luceneMatchVersion` with the Solr server version; full reindex.

### [T02] Saxon 8.7 — critically outdated XSLT processor
- **Finding:** `net.sf.saxon:saxon:8.7` (released 2006) — the current version is Saxon-HE 12.x. About 18 years of security patches are missing.
- **Impact:** Security risk; Saxon 8.7 does not support XSLT 3.0 (conflict with AGENTS.md).
- **Proposal:** Upgrade to `net.sf.saxon:Saxon-HE:12.5`; verify compatibility of the existing XSLT.

### [T02] God classes: SolrSearcher (1,048 lines) and FedoraHarvester (1,024 lines)
- **Finding:** Both classes have excessive responsibilities (query building, access control, HTTP communication, data transformation all together).
- **Impact:** Low testability, high change complexity, regression risk.
- **Proposal:** Extract responsibilities into specialized classes (Strategy/Command pattern).

### [T02] Absence of automated tests
- **Finding:** The `web/src/test` directory does not exist. The project has no automated Java tests.
- **Impact:** Regressions cannot be detected automatically; the CI pipeline cannot verify code correctness.
- **Proposal:** Add unit tests for SearchUtils, IndexUtils, FormatUtils, FedoraUtils.
  Consider integration tests for Solr queries.

## Medium Priority

<!-- Optimizations, XSLT refactoring, Docker build -->

### [T03] setRows(10000) without pagination in the Searcher classes
- **Finding:** DokumentSearcher (2×), AkceSearcher, ProjektSearcher (5×), PIANSearcher, LokalitaSearcher — all call `setRows(10000)` without an overflow check.
- **Impact:** Memory problems; silent truncation of results with a larger database.
- **Proposal:** Cursor-based iteration or explicit pagination.

### [T03] rdate (DateRangeField) without docValues on project dates
- **Finding:** projekt_datum_zahajeni, projekt_datum_ukonceni, projekt_datum_provedeni are `rdate`. DateRangeField does not support docValues.
- **Impact:** Cannot sort or facet by these dates.
- **Proposal:** Add duplicate pdate fields for sort/facet; keep rdate for range queries.

### [T03] oai/conf/managed-schema.xml — non-standard file extension
- **Finding:** The other collections have `managed-schema` without an extension, oai has `managed-schema.xml`.
- **Impact:** Potential problem with the managed schema API.
- **Proposal:** Rename to `managed-schema`.

### [T03] copyField source="*" in the heslar and soubor collections
- **Finding:** The wildcard copyField copies numeric fields and dates into the fulltext field as well.
- **Impact:** Unnecessary data duplication in the index; higher memory usage.
- **Proposal:** Explicit list of source fields.

### [T02] javax.mail in a Jakarta EE 11 project
- **Finding:** `javax.mail:mail:1.4.7` uses the old `javax` namespace; Jakarta EE 11 expects `jakarta.mail`.
- **Impact:** Possible classloading conflict on a modern application server (Tomcat 11+, WildFly 27+).
- **Proposal:** Replace with `jakarta.mail:jakarta.mail-api` + the Angus Mail implementation.

### [T02] EOL dependency: Apache XML-RPC
- **Finding:** `org.apache.xmlrpc:xmlrpc-client:3.1.3` — end-of-life since 2016.
- **Impact:** No security updates; unnecessary dependency.
- **Proposal:** Identify usages in the code and replace with `java.net.http`.

### [T02] No parent POM or BOM
- **Finding:** The versions of shared dependencies (`pdfbox`, `commons-io`, `org.json`, `solr-solrj`) are managed separately in both `web/pom.xml` and `ThumbnailsGenerator/pom.xml`.
- **Impact:** Manual version synchronization; risk of inconsistent versions during upgrades.
- **Proposal:** Add a root parent POM with a `dependencyManagement` section.

### [T02] ThumbnailsGenerator targets Java 1.8
- **Finding:** `maven.compiler.source/target = 1.8`, the main application targets Java 17.
- **Impact:** Inconsistent runtime; Java 1.8 is EOL (no security patches from Oracle).
- **Proposal:** Upgrade to Java 17.

### [T02] Duplicate HTTP clients in the main application
- **Finding:** `org.apache.httpcomponents:httpclient:4.5.14` in Maven + `java.net.http` in the code.
- **Impact:** Unnecessary dependency inflates the WAR; two different HTTP communication paradigms.
- **Proposal:** Standardize on `java.net.http`.

### [T01] Missing managed-schema for the Solr collection `oai`
- **Finding:** `solr/oai/conf/` does not contain `managed-schema` (the other collections have it).
- **Impact:** Unclear collection schema; risk of unexpected behavior during indexing.
- **Proposal:** Verify whether it is intentional; add an explicit schema or a comment to solrconfig.xml.

### [T01] Duplicated solrconfig.xml in 10 collections
- **Finding:** 10 of the 14 Solr collections share an identical `solrconfig.xml`
  (SHA-256: `5ce00b28...`).
- **Impact:** Every change must be propagated to 10 files manually.
- **Proposal:** Consider Solr configset inheritance or generating the configuration in CI.

### [T01] Missing Docker/infrastructure configuration in the repository
- **Finding:** No Dockerfile or docker-compose files in the repository.
- **Impact:** Running the application locally is not possible without external resources; complicates onboarding.
- **Proposal:** Add a development `docker-compose.yml` or document a link to the infrastructure repository.

## Low Priority

<!-- Cosmetic changes, documentation, minor code quality -->

### [T02] Axios redundant alongside Angular HttpClient
- **Finding:** `axios:^1.11.0` in the npm dependencies — duplicates Angular HttpClient.
- **Proposal:** Verify usage; migrate to Angular HttpClient and remove axios.

### [T02] Moment.js in maintenance mode
- **Finding:** `moment:^2.30.1` — Moment.js has been in maintenance mode since 2020.
- **Proposal:** Migrate to `date-fns` or the native Angular `DatePipe`.

### [T02] Version range for thumbnailator
- **Finding:** `net.coobird:thumbnailator:[0.4,0.5)` — non-deterministic build.
- **Proposal:** Pin a specific version (e.g. `0.4.20`).

### [T02] @ngu/carousel version for Angular 20 in an Angular 21 application
- **Finding:** `@ngu/carousel:^20.0.1` — carousel for Angular 20, the application is Angular 21.
- **Proposal:** Upgrade to a version compatible with Angular 21 or replace it.

### [T02] Duplicate path validation in ApiServlet and InitServlet
- **Finding:** Both classes implement their own path normalization.
- **Proposal:** Extract into a shared utility class `PathUtils`.

### [T01] Typo in a Java package name: `imagging`
- **Finding:** The package `cz.inovatika.arup.digiarchiv.web4.imagging` contains a typo.
- **Impact:** Confusing for developers, visible in imports and documentation.
- **Proposal:** Rename the package to `imaging` (IDE refactor → rename package).

### [T01] Inconsistent base package in ThumbnailsGenerator
- **Finding:** ThumbnailsGenerator uses `cz.incad.arup`, the main application uses `cz.inovatika.arup`.
- **Impact:** Makes it harder to navigate the codebase; suggests the module has a historically different origin.
- **Proposal:** Unify the base package (on the occasion of a larger refactoring of the module).
