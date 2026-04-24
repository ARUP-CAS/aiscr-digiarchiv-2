# Setup dev / onboarding — Digitální archiv AMČR

Checklist for setting up the development environment and for agents that execute steps on the developer's machine.

## Environment

- **OS:** Windows 11 (use Unix-style syntax in bash/scripts where possible).
- **Java:** JDK 17+ (required by `web/pom.xml`).
- **Maven:** 3.8+ (build system for Java backend).
- **Node.js:** 18+ with npm (required by Angular frontend in `web/src/main/ng/`).

## Key paths

| Path | Purpose |
|------|---------|
| `web/src/main/java/` | Java backend (Spring) |
| `web/src/main/ng/` | **Angular frontend** (THE frontend) |
| `web/src/main/webapp/` | NOT the frontend (minimal JSP/web descriptors only) |
| `web/src/main/resources/` | XSLT transformations, application config |
| `web/pom.xml` | Maven build (main module) |
| `solr/` | 14 Solr configsets |
| `ThumbnailsGenerator/` | Standalone utility (separate POM, Java 1.8) |

## Basic commands

| Purpose | Command |
|---------|---------|
| Java compile | `cd web && mvn compile -q` |
| Java tests | `cd web && mvn test` |
| Angular build | `cd web/src/main/ng && npm install && npm run build` |
| Full pre-PR | `./scripts/pre-pr-check.sh` (Bash) or `.\scripts\pre-pr-check.ps1` (PowerShell) |

## Before the first commit

1. Branch from `dev`: `git checkout dev && git pull && git checkout -b feature/<issue>`.
2. Verify build passes: `cd web && mvn compile -q && mvn test`.
3. Verify Angular build: `cd web/src/main/ng && npm install && npm run build`.
4. Do not add to commits: vendored files, `*.min.js`, `*.min.css`, `node_modules/`, `target/`.

## For agents

- Read `AGENTS.md` and `.agents/config/review_cache.json` first.
- Code conventions: `[project_conventions.md](project_conventions.md)`.
- Full wording: `CONTRIBUTING.md`, `CLAUDE.md` (in the repository root).

## Further documentation

- Project conventions: `CONTRIBUTING.md`
- Agent governance: `AGENTS.md`
- Quick orientation: `CLAUDE.md`
