<!-- SPDX-FileCopyrightText: 2026 AIS CR (Archaeological Information System of the Czech Republic) <https://ror.org/01a7rqj69> -->
<!-- SPDX-License-Identifier: CC-BY-4.0 -->
<!-- delivery-version: 713f5616b3faf0de584fb8fc820fb9bfa4233451 2026-11-14T23:05:33+01:00 -->

# schemas – JSON Schema for workstation_safety

The schemas here validate selected JSON templates **before** [`setup_workstation_assistant_safety.py`](../../../../scripts/setup_workstation_assistant_safety.py) writes them.

| File | Validates |
| ---- | --------- |
| `claude_safety_snippet.schema.json` | `../claude_safety_snippet.json` |

The Codex template `../codex_user_config.toml` is checked via Python (`tomllib` plus required keys: `approval_policy` and top-level `sandbox_mode`), not JSON Schema in this folder.

When template fields change, update the matching schema and the script tests under `.agents/scripts/tests/`.
