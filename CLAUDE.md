# CLAUDE.md — Claude Code Project Context

For agent governance, scope rules, and `.agents/` structure see `AGENTS.md`.
For coding conventions, commit format, and PR workflow see `CONTRIBUTING.md`.

---

## Quick Orientation

- **Digitální archiv AMČR** — archaeological digital archive (AIS CR)
- Primary language of project: **Czech** (code comments, Javadoc, commit messages, reports)
- Branches: `dev` (all development) → `main` (human-only merge)
- Agent branches: `agents/claude/<topic>` from `dev`

## Key Paths

```
web/src/main/java/        → Java backend (Spring), ~88 files
web/src/main/ng/          → Angular 21 frontend (THE frontend)
web/src/main/webapp/      → NOT the frontend (minimal JSP/descriptors)
web/src/main/resources/   → XSLT transformations, config
web/pom.xml               → Maven build (only build system)
solr/                     → 14 Solr configsets
ThumbnailsGenerator/      → Standalone thumbnail utility (separate POM)
.agents/                  → AI review artifacts
```

## Build & Verify

```bash
# Java
cd web && mvn compile -q && mvn test

# Angular frontend
cd web/src/main/ng && npm install && npm run build
```

No automated tests exist — `web/src/test/` is absent.

## Commit Format

Commit format and allowed types: see `CONTRIBUTING.md` § Commit zprávy.

## Known Critical Issues (from .agents/reports/bugs.md)

| ID | Severity | Issue |
|----|----------|-------|
| BUG-003 | Kritická | Typo `multiValued="fslse"` in `solr/entities/conf/managed-schema:157` |
| BUG-004 | Vysoká | `FedoraHarvester.indexModels()` — 10M records without pagination → OOM |
| BUG-005 | Vysoká | luceneMatchVersion 9.4 vs solr-solrj 9.10.1 mismatch (all 14 collections) |
| BUG-001 | Vysoká | Saxon 8.7 — 18 years without security patches |
| BUG-002 | Střední | `javax.mail` in Jakarta EE 11 project |

## Gotchas

- `web/src/main/webapp/` is NOT the frontend — Angular lives in `web/src/main/ng/`
- 10 of 14 Solr collections share identical `solrconfig.xml` — propagate changes to all
- `entities/managed-schema` is 1000+ lines — grep-first, don't read entirely
- Saxon 8.7 does NOT support XSLT 3.0 (despite earlier docs claiming it)
- ThumbnailsGenerator targets Java 1.8 (EOL), main app targets Java 17
- Base package inconsistency: main app `cz.inovatika.arup` vs ThumbnailsGenerator `cz.incad.arup`
- Package typo: `web4.imagging` should be `web4.imaging`
- Docker/docker-compose files are NOT in this repo — infrastructure is external
- God classes: `SolrSearcher` (1048 LOC), `FedoraHarvester` (1024 LOC)

## AI Review System

Incremental review managed via `.agents/prompts/review_codebase.md`.

- Tasks T01–T03: done | T04–T10: pending | T11: blocked (needs T01–T10)
- State: `.agents/config/review_cache.json`
- To continue review: read `.agents/prompts/review_codebase.md` and follow init sequence
- All review outputs in Czech
