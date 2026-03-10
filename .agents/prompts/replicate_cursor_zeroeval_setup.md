# Prompt — replicate Cursor + ZeroEval setup in another repository

Use this prompt in a **different** repository to recreate the same local Cursor setup that is used in `aiscr-digiarchiv-2`, including:

- `.cursor/settings.json` with model strategy and ZeroEval candidates
- `.cursor/rules/*.mdc` with repo-level and model-sync rules
- `.cursor/arena_update_cursor_models.py` script using ZeroEval leaderboard

> Paste everything below into a fresh AI session in the target repo.

---

You are an AI coding assistant working in a **different** repository.  
Your task is to **replicate the local Cursor configuration pattern** used in `aiscr-digiarchiv-2`, adapted to this new repo.

Follow these steps exactly:

## 1. Create / update `.cursor/settings.json`

1. If `.cursor/settings.json` does not exist, create it. If it exists, merge non-destructively.
2. Ensure it contains at least the following structure (adapt names if the user prefers other defaults):

   - `assistantLanguage`: keep or set according to the project’s primary language (e.g. `"cs"` or `"en"`).
   - `defaultModel`: fast, capable general model (e.g. `"gpt-5.1"`).
   - `codeModel`: model to use for code-heavy tasks (e.g. `"gpt-5.1"`).
   - `explainModel`: cheaper/smaller model for simple explanations (e.g. `"gpt-5.1-mini"`).
   - `advancedModel`: strong model for complex work (e.g. `"opus-4.6"`).
   - `useAdvancedModelForComplexTasks`: `true`.
   - `modelStrategy` object with:
     - `description`: explain, in **English**, that default models are used for routine work and top ZeroEval models for complex tasks.
     - `useDefaultFor`: list of tags like `"small_refactors"`, `"single_file_changes"`, `"simple_explanations"`.
     - `useAdvancedFor`: list of tags like `"multi_file_refactors"`, `"architecture_design"`, `"subtle_bug_hunting"`, `"performance_optimizations"`, `"security_sensitive_changes"`.
     - `modelCandidates`: object with keys `text`, `code`, `document`, `search` (each an array of strings, initially empty if no sync has been run yet).
     - `lastArenaSync`: `null` or ISO timestamp; keep null if no sync has been run.
   - Keep any existing repo-specific fields (tools, formatting, behavior) and do not remove unrelated settings.

## 2. Create `.cursor/rules/amcr-repo-rules.mdc`-style repo rule

1. Create a new file `.cursor/rules/<repo>-repo-rules.mdc` (choose `<repo>` name appropriate to this repository).
2. Content guidelines:

   - Frontmatter:
     - `description`: “Repo-level Cursor rules for <this repo>”
     - `alwaysApply: true`
   - Body:
     - Short English summary that this file is for **Cursor AI**, kept short, and only contains repo-specific gotchas.
     - A “Canonical sources of truth” section pointing to this repo’s equivalents of:
       - `AGENTS.md` (or similar agent governance doc, if present)
       - `CONTRIBUTING.md`
       - Any quick-start / environment / build docs (e.g. `CLAUDE.md`, `README.md`, `README_en.md`)
       - Any `.agents/`-like directory with long-running review artefacts (if present)
     - A “Repo-specific gotchas” section listing:
       - Where the **true frontend** lives (if any).
       - Where backend code lives.
       - Any non-obvious modules (e.g. secondary Maven module, tooling subproject).
       - Any patterns about tests (e.g. “no Java tests yet; at least run mvn compile/test if possible”).
       - Any AI artefact directories that are versioned (e.g. `.agents/`).
     - A small “How to evolve this file” section, for example:
       - Keep this file short and curated; when adding a new gotcha, consider whether an older one can be removed, merged, or moved into higher-level docs.
       - Remove or update entries when they stop being true (e.g. after refactors or infra changes).
       - If a rule needs more than a few bullet points of explanation, move the detailed guidance into `AGENTS.md` / `CONTRIBUTING.md` and keep only a short pointer here.
     - A brief “Branch workflow” section describing:
       - Base branch to branch from (e.g. `dev` or `main`).
       - Target branch for PRs.
       - Naming conventions for agent branches (e.g. `agents/{agent_name}/<topic>`).

Adapt names, paths and gotchas to this repository; do **not** copy aiscr-specific paths verbatim unless they exist here too.

## 3. Create `.cursor/rules/arena-model-sync.mdc`-style ZeroEval sync rule

Create `.cursor/rules/arena-model-sync.mdc` with:

- Frontmatter:
  - `description: Keep Cursor model candidates in sync with ZeroEval leaderboard (local-only, no git/CI coupling)`
  - `alwaysApply: true`

