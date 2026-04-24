# Hooks reference — recommended local configuration

Hooks are configured locally (for example in `.claude/settings.json` or the Cursor equivalent); the `.cursor/` and `.claude/` directories are in `.gitignore`. This file is a reference description for the team — copy the rules into your local configuration.

## 1. PostToolUse: lint after editing Angular files

**Purpose:** After writing to `*.ts` or `*.scss` in `web/src/main/ng/`, run linting so the code stays consistent with `CONTRIBUTING.md` conventions.

**Configuration idea:**

- Trigger: after any tool use that modifies a file matching `web/src/main/ng/**/*.ts` or `web/src/main/ng/**/*.scss`.
- Action: for the changed files, run for example:
  - `npx ng lint` (from `web/src/main/ng/`), or
  - `npx prettier --check <path>` if Prettier is configured.

**Note:** Vendored files (`*.min.js`, `*.min.css`) and files under `node_modules/` are excluded.

## 2. PreToolUse: warn/block editing of vendored and sensitive files

**Purpose:** Prevent accidental edits of vendored third-party files and files that should not be modified by agents.

**Paths to protect (or warn about):**

- `**/*.min.js`, `**/*.min.css` — vendored/minified files
- `**/node_modules/**` — npm dependencies
- `**/target/**` — Maven build output
- `**/dist/**` — Angular build output
- Files with third-party copyright headers (`/*!`, `* @license`, `* jQuery`, `* Bootstrap`)

**Configuration idea:**

- Before executing a tool use that writes to a file, check the path.
- If it matches the patterns above: block with an explanation or show a confirmation prompt (depending on client capabilities).

## 3. PreToolUse: warn when editing Solr schema files

**Purpose:** Remind that Solr schema changes may require reindexing, and that 10 of 14 collections share identical `solrconfig.xml` — changes must be propagated.

**Paths to watch:**

- `solr/*/conf/managed-schema`
- `solr/*/conf/solrconfig.xml`

**Configuration idea:**

- Before writing to a Solr config file, show a reminder:
  - "This change may require reindexing. Mention it in the PR."
  - "10 collections share the same solrconfig.xml — propagate changes."

---

For full context see `[automation_recommendations.md](../reports/automation_recommendations.md)` and `AGENTS.md` § Shared Automation Rules.
