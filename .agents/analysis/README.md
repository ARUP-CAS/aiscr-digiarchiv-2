# .agents/analysis

Machine-readable JSON analyses produced for configured codebase-review tasks. [`../config/review_config.toml`](../config/review_config.toml) owns task identities, scope, and ordering.

Typical evidence includes repository maps, dependency graphs, and task-specific analyses of Solr, XSLT, containers, security, frontend, documentation, CI/CD, or scripts. File naming follows the review configuration; `repository_map.json` and `dependency_graph.json` provide structural and dependency context.

These files retain evidence for comparison across review runs and for final audits and refactoring-backlog preparation. Historical snapshots may describe a previous review configuration or repository state: check their metadata against the current tree before reusing their conclusions.

