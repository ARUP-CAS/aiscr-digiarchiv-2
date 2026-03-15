# AGENTS.md — Instructions for AI Agents

Rules in this file apply to the entire `aiscr-digiarchiv-2` repository.
A nested `AGENTS.md` in a subdirectory takes precedence for that subtree.

For coding conventions, commit format, and contribution workflow see `CONTRIBUTING.md`.

---

## Repository Overview

This repository contains the **Digitální archiv AMČR** — the digital archive and
presentation layer for archaeological data within the Archaeological Information
System of the Czech Republic (AIS CR).

This repository is part of the **ARUP-CAS AIS CR ecosystem**.

Related repositories:

- `aiscr-webamcr` — AMČR web application (upstream data source)
- `aiscr-api-home` — AIS CR API documentation and canonical source of truth for endpoint definitions
- `aiscr-webamcr-help` — AMČR user documentation

**External APIs:** This repo **exposes** File API (digiarchiv.aiscr.cz) and OAI-PMH (api.aiscr.cz/oai); it **consumes** AMČR (amcr.aiscr.cz) for user accounts and data. Canonical API docs and live endpoint definitions: [aiscr-api-home](https://github.com/ARUP-CAS/aiscr-api-home), https://api.aiscr.cz/. Do not duplicate endpoint specs here.

Changes to shared infrastructure (APIs, Solr schemas, data models) may affect
multiple repositories. Agents must prefer **conservative, compatible changes**.

---

## Project Layout

| Path | Purpose |
|------|---------|
| `web/src/main/java/` | Java backend (Spring), base pkg `cz.inovatika.arup.digiarchiv.web4` |
| `web/src/main/ng/` | **Angular frontend** (TypeScript, SCSS) |
| `web/src/main/webapp/` | Minimal JSP / web descriptors only (NOT the frontend) |
| `web/src/main/resources/` | XSLT transformations, application config |
| `web/pom.xml` | Maven build (the only build system) |
| `solr/` | 14 Apache Solr configsets |
| `ThumbnailsGenerator/` | Standalone Java utility (separate POM, pkg `cz.incad.arup`) |
| `.github/` | GitHub Actions CI/CD workflows |
| `.agents/` | AI review artefacts — see `.agents/README.md` |

---

## Tech Stack & Versions

| Component | Technology | Version / Notes |
|-----------|-----------|-----------------|
| Backend | Java, Spring | Java 17 |
| Search | Apache Solr | solr-solrj 9.10.1 (client) |
| Transformations | XSLT (Saxon) | Saxon 8.7 — XSLT 2.0 only (XSLT 3.0 not supported) |
| Frontend | Angular, TypeScript, SCSS | Angular 21 |
| Build | Maven | `web/pom.xml`, `ThumbnailsGenerator/pom.xml` |
| CI/CD | GitHub Actions | `.github/workflows/` |
| Thumbnails | ThumbnailsGenerator | Java 1.8 (EOL — inconsistent with main app) |
| Jakarta EE | 11.0.0 | All dependencies must use `jakarta.*` namespace |

**Infrastructure note:** Docker/docker-compose files are **not** part of this
repository. Infrastructure configuration is maintained externally.

**Testing note:** No automated tests exist — `web/src/test/` directory is absent.

---

## Repository Orientation (Mandatory First Step)

Before starting any work, agents must gather repository context.

### For review agents (running `.agents/prompts/review_codebase.md`)

Read all of:

- `.agents/config/review_cache.json` — progress state
- `.agents/config/review_config.yaml` — configuration
- `.agents/analysis/repository_map.json` — structural index
- `.agents/reports/bugs.md` — discovered issues
- `.agents/reports/refactoring_backlog.md` — improvement backlog
- `.agents/prompts/review_codebase.md` — task registry and execution procedure

For **incremental updates** (after a full audit cycle is complete):

- `.agents/prompts/review_update.md` — incremental update workflow (U01–U06)
- `.agents/scripts/review_tools.py` — validation CLI (`hash`, `cross-validate`,
  `coverage-gaps`, `id-inventory`, `lint-artifacts`, `prompt-evolution`,
  `repo-structure`, `status`, `all`)

### For general agents (feature work, bugfixes, refactoring)

Read at minimum:

- This file (`AGENTS.md`)
- `.agents/reports/bugs.md` — to avoid duplicating known issues
- `.agents/analysis/repository_map.json` — structural overview

### Resolving Inconsistencies

If content in `.agents/` contradicts rules in `AGENTS.md`, `CONTRIBUTING.md`,
or other authoritative project documentation:

1. Treat the higher-level document as the **source of truth**.
2. Adapt or update affected `.agents/` files to align.
3. Record the adjustment in `review_cache.json` or `refactoring_backlog.md`.

---

## AI-Generated Content

All AI-generated artefacts must be stored in `.agents/`.
AI-generated analysis must not be mixed with application code.

Agent branches must follow the naming convention:
`agents/<agent-name>/<topic>` (branched from `dev`).

Rules:

- Agents must **never push directly to protected branches** (`main`, `dev`)
- AI-generated work must be **reviewed by a human before merge**
- Merging into `main` is performed exclusively by human maintainers

---

## Goal

Agents should aim for **small, safe, reviewable improvements** aligned with:

- repository coding conventions (see `CONTRIBUTING.md`)
- CI requirements
- long-term maintainability

Agents must avoid large refactors unless explicitly requested.

The `.agents/` framework supports **long-running incremental technical review**.

---

## Agent Behaviour

Agents must:

- gather repository context before starting work
- avoid repeating previously recorded work
- prefer incremental improvements
- record findings in `.agents/`
- keep changes minimal and well scoped
- follow existing coding conventions (`CONTRIBUTING.md`)
- ensure compatibility with the CI pipeline
- suggest improvements to `AGENTS.md` when appropriate

Agents should prioritise consistency with existing code rather than
introducing stylistic changes.

### Recommended Skills

Optional capabilities that may improve maintenance efficiency. Invoke when relevant:

- **`gh-fix-ci`** — Use when a PR has **failing GitHub Actions** checks. Requires `gh` auth (`gh auth login`); uses the skill’s script or `gh pr checks` / `gh run view` to fetch logs, summarize failures, then plan and fix after your approval. Only covers GitHub Actions (external CI is out of scope).
- **`gh-address-comments`** — Use when **addressing review comments** on the open PR for the current branch. Uses `gh` and the skill’s `fetch_comments.py` to list threads, then you choose which comments to address; the agent applies the fixes. Run `gh auth status` first; if not logged in, authenticate then retry.

### Recommended subagents

For larger or riskier changes, run a dedicated subagent in parallel; review rules remain in this file and `CONTRIBUTING.md`.

- **code-reviewer** — Use for multi-file changes, refactors, or when touching Java + Angular together. Configuration is tool-specific; quality criteria are in `AGENTS.md` and `CONTRIBUTING.md`.
- **security-reviewer** (optional) — Use when changing auth, CORS, or security-sensitive code. Cross-check with `.agents/reports/bugs.md` and `.agents/analysis/security_analysis.json`.

---

## Verification Sources

When verifying repository behaviour or architecture, use sources in this
order of authority:

1. Running systems or APIs (when accessible)
2. Source code in this repository
3. Technical documentation
4. Repository documentation

Record any discrepancies in `.agents/`.

---

## Scope

### In Scope

Agents may modify:

- application code in `web/`
- Solr configuration in `solr/`
- CI configuration in `.github/`
- documentation files
- files inside `.agents/`

### Out of Scope

Agents must not modify:

- `node_modules/`, `target/`, `build/`, `dist/`, `_site/`
- vendored/minified third-party files (`*.min.js`, `*.min.css`)
- external dependencies (unless explicitly requested)

Agents must not introduce changes that alter runtime behaviour unless
explicitly required by the task.

---

## Shared automation and rules (no .cursor)

Shared rules and automation context for AI agents must live in **versioned** locations so they are not affected by `.gitignore`:

- **This file (`AGENTS.md`)**: Agent governance, scope, behaviour, recommended skills.
- **`CONTRIBUTING.md`**: Branch/PR workflow, commit format, testing.
- **`CLAUDE.md`**: Quick orientation, paths, build commands, gotchas.
- **`.agents/`**: Prompts (e.g. `prompts/review_codebase.md`, `prompts/review_update.md`), config (`config/review_config.yaml`), reports (`reports/bugs.md`), scripts (`scripts/review_tools.py`).

Do **not** rely on `.cursor/`, `.claude/`, or `.codex/` for team-shared configuration — those directories are in `.gitignore`. Put only local or personal overrides there; canonical rules stay in the paths above.

Detailed automation recommendations (MCP, skills, hooks, subagents) are in `.agents/reports/automation_recommendations.md`.

---

## Branch and PR Rules

See `CONTRIBUTING.md` for the full contribution workflow (branch from `dev`, PR to `dev`, agent branches `agents/<agent-name>/<topic>`, Draft PRs for WIP).

---

## Testing Expectations

See `CONTRIBUTING.md` for detailed testing requirements. Minimum: `cd web && mvn compile -q && mvn test` and `cd web/src/main/ng && npm run build`; or from repo root `scripts/pre-pr-check.sh` / `scripts/pre-pr-check.ps1`. If tooling is unavailable, state so in the PR and perform static review.

---

## Repository Context

This repository is part of the AIS CR ecosystem. Related repositories: `aiscr-webamcr`, `aiscr-api-home`, `aiscr-webamcr-help`. Agents working across repositories should preserve consistency in documentation and review processes.