- Body:
  - Explain that `.cursor/settings.json` is **local only** (should not be relied on by CI or GitHub).
  - Explain that `modelStrategy.modelCandidates.{text,code,document,search}` are **hints** based on `https://api.zeroeval.com/leaderboard/models/full?justCanonicals=true`, not hard requirements.
  - “Weekly freshness check” section:
    - Before a non-trivial coding session, read `modelStrategy.lastArenaSync`.
    - If missing or older than 7 days, treat the config as stale.
    - When stale and internet is available, run `python .cursor/arena_update_cursor_models.py` from the repo root to refresh candidates and `lastArenaSync`.
    - If the script or dependencies are missing, tell the user briefly instead of failing silently.
  - “How to use the candidates” section:
    - For routine, low-risk tasks, use `defaultModel` / `codeModel`.
    - For complex/high-stakes tasks (large refactors, subtle bugs, performance/security work), prefer top entries in:
      - `modelCandidates.code` for coding-heavy work.
      - `modelCandidates.text` / `document` for long-form reasoning or documentation.
      - `modelCandidates.search` when heavy web/search reasoning is needed.
    - If a candidate model is not available in this environment, gracefully fall back to the closest available model type.

## 4. Create `.cursor/model_mapping.json` (editable switchboard)

1. Add a new file `.cursor/model_mapping.json` that is intended to be **edited by humans** when new models become available in this environment.
2. This file is a simple **switchboard**: you only **list the ZeroEval model names you want to allow per category** (under `enabled.default`, `enabled.code`, `enabled.explain`, `enabled.advanced`) and the script will derive preference order automatically. You **never** need to guess internal model IDs.
3. Use this structure:

   ```json
   {
     "//_comment": "Human-editable switchboard: per-category allow-lists. Add/remove model names under each key; ordering is derived automatically from ZeroEval ranks.",
     "enabled": {
       "default": ["GPT-5.1"],
       "code": ["Claude Opus 4.6"],
       "explain": ["GPT-5.1"],
       "advanced": ["Claude Opus 4.6", "Claude Sonnet 4.6", "GPT-5.1"]
     },
     "preferredOrder": {
       "default": ["GPT-5.1"],
       "code": ["Claude Opus 4.6"],
       "explain": ["GPT-5.1"],
       "advanced": ["Claude Opus 4.6", "Claude Sonnet 4.6", "GPT-5.1"]
     }
   }
   ```

4. When the set of available models changes (e.g. new GPT/Claude/Gemini variants in Cursor), only this file needs to be updated; the Python script remains unchanged.

## 5. Create `.cursor/arena_update_cursor_models.py` using ZeroEval

1. Add a new file `.cursor/arena_update_cursor_models.py`.
2. Implement it in Python so that it:

   - Fetches JSON from `https://api.zeroeval.com/leaderboard/models/full?justCanonicals=true`.
   - Parses it as a list of model objects.
   - Uses these heuristics to select top `max_models` (e.g. 10) for each category:
     - **text**: sort by `mmmlu_score`, then `gpqa_score` (descending).
     - **code**: sort by `swe_bench_verified_score` (descending).
     - **document**: sort by `mmmu_score`, then `mmmu_pro_score`, then `mmmlu_score`.
     - **search**: sort by `browsecomp_score`.
     - Treat missing scores as `0.0`.
   - Opens `.cursor/settings.json`, ensures `modelStrategy` and `modelStrategy.modelCandidates` exist, and fills:
     - `modelCandidates.text`, `modelCandidates.code`, `modelCandidates.document`, `modelCandidates.search` with the chosen model **names**.
   - Loads `.cursor/model_mapping.json` to get:
   - `enabled` keys for each ZeroEval model name — any name present under `enabled` is allowed to drive model selection; its value is ignored.
   - `preferredDefaultOrder` and `preferredAdvancedOrder` (arrays of names) to record the current preference order, which the script can recompute from leaderboard ranks.
   - Resolves enabled model names to concrete Cursor model IDs using a small built-in mapping for well-known models (e.g. Claude Opus, GPT-5.x, Claude Sonnet 4.6) and, if needed, by falling back to the `model_id` field from the ZeroEval JSON.
   - Propagates candidates into `defaultModel` / `codeModel` / `advancedModel` **only** if their names both appear in `modelCandidates` and are enabled in the mapping file, using the preferred order lists.
   - Updates `modelStrategy.lastArenaSync` to the current UTC ISO timestamp.
   - Writes `settings.json` back with pretty-printed JSON (`indent=2`, `ensure_ascii=False`).
   - If the HTTP call fails or JSON is invalid, leave `settings.json` unchanged.

Keep imports minimal (`json`, `datetime`, `typing`, `requests`). Do **not** introduce any repo-specific dependencies.

## 5. Final checks and idempotency

1. Ensure that running `python .cursor/arena_update_cursor_models.py` multiple times is **idempotent**:
   - It only changes `modelCandidates` and `lastArenaSync`.
   - It does not remove or rename any unrelated keys in `settings.json`.
2. Ensure all new `.cursor` files are:
   - Small and focused.
   - Written in **English**.
   - Safe to keep uncommitted (they are local-only by design).

3. When reporting back to the user in the new repo, briefly summarize:
   - What was created/updated under `.cursor/`.
   - Whether the ZeroEval sync ran successfully, or why it failed (e.g. network error).

