# .agents/

Repository-specific review state and shared tooling for AI-assisted work. Governance starts with [AGENTS.md](../AGENTS.md), and the human contribution workflow is defined in [CONTRIBUTING.md](../CONTRIBUTING.md).

## Structure

- `analysis/` — machine-readable findings from configured codebase-review tasks, including repository maps and dependency graphs.
- `config/` — review configuration and persistent review state.
- `reports/` — review findings, task reports, consolidated audits, and other durable evidence.
- `scripts/` — the canonical review engine and GitHub review helpers.
- `canonical_configs/` — references and document/workstation templates selected for delivery to this repository.
- `skills/` — repository-local delivered workflow surfaces, including [`aiscr-review-codebase`](skills/aiscr-review-codebase/SKILL.md).

## Authority and ownership

- `config/review_config.toml` owns review task definitions, scope, paths, and thresholds. `scripts/codebase_review.py` is the corresponding engine, and `config/review_cache.json` records its persistent progress and hashes.
- Delivered workflow bodies, helper libraries, references, and templates are governed by their upstream sources. Do not patch delivered copies as though they were repository-native code.
- Repository-owned configuration, review state, analytical evidence, and operational guidance remain local and must not be treated as another hub mirror.
- Documentation-hygiene and related ecosystem-wide audits are requested from the management hub rather than maintained as duplicate local prompts and discovery helpers.
- Preserve analytical history and review provenance; do not erase cache or report state merely to make the current layout look cleaner.

