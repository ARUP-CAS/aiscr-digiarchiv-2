# Documentation Hygiene Audit Report

**Repository:** aiscr-digiarchiv-2 (Digitální archiv AMČR)  
**Date:** 2026-03-11  
**Files audited:** 20 instruction-bearing files (root + .agents + .cursor)  
**Fixes applied:** 2026-03-11 (all critical and WARN fixes; optional notes added)

## Summary

| Check | Status | Findings |
|-------|--------|----------|
| C1 File Discovery | OK | 20 files found |
| C2 Audience Mapping | OK | 0 redundant files; clear ownership |
| C3 Duplication | OK | Commit-format duplication removed; cross-refs in place |
| C4 Drift | OK | review_config.yaml target_file paths corrected |
| C5 Cross-References | OK | 0 broken refs; 1 ambiguous (CODEOWNERS location) |
| C6 Token Efficiency | OK | AGENTS/CLAUDE shortened; cross-refs replace repeated content |
| C7 Governance | OK | Present in CONTRIBUTING.md § Dokumentační governance |

**Fixes applied (2026-03-11):** Critical: review_config.yaml target_file paths updated. WARN: AGENTS.md Branch/PR and Testing sections shortened; CLAUDE.md commit format replaced with cross-ref. Optional: audit prompt CODEOWNERS note; config comment on path convention.

---

## C1 — File Discovery

| File | Lines | Language | Apparent audience |
|------|-------|----------|-------------------|
| `README.md` | 137 | CS | GitHub visitors, developers |
| `README_en.md` | 137 | EN | Same (EN) |
| `CONTRIBUTING.md` | 232 | CS | Contributors, AI agents |
| `CLAUDE.md` | 72 | EN | Claude Code / AI agents |
| `AGENTS.md` | 244 | EN | AI coding agents |
| `CODEOWNERS` | (root) | — | GitHub code ownership |
| `.agents/README.md` | 22 | CS | Developers, agents |
| `.agents/prompts/review_codebase.md` | 519 | EN/CS | Review agents |
| `.agents/prompts/audit_doc_hygiene.md` | 236 | EN | Any agent (audit prompt) |
| `.agents/prompts/prompt_evolution/README.md` | — | CS | Maintainers |
| `.agents/prompts/prompt_evolution/T01_prompt_update.md` | — | CS | Prompt evolution |
| `.agents/prompts/prompt_evolution/T02_prompt_update.md` | — | CS | Prompt evolution |
| `.agents/prompts/prompt_evolution/T03_prompt_update.md` | — | CS | Prompt evolution |
| `.agents/config/review_config.yaml` | 188 | YAML | Review system, agents |
| `.agents/config/review_cache.json` | 171 | JSON | Review system state |
| `.agents/reports/bugs.md` | — | CS | Developers, agents |
| `.agents/reports/refactoring_backlog.md` | — | CS | Developers, agents |
| `.agents/reports/automation_recommendations.md` | 109 | CS | Maintainers |
| `.agents/reports/review_reports/README.md` | — | — | Internal |
| `.cursor/rules/repo-rules.mdc` | 37 | EN | Cursor AI (local) |
| `.cursor/rules/arena-model-sync.mdc` | 27 | EN | Cursor AI (local) |

**Note:** No `.github/CODEOWNERS` or `.github/PULL_REQUEST_TEMPLATE.md`; `CODEOWNERS` is at repo root. No `MEMORY.md`, `CODEX.md`, `COPILOT.md`, or `.cursorrules` in repo.

---

## C2 — Audience & Responsibility

