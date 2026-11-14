# .agents/reports

Durable outputs from the repository's codebase-review and validation work.

## Structure

- `review_reports/` contains per-task and consolidated review reports. See [`review_reports/README.md`](review_reports/README.md).
- Finding and backlog records use the source and rendered formats supported by the current review configuration and its engine.
- Testing-scenario publications and redacted extracts preserve evidence without turning those extracts into live production data or real personal data.
- `incidents/` holds incident reports produced from the delivered [`incident_postmortem` template](../canonical_configs/templates/documents/incident_postmortem.md).

## Operating guidance

- Use [`aiscr-review-codebase`](../skills/aiscr-review-codebase/SKILL.md) as the operational source for review lifecycle, modes, and report handling.
- Keep new review-workflow prose English. Preserve exact Czech quotations, source comments, documentation excerpts, GitHub issue titles, and AIS CR domain identifiers when their literal wording is evidence.
- Apply the severity, admissibility, provenance, and finding-disposition requirements in the [shared review-conduct reader](../../.github/instructions/aiscr-review-conduct.instructions.md).
- Preserve analytical history and review provenance. Report-only artifacts do not authorize repairs, source changes, or issue-tracking writes.
- Ecosystem-wide documentation audits are requested from the management hub rather than recreated as sibling-local workflow prompts or duplicate tooling.

