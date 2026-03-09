# AGENTS.md — Instructions for AI Agents

Rules in this file apply to the entire `aiscr-digiarchiv-2` repository.
A nested `AGENTS.md` in a subdirectory takes precedence for that subtree.

---

## Repository Overview

This repository contains the **Digitální archiv AMČR** application used
within the Archaeological Information System of the Czech Republic (AIS
CR).

The system provides a digital archive and presentation layer for
archaeological data and documents. The repository contains the backend
application, search configuration, transformation layers, and supporting
utilities.

Key components include:

- Java / Spring application (`web/`)
- Apache Solr search configuration (`solr/`)
- XSLT transformations for presentation
- Docker-based development and deployment environment
- Supporting utilities such as the thumbnail generator

This repository is part of the **ARUP-CAS AIS CR ecosystem**.

------------------------------------------------------------------------

## Repository Orientation (Mandatory First Step)

Before starting any work, agents must gather repository context.

Agents must read:

- `.agents/analysis/repository_map.json`
- `.agents/config/review_cache.json`
- `.agents/reports/bugs.md`
- `.agents/reports/refactoring_backlog.md`
- `.agents/prompts/review_codebase.md`

Purpose:

These files store persistent context accumulated across previous AI
review sessions. Reading them prevents duplicated work and provides
architectural context.

### Resolving Inconsistencies

If content in `.agents/` contradicts high-level repository rules or governance
defined in this document (`AGENTS.md`), `CONTRIBUTING.md`, or other authoritative
project documentation, agents must treat those higher-level documents as the
**source of truth**.

In such cases agents should:

1. Prefer the high-level governance rules defined in:
   - `AGENTS.md`
   - `CONTRIBUTING.md`
   - repository coding standards
   - CI workflow configuration
2. Adapt or update affected files in `.agents/` to align with those rules.
3. Record the adjustment in the review history (for example `review_cache.json`
   or `refactoring_backlog.md`) when relevant.

This ensures long-running AI review artefacts remain consistent with
current repository governance.

------------------------------------------------------------------------

## AI-Generated Content

All AI-generated artefacts must be stored in:

`.agents/`

Examples include:

- audit reports
- architecture analysis
- dependency analysis
- prompt evolution notes
- review outputs

Agent branches must follow the naming convention:

`agents/<agent_name>/<topic>`

Examples:

- `agents/gpt/review-solr-config`
- `agents/claude/refactor-xslt`

Rules:

- Agent branches must always be created **from `dev`**
- Agents must **never push directly to protected branches**
- AI-generated work must be **reviewed by a human before merge**

AI-generated analysis artefacts must not be mixed with application code.

Merging into `main` is performed exclusively by human maintainers.

------------------------------------------------------------------------

## Goal

Agents should aim for **small, safe, reviewable improvements** aligned
with:

- repository coding conventions
- CI requirements
- project documentation
- long-term maintainability

Agents must avoid large refactors unless explicitly requested.

The framework is designed to support **long-running incremental
technical review of the repository**.

------------------------------------------------------------------------

## Agent Behaviour

Agents must:

- gather repository context before starting work
- avoid repeating previously recorded work
- prefer incremental improvements
- record findings in `.agents/`
- keep changes minimal and well scoped
- follow existing coding conventions
- ensure compatibility with the CI pipeline
- suggest improvements to `AGENTS.md` when appropriate

Agents should prioritise consistency with existing code rather than
introducing stylistic changes.

### Recommended Skills

Specialised capabilities that may improve maintenance efficiency.

Skills are **optional helpers**, not mandatory tools.

Typical useful skills include:

- `doc` --- reviewing and editing documentation artefacts
- `gh-fix-ci` --- diagnosing and fixing CI failures
- `gh-address-comments` --- incorporating pull request review comments

Agents should use such capabilities only when they improve quality or
efficiency.

------------------------------------------------------------------------

## Verification Sources

When verifying repository behaviour or architecture, use the following
sources in order of authority:

1.  Running systems or APIs (when accessible)
2.  Source code in this repository
3.  Technical documentation
4.  Repository documentation

If discrepancies are detected, prefer the higher-priority source and
record the finding in `.agents`.

------------------------------------------------------------------------

## Scope

### In Scope

Agents may modify:

- application code in `web/`
- configuration files
- CI configuration
- documentation
- files inside `.agents/`

Agents may also create analysis or documentation files when useful.

### Out of Scope

Agents must not modify generated or external artefacts such as:

