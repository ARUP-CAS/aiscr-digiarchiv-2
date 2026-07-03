# Workflow-evolution legacy evidence

Created: 2026-07-03
Expanded: 2026-07-04 after reviewer feedback.

This report preserves and classifies the retired `.agents/prompts/prompt_evolution/`
channel. The original files were sibling-local evidence for improving the
codebase-review workflow before the hub-canonical `aiscr-codebase-review`
workflow owned the active feedback path.

## Classification

All source files matched the legacy `<task_id>_prompt_update.md` convention and
were retained as evidence here before removing the retired prompt-evolution
directory. No hub backlog handoff was approved for these notes in this session.
Future U05 review runs may use this report as evidence, but new workflow changes
still require an explicit human-approved report-to-backlog handoff.

## Evidence Inventory

The notes below preserve implementation-relevant facts from the retired source
files. They are intentionally more detailed than a status summary so the deleted
legacy files are not needed for future implementation work.

### T01 Repository Map

Source: `T01_prompt_update.md`
Disposition: retained implementation evidence.

- Treat `web/src/main/ng/` as the real Angular frontend. The older
  `web/src/main/webapp/` path contains only minimal JSP and web descriptor files.
- Do not assume Docker compose files exist in this repository. Infrastructure
  files may live in an external repository.
- Record absence of `web/src/test` and automated tests during initialization
  instead of treating the path as required.
- Preserve repository sizing facts for planning: the legacy run found 88 Java
  files, not hundreds.
- Preserve the 14-Solr-collection fact. T03 may need collection-level subtasks
  such as T03a through T03n.
- Add or retain these important paths:
  `web/src/main/ng/`, `web/src/main/ng/package.json`,
  `web/src/main/ng/tsconfig.json`, and `ThumbnailsGenerator/pom.xml`.

### T02 Dependency Graph

Source: `T02_prompt_update.md`
Disposition: retained implementation evidence.

- Define dependency-analysis depth explicitly. Package-level Java dependency
  analysis is not enough when god classes exist; include class-level dependencies
  for classes over roughly 500 LOC.
- Check EOL or maintenance-mode status for every Maven and npm dependency.
- Because the project targets Jakarta EE 11, verify that dependencies use
  `jakarta.*` rather than legacy `javax.*` namespaces where relevant.
- Flag Maven version ranges such as `[0.4,0.5)` as reproducibility risks.
- Preserve known-fact candidates:
  `saxon_version: "8.7"`, target `Saxon-HE 12.x`,
  `jakarta_ee_version: "11.0.0"`, `java_version: "17"` for the main app, and
  Java 1.8 for `ThumbnailsGenerator`.
- For Solr analysis, compare `solr-solrj:9.10.1` against the Solr server
  configuration.
- For XSLT analysis, check whether files use XSLT 3.0 or unsupported Saxon 8.7
  constructs.
- Add or retain `web/src/main/ng/angular.json`,
  `web/src/main/ng/tsconfig.json`, and
  `web/src/main/webapp/META-INF/context.xml`.
- Prefer `jdeps` or equivalent automation for Java internal dependencies when
  available.
- Remember Saxon artifact naming: the legacy dependency used
  `net.sf.saxon:saxon`; newer Saxon-HE artifacts use `net.sf.saxon:Saxon-HE`.

### T03 Solr Analysis

Source: `T03_prompt_update.md`
Disposition: retained implementation evidence.

- Preserve the prioritization pattern: analyze `entities`, `work`, `heslar`, and
  `soubor` deeply, and summarize lower-risk collections more briefly when needed.
- Preserve the value of explicit god-class context, especially `SolrSearcher` and
  `FedoraHarvester`, so future agents start from the right Java files.
- `max_files_per_task: 20` was enough for 14 configsets plus key Java files only
  because the review prioritized.
- Use a grep-first strategy for large Solr schemas. For
  `entities/managed-schema` with more than 1000 lines, first extract
  `<field`, `<copyField`, and `<uniqueKey` sections instead of reading the whole
  file at once.
- When Docker files are absent, compare Solr client/server compatibility using
  `pom.xml` and `luceneMatchVersion` in `solrconfig.xml`.
- Detect Java-to-Solr schema mismatches by extracting field names from
  `addField()` calls and comparing them with explicit and dynamic schema fields.
  Focus on prefixes such as `adb_*`, `az_*`, and `dokument_*` in
  `ArcheologickyZaznam`.
- Treat "stored but never returned" as a heuristic. Search for `setFields` and
  mark fields absent from handlers/result transformations as potentially unused,
  not proven unused.
- Check sensitive Solr collections including `uzivatel`, `uzivatel_ui`, and
  `organizations` for fields such as email, phone, password, and address. Escalate
  exposed sensitive fields to `bugs.md`.
- Carry T03 findings into T04: Saxon 8.7 processes Fedora API XSLT transforms, so
  T04 should check incompatible constructs such as `xs:`, `for-each-group`, and
  `sequence`.

## Cleanup Decision

The original `.agents/prompts/prompt_evolution/` directory is no longer an active
feedback channel. The implementation-relevant notes from every legacy source
file are preserved above, and the retired source files can remain deleted. New
workflow-evolution feedback remains sibling-local evidence unless a maintainer
explicitly approves a backlog handoff.
