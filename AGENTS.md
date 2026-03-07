# AGENTS.md

Rules in this file apply to the entire `aiscr-digiarchiv-2` repository.
A nested `AGENTS.md` in a subdirectory takes precedence for that subtree.

---

## Repository orientation

Before starting any work, gather context:

- **Repository structure overview:** `docs_agents/repository_map.json`
- **Ongoing audit state and past findings:** `docs_agents/review_cache.json`,
  `docs_agents/bugs.md`, `docs_agents/refactoring_backlog.md`
- **Analytical background** (architecture, Solr, XSLT, Docker, security, etc.):
  other JSON files in `docs_agents/`

These files contain context accumulated across previous sessions —
reading them avoids duplicating work and provides relevant background for the task.

For technical audit or review tasks, read first:
`docs_agents/PROMPT.md`

---

## AI-generated content

All content produced by agents (audit reports, analysis JSON files, prompt evolution notes)
belongs in the `docs_agents/` directory and should be committed to a dedicated
`agents/{agent_name}/` branch, branched off `dev`.

This keeps AI-generated artefacts reviewable and separate from application code.
`agents/` branches are merged into `dev` only after human review.
Merging into `main` is done exclusively by humans — do not target `main` in any PR.

---

## Goal

Keep changes small, safe, and easy to review in line with project conventions
(`CONTRIBUTING.md`, `README.md`, CI workflows and project documentation).

---

## Agent behaviour

- Gather context before starting work (see section above) — do not repeat what was already done.
- Use available skills where they add value.
- After completing a task, suggest whether and how to update this file to improve its quality.
- Record proven skills in the `Recommended skills` section below.

---

## Recommended skills

- `doc` — for reviewing and editing documentation artefacts where formatting matters.
- `gh-fix-ci` — when you need to quickly locate and fix CI failures.
- `gh-address-comments` — when incorporating review comments from a PR.

---

## Repository quick reference

- Main application: `web/` (Java, Spring)
- Search engine: `solr/` (Apache Solr configuration and schemas)
- Thumbnail utility: `ThumbnailsGenerator/` (standalone Java utility)
- Infrastructure: `docker-compose*.yml`
- CI/CD: `.github/`
- Ongoing audit: `docs_agents/` — read before any technical task

---

## Authoritative rule sources (read before larger changes)

1. `CONTRIBUTING.md`
2. `web/` — project-level build configuration (`pom.xml` or `build.gradle`)
3. `.github/` — CI workflow definitions

---

## Mandatory rules for edits

1. Do not change runtime behaviour when the task is purely documentary or a no-feature refactor.
2. Follow the style of existing code; do not introduce large unrelated refactors.
3. Do not modify Solr schema (`solr/`) without noting the need for reindexation.
4. Do not overwrite or remove existing changes outside the scope of the task.
5. Do not commit secrets, keys, or sensitive local configuration.

---

## Coding standards and quality

Java:

- Constructor injection in Spring components — never field injection (`@Autowired` on fields).
- All checked and significant unchecked exceptions must be logged before re-throw or handling.
- JPQL / SQL queries must be parameterised — no string concatenation with user input.
- Javadoc in Czech for all public classes and methods in the `web/` module.
- Do not use Google-style Javadoc sections; use standard `@param`, `@return`, `@throws`.
- Keep descriptions specific to actual behaviour — not generic templates.

TypeScript / JavaScript:

- `strict: true` in `tsconfig.json` — do not disable strict mode.
- Avoid `any` types; define explicit interfaces.
- Prefer `async/await` over callback-based patterns.

XSLT:

- Each XSLT file must have a header comment stating its purpose and input namespace.
- XSLT 2.0 / 3.0 (Saxon processor) — use its features, do not write XSLT 1.0-compatible code.
- Namespace prefixes must be consistent across all files.
- Every change must be validated against sample AMČR API data.

SCSS:

- Follow BEM naming for all classes.
- New colour and dimension values belong in `_variables.scss`.
- No inline styles in HTML / Thymeleaf templates.

---

## Tests before submitting changes

Minimum:

1. `mvn compile -q` (or `./gradlew compileJava`) — verify Java compilation
2. `mvn test` (or `./gradlew test`) — unit tests must pass
3. `npm run build` — verify TypeScript / SCSS compilation

Based on change type:

1. Run targeted tests for affected Java modules.
2. For XSLT changes: validate transformations against sample AMČR API XML fixtures.
3. For Solr schema changes: start Solr locally and run a smoke-test query.
4. Selenium / integration tests only when the scope requires it and after confirmation
   by the human initiating the prompt.

Always briefly describe the test outcome in the PR/summary
(`what was run`, `what passed`, `what could not be run`).

### Fallback without Java / Maven

If `mvn` / `java` is unavailable in the environment:

1. State this explicitly in the summary/PR.
2. Perform at least a static diff review:
   - `git diff -- '*.java'`
   - `git diff -- '*.xsl' '*.xslt'`
3. Never claim checks passed if the tools could not be run.

---

## Git and PR workflow

- Base branch for all development: `dev`. Always branch off `dev`, if not instructed specifically otherwise.
- Branch naming:
  - application changes: `feature/<issue>` or `bugfix/<issue>` (see `CONTRIBUTING.md`)
  - agent-generated content: `agents/{agent_name}/<topic>`
- Make small, logical commits.
- In a PR include:
  - issue reference (`#number`)
  - Motivation
  - Description
  - Testing
- Use a Draft PR if the work is not yet ready for review.
- Do not open PRs targeting `main` — merging into `main` is done exclusively by humans.

---

## What owners and CI typically expect

- CI runs on PRs into `dev` (and `main`, which is managed by humans)
- Solr schema changes require a note about reindexation in the PR description
- XSLT changes require evidence of validation against sample data

Prefer changes that pass the CI pipeline without manual intervention.