- `node_modules/`
- `_site/`
- `target/`
- `build/`
- `dist/`

Agents must also avoid modifying external dependencies unless explicitly
requested.

Agents must not introduce changes that alter runtime behaviour unless
explicitly required by the task.

------------------------------------------------------------------------

## Tech Stack

Technologies detected in this repository include:

### Backend

- Java
- Spring Framework

### Search Infrastructure

- Apache Solr

### Transformation Layer

- XSLT (Saxon processor, XSLT 2.0 / 3.0)

### Frontend / Assets

- TypeScript
- SCSS

### Build System

- Maven or Gradle

### Infrastructure

- Docker
- Docker Compose

### CI/CD

- GitHub Actions

### Coding Standards (Summary)

Agents must follow the coding conventions already used in the
repository.

Important examples:

Java

- Prefer constructor injection for Spring components (avoid field
    injection).
- Log significant exceptions before handling or rethrowing them.
- Use parameterised queries for JPQL / SQL.
- Public classes and methods in `web/` should include meaningful
    Javadoc.

TypeScript / JavaScript

- Respect strict typing (`strict: true`).
- Avoid the `any` type where possible.
- Prefer `async/await` over callback-based patterns.

XSLT

- Each XSLT file should include a header comment describing its
    purpose.
- Use XSLT 2.0 / 3.0 features available in the Saxon processor.
- Maintain consistent namespace prefixes.

SCSS

- Follow BEM naming conventions.
- Shared values should be placed in variables files.

Agents must avoid introducing inconsistent formatting or stylistic
changes unrelated to the task.

------------------------------------------------------------------------

## Branch and PR Rules

This repository follows a **multi-branch workflow**.

Typical branch flow:

`dev → main`

Rules:

- All development targets the **`dev` branch**
- Never push directly to **`main`**
- Always open a Pull Request

Branch naming conventions:

Application work:

- `feature/<topic>`
- `bugfix/<topic>`
- `docs/<topic>`

Agent branches:

- `agents/<agent-name>/<topic>`

Examples:

- `feature/add-thumbnail-cache`
- `bugfix/fix-solr-query`
- `docs/update-api-docs`
- `agents/gpt/review-solr-config`

Pull Requests must include:

- motivation
- description of changes
- testing summary

Use **Draft PRs** for unfinished work.

Merging into `main` is performed only by human maintainers.

------------------------------------------------------------------------

## Testing Expectations

Before submitting changes, agents should verify that the repository
builds successfully.

Typical checks include:

Minimum:

- `mvn compile`
- `mvn test`
- `npm run build`

If the required tooling is not available in the execution environment:

- clearly state this in the PR or task summary
- perform at least a static review of modified files
- never claim that tests passed if they could not be executed

When relevant, agents should also:

- run targeted tests for affected modules
- validate XSLT transformations against example API XML data
- ensure that changes do not break Solr configuration
- confirm that CI configuration remains valid

Always briefly document what verification steps were performed.

------------------------------------------------------------------------

## .agents Structure

The `.agents/` directory stores persistent AI review context.
It is organized into subfolders:

### prompts/

`review_codebase.md`\
Instructions for running long-term AI-assisted review sessions.\
Contains the initialization sequence, task registry and execution procedure.

`prompt_evolution/`\
Suggestions for improving the review prompt, accumulated across sessions.

### config/

`review_config.yaml`\
Configuration for automated review modules based on the repository stack.

`review_cache.json`\
Persistent storage of results from previous AI review sessions.

### analysis/

`repository_map.json`\
High-level structural overview of the repository.

`dependency_graph.json`\
Overview of dependencies between frameworks, services, and repository modules.

`*_analysis.json`\
Task-specific analysis outputs (Solr, XSLT, Docker, security, etc.).

### reports/

`review_reports/`\
Task completion reports (`<task_id>.md`).

`bugs.md`\
Structured log of discovered issues.

`refactoring_backlog.md`\
Long-term improvement backlog.

Agents should update these files whenever relevant findings are made.

------------------------------------------------------------------------

## Repository Context

This repository is part of the **AIS CR infrastructure** maintained by
**ARUP-CAS**.

Related repositories include:

- aiscr-webamcr --- AMČR web application
- aiscr-api-home --- AIS CR API entry point
- aiscr-webamcr-help --- AMČR user documentation

Changes in shared infrastructure such as:

- APIs
- Solr schemas
- shared data models

may affect multiple repositories.

Agents should therefore prefer **conservative, compatible changes** that
do not break the broader ecosystem.
