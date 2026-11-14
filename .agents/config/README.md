# .agents/config

Configuration and persistent state for the repository's codebase-review workflow.

## Files

- [`review_config.toml`](review_config.toml) declares the repository's technology stack, review tasks, ordering, scope, exclusions, and relevant limits. It is the owning source for these facts; reference it rather than duplicating them in prompts or other documents.
- `review_cache.json` is the persistent review-state store. It records task progress, review timing, file hashes, and latest-review evidence as managed by the review engine.

The current engine is [`../scripts/codebase_review.py`](../scripts/codebase_review.py), driven by [`aiscr-review-codebase`](../skills/aiscr-review-codebase/SKILL.md). Use the approved workflow to update configuration and review state; an out-of-date cache entry is evidence to investigate, not a reason to reset unrelated history.

