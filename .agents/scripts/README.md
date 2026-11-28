# .agents/scripts

Command-line helpers for the repository's reviewed workflows. Run them through the repository virtualenv where one exists and follow the corresponding workflow's approval requirements.

## Codebase review

- `codebase_review.py` is the canonical engine for artifact validation, review-state reporting, and finding/backlog rendering. It requires Python 3.12 or newer and reads [`../config/review_config.toml`](../config/review_config.toml).
- Use `--help` for the current subcommands and named options. For example, `python .agents/scripts/codebase_review.py --help` describes the CLI without modifying review state.
- [`github/`](../skills/aiscr-review-pr/SKILL.md) contains the deterministic PR-review helpers. The workflow governs acquisition, approval, payloads, and posting; do not replace those controls with direct ad hoc GitHub writes.

`validate_release_yml.py` is bundled for release-configuration validation. Documentation-hygiene and link audits are requested from the management hub instead of maintaining duplicate local discovery and checking scripts.

Before changing a helper, check `.agents/delivery-manifest.json` for its delivery status and protect all delivered mirrors and safety controls from local divergence.