- **README.md / README_en.md** — Public entry; project description, stack, structure, links. Distinct by language; no redundancy.
- **CONTRIBUTING.md** — Single source of truth for branches, PR, commits, testing, code conventions, documentation governance. Audience: human contributors + agents (referenced by AGENTS.md).
- **AGENTS.md** — Agent governance, scope, behaviour, recommended skills, testing expectations. Defers workflow detail to CONTRIBUTING.md. Clear ownership.
- **CLAUDE.md** — Short orientation, paths, build commands, gotchas, known issues. Explicitly defers to AGENTS.md and CONTRIBUTING.md. No overlap of responsibility.
- **.agents/README.md** — Structure of `.agents/`; points to AGENTS.md. No overlap.
- **.agents/prompts/review_codebase.md** — Review task registry and procedure; defers to AGENTS.md. Responsibility: how to run review, not what the rules are.
- **.agents/config/review_config.yaml** — Canonical review config (per CONTRIBUTING.md). Status semantics and task list live here; cache holds runtime state.
- **.agents/config/review_cache.json** — Runtime progress; not authority for task definitions.
- **.cursor/rules/*.mdc** — Local Cursor rules; repo-rules points to AGENTS.md/CONTRIBUTING.md/CLAUDE.md and adds gotchas; arena-model-sync is tooling-only. Both marked local; AGENTS.md states shared rules live in versioned docs.

**Verdict:** Every file has a clear, distinct audience and responsibility. No redundant file pair.

---

## C3 — Content Duplication Detection

| Topic | Files | Classification |
|-------|--------|----------------|
| Branch workflow (dev/main, branch from dev) | README, README_en, CONTRIBUTING, AGENTS, CLAUDE, repo-rules.mdc | **Acceptable** — README summary for visitors; CONTRIBUTING is source of truth; others cross-reference. |
| Build/verify commands (mvn compile, mvn test, npm run build) | README, README_en, CONTRIBUTING, AGENTS, CLAUDE, pre-pr-check scripts | **Acceptable** — README for quick start; CONTRIBUTING/AGENTS for PR/review; scripts implement same; minor redundancy. |
| Commit format [type] desc (#issue) | CONTRIBUTING, AGENTS, CLAUDE | **Redundant** — CONTRIBUTING has full table; AGENTS/CLAUDE repeat types. Replace with "See CONTRIBUTING.md § Commit zprávy" in AGENTS/CLAUDE to save tokens. |
| Key paths / repository structure | README, README_en, CLAUDE, AGENTS, .agents/README, review_codebase | **Acceptable** — README for humans; CLAUDE/AGENTS condensed for agents; review_codebase for analysis layout. |
| "web/src/main/webapp/ is NOT the frontend" | CLAUDE, AGENTS, repo-rules.mdc | **Acceptable** — Critical gotcha; repetition intentional. |
| Testing expectations (mvn test, npm build, pre-pr-check) | CONTRIBUTING, AGENTS, README | **Acceptable** — CONTRIBUTING owns detail; AGENTS/README point to it or summarise. |
| Documentation governance (who owns what) | CONTRIBUTING only | **OK** — Single place. |
| Tech stack (Java 17, Angular 21, Saxon, Solr) | README, README_en, AGENTS, CLAUDE | **Acceptable** — README for visitors; AGENTS/CLAUDE for agent context. |
| Agent branches agents/{name}/topic | CONTRIBUTING, AGENTS, repo-rules | **Acceptable** — CONTRIBUTING detail; others reference or summarise. |

**Contradictory duplication:** None found.

**Redundant duplication (same audience):** Commit format repeated in AGENTS.md and CLAUDE.md where a cross-reference would suffice.

---

## C4 — Drift Detection

### 1. Embedded config vs live config — **FAIL**

**review_config.yaml** (live config) uses different `target_file` paths than the prompt and the actual repository layout:

| Task | review_config.yaml | review_codebase.md / actual repo |
|------|--------------------|-----------------------------------|
| T01  | `.agents/repository_map.json` | `.agents/analysis/repository_map.json` |
| T02  | `.agents/dependency_graph.json` | `.agents/analysis/dependency_graph.json` |
| T03–T10 | `.agents/<name>_analysis.json` | `.agents/analysis/<name>_analysis.json` |
| T11  | `.agents/review_reports/final_audit.md` | `.agents/reports/review_reports/final_audit.md` |

**Conflict:** CONTRIBUTING.md states that `.agents/config/review_config.yaml` is the **zdroj pravdy** and that embedded examples in prompts must align with it. In practice, the prompt and the directory structure (and existing files like `repository_map.json`) use `analysis/` and `reports/review_reports/`. So the **config file is out of date** relative to the actual layout and the prompt.

**Which is correct:** The prompt and the repo layout (`.agents/analysis/*.json`, `.agents/reports/review_reports/`) are correct. **review_config.yaml** was updated 2026-03-11 to use these paths.

### 2. Task status definitions — **OK**

Task status semantics are defined in `review_codebase.md` (REVIEW CACHE section) and reflected in `review_cache.json`. No contradiction. `review_config.yaml` defines task list and priorities but does not redefine status semantics; cache is the runtime state. **No drift.**

### 3. Path references (e.g. web/src/main/ng) — **OK**

Consistent across README, README_en, CLAUDE, AGENTS, repo-rules. **No drift.**

### 4. Version numbers (Java 17, Angular 21, Saxon 8.7) — **OK**

Consistent where mentioned. **No drift.**

### 5. Rule conflicts — **OK**

No "do X" vs "don't do X" conflicts between files.

---

## C5 — Cross-Reference Integrity

Verified:

- `CONTRIBUTING.md` exists; sections "Větve a prostředí", "Testy před odesláním PR", "Commit zprávy", "Dokumentační governance", "AI agenti" exist. All "viz CONTRIBUTING.md" / "See CONTRIBUTING.md" refs valid.
- `AGENTS.md` exists; "see CONTRIBUTING.md" and "See CONTRIBUTING.md" valid.
- `CLAUDE.md` → AGENTS.md, CONTRIBUTING.md — valid.
- `.agents/README.md` → `../AGENTS.md` — valid.
- `CONTRIBUTING.md` → `AGENTS.md`, `.agents/prompts/review_codebase.md`, `.agents/config/review_cache.json` — valid.
- `README.md` / `README_en.md` → `CONTRIBUTING.md`, `CITATION.cff`, `.gitignore` — files exist at root.
- `review_codebase.md` → `AGENTS.md`, `.agents/config/review_config.yaml`, `.agents/config/review_cache.json`, `.agents/reports/bugs.md`, etc. — valid.
- Audit prompt example "see CONTRIBUTING.md §Testing" — CONTRIBUTING has "Testy před odesláním PR" (no "§Testing"); no such exact section anchor. **Not broken** — different wording; content exists.

**Ambiguity:** README and README_en list `CODEOWNERS` at repo root in the tree; audit_doc_hygiene.md mentions `.github/CODEOWNERS`. In this repo CODEOWNERS is at **root**, not `.github/`. So the audit prompt’s list is a generic suggestion; repo correctly uses root CODEOWNERS. **No broken ref.**

**Verdict:** No broken or stale cross-references.

---

## C6 — Token Efficiency (AI-specific)

Files likely auto-injected or frequently loaded: **CLAUDE.md**, **AGENTS.md**, **.cursor/rules (repo-rules.mdc, arena-model-sync.mdc)**.

- **CLAUDE.md:** Commit format types list duplicates CONTRIBUTING; build commands duplicate README/CONTRIBUTING. Replacing the commit types with "See CONTRIBUTING.md" would save ~50–80 tokens. Build block is useful for quick reference; keep or shorten to one line + ref.
- **AGENTS.md:** Branch/PR summary and testing block overlap CONTRIBUTING. Already uses "See CONTRIBUTING.md" in several places; "Testing Expectations" and "Branch and PR Rules" could be shortened to 1–2 lines + "See CONTRIBUTING.md" to save ~200–300 tokens.
- **repo-rules.mdc:** Short; points to canonical docs. Low waste.
- **arena-model-sync.mdc:** Local tooling only; no duplication with AGENTS/CLAUDE.

**Rough token savings** if redundancy is reduced: ~300–500 in AGENTS.md (replace repeated workflow/testing with refs), ~50–80 in CLAUDE.md (commit types). Total **~350–580 tokens**; upper bound ~800 if build commands in CLAUDE are also reduced to a single cross-reference.

---

## C7 — Governance Rules Presence

**Present.** CONTRIBUTING.md has a "Dokumentační governance" section that defines:

- README/README_en — public entry; stack and structure.
- CONTRIBUTING.md — source of truth for workflow, tests, security minima.
- AGENTS.md — agent rules; overrides .agents/ when in conflict.
- .agents/ — config, prompts, outputs; generated analysis/reports not edited by hand.
- .agents/config/review_config.yaml — source of truth for review config; prompts must align.

It also states that if information conflicts, update CONTRIBUTING/AGENTS first then align other docs.

**Compliance:** After the 2026-03-11 fix to **review_config.yaml**, the repo complies with the stated governance.

---

## Recommended Fixes

### Critical (FAIL) — ✅ APPLIED 2026-03-11

1. **review_config.yaml target_file paths wrong** — **File:** `.agents/config/review_config.yaml`  
   **Action:** Updated every `target_file` under `tasks` to match the actual layout (`.agents/analysis/*.json` for T01–T10, `.agents/reports/review_reports/final_audit.md` for T11). Added comment that paths are relative to repo root.

### Important (WARN) — ✅ APPLIED 2026-03-11

1. **Commit format duplicated in AGENTS.md and CLAUDE.md** — CLAUDE.md: replaced inline commit types with cross-ref to CONTRIBUTING.md § Commit zprávy. AGENTS.md: shortened "Branch and PR Rules" and "Testing Expectations" to one paragraph each + See CONTRIBUTING.md.
2. **Token efficiency** — AGENTS.md sections shortened as above.

### Optional improvements — ✅ APPLIED 2026-03-11

1. Note in `.agents/prompts/audit_doc_hygiene.md` that this repo uses `CODEOWNERS` at repo root (added in C1 file list).
2. Comment in `.agents/config/review_config.yaml` that `target_file` paths are relative to repo root and use `analysis/` or `reports/review_reports/` as appropriate.
