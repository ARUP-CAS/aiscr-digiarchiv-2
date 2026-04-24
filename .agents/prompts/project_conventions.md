# Project Conventions — Digitální archiv AMČR (summary for agents)

Short extract of conventions from `CONTRIBUTING.md` and `AGENTS.md`. The authoritative wording is in `[CONTRIBUTING.md](../../CONTRIBUTING.md)` and `[CLAUDE.md](../../CLAUDE.md)`.

## Branches

- Base: **always** from `dev`. PRs target `dev`.
- Branch naming: `feature/<issue>`, `bugfix/<issue>`, `agents/<agent_name>/<topic>`.
- Never push directly to `dev` or `main`.

## Commit messages

```
[type] short description (#issue-number)
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style`, `perf`.

## Java / code

- **Constructor injection** in Spring — no field injection (`@Autowired` on fields).
- **Javadoc:** Czech language, standard `@param`, `@return`, `@throws`. Not Google-style.
- **Parametrized queries** — no string concatenation with user input.
- Log exceptions before re-throw; never swallow them.

## TypeScript / Angular

- `strict: true` in `tsconfig.json`.
- No `any`; use explicit types and interfaces.
- No `console.log` in production code.

## XSLT

- Header comment with purpose and input namespace required.
- XSLT 2.0 (Saxon 8.7) — do NOT use XSLT 3.0 features.
- Namespace prefixes must be consistent across files.

## SCSS

- BEM methodology for class naming.
- New colors/dimensions go to `_variables.scss`.
- No inline styles.

## What not to edit

- Vendored/minified files (`*.min.js`, `*.min.css`, files with third-party headers).
- `node_modules/`, `target/`, `dist/`, `build/`.
- Sensitive files (see `hooks_reference.md`).

## Agent outputs

- Everything goes into `.agents/` (reports, analyses, cache).
- Bugs go to `.agents/reports/bugs.md`; cross-reference with GitHub Issues.
- Refactoring suggestions go to `.agents/reports/refactoring_backlog.md`.

## Key paths

- **Frontend:** `web/src/main/ng/` — NOT `web/src/main/webapp/` (that's only JSP/web descriptors).
- **Java backend:** `web/src/main/java/`
- **Solr config:** `solr/` (14 configsets, 10 share identical `solrconfig.xml`)
- **XSLT:** `web/src/main/resources/`
- **No tests:** `web/src/test/` does not exist.
