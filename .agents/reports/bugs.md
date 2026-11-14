# Tracked bugs — Digitální archiv AMČR (aiscr-digiarchiv-2)

> Review prose follows the canonical English-default rule; verbatim Czech is preserved where exact source wording, field names, or AIS CR domain identifiers matter.
> Before adding a new bug, check the existing GitHub Issues (currently 60 open).
>
> Statuses: `already tracked (Issue #XXX)` | `extends existing issue #XXX` | `new issue candidate`
>
> Severity values: `Critical` | `High` | `Medium` | `Low`

---

<!-- Entries are added by agents after completing individual tasks -->

### BUG-001: Saxon 8.7 — critically outdated XSLT processor without security patches

- **Files:** `web/pom.xml:125`
- **Severity:** High
- **GitHub Issue:** new issue candidate
- **Description:** The Maven dependency `net.sf.saxon:saxon:8.7` dates from 2006 (roughly an 18-year-old version). The current open-source version is Saxon-HE 12.x. The absence of security patches over 18 years of development represents a security risk when processing input XML from the Fedora API. AGENTS.md declares the use of an XSLT 2.0/3.0 processor, but Saxon 8.7 does not support XSLT 3.0 and has only partial XSLT 2.0 support.
- **Recommended fix:** Replace the dependency with `net.sf.saxon:Saxon-HE:12.5` (open source, backward compatible for XSLT 2.0, full XSLT 3.0 support). After the upgrade, verify the existing XSLT transformations.
- **Task:** T02

---

### BUG-002: javax.mail namespace incompatible with Jakarta EE 11

- **Files:** `web/pom.xml:78`
- **Severity:** Medium
- **GitHub Issue:** new issue candidate
- **Description:** The dependency `javax.mail:mail:1.4.7` uses the old `javax` namespace. The project targets Jakarta EE 11, which uses the `jakarta.*` namespace. On modern application servers (Tomcat 10+, WildFly 27+) this may cause a classloading conflict or non-functional email sending.
- **Recommended fix:** Replace with `jakarta.mail:jakarta.mail-api` + an implementation (Angus Mail: `org.eclipse.angus:angus-mail`). Also update `org.apache.commons:commons-email`, which is built on javax.mail.
- **Task:** T02

---

### BUG-003: Typo multiValued="fslse" in the entities schema — undefined indexing behavior

- **Files:** `solr/entities/conf/managed-schema:157`
- **Severity:** Critical
- **GitHub Issue:** new issue candidate
- **Description:** The field `samostatny_nalez_projekt` has `multiValued="fslse"` — a typo instead of `"false"`. Solr accepts the invalid boolean attribute and its behavior depends on the version. It may cause unexpected multi-valued behavior (storing multiple values in a single-value field) or failure to index a samostatný nález record with a parse exception error.
- **Recommended fix:** Fix to `multiValued="false"`. After the fix, reindex the entities collection (or at least records of type `samostatny_nalez`).
- **Task:** T03

---

### BUG-004: FedoraHarvester.indexModels() — hardcoded limit of 10,000,000 records without pagination

- **Files:** `web/src/main/java/cz/inovatika/arup/digiarchiv/web4/fedora/FedoraHarvester.java:263`
- **Severity:** High
- **GitHub Issue:** new issue candidate
- **Description:** The `indexModels()` method sets `max_results=10000000` and loads the entire Fedora API response as a single `JSONObject` into JVM memory. With a large database (thousands of documents with metadata) this leads to an `OutOfMemoryError`. Paradoxically, `checkDatestamp()` in the same class correctly uses `CursorMark` for iteration — but `indexModels()` does not implement this. A full reindex (triggered, for example, after a migration) can therefore bring down the application server.
- **Recommended fix:** Implement cursor-based pagination using `CursorMarkParams` (as `checkDatestamp()` does), or offset-based pagination with `batchSize=1000`.
- **Task:** T03

---

### BUG-005: luceneMatchVersion=9.4 vs. solr-solrj 9.10.1 — configuration mismatch across all 14 collections

- **Files:** `solr/entities/conf/solrconfig.xml:38` (and 13 more solrconfig.xml files)
- **Severity:** High
- **GitHub Issue:** new issue candidate
- **Description:** All `solrconfig.xml` files declare `<luceneMatchVersion>9.4</luceneMatchVersion>`, while the client library `solr-solrj` is at version 9.10.1 (see `web/pom.xml:119`). If the Solr server runs version 9.10.x (matching the client), the old `luceneMatchVersion` causes: (1) non-activation of the Lucene 9.5–9.10 index and tokenization optimizations; (2) potential differences in analyzer behavior between full reindexing and querying. No migration note exists.
- **Recommended fix:** Determine the current Solr server version. Update `luceneMatchVersion` to a matching value in all 14 `solrconfig.xml` files. Then perform a full reindex of all collections.
- **Task:** T03
