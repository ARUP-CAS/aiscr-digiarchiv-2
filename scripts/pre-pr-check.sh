#!/usr/bin/env bash
# Pre-PR check: compile Java, run Maven test, build Angular.
# Run from repository root. See CONTRIBUTING.md and AGENTS.md.
set -e
echo "Running Java compile and test (web/)..."
(cd web && mvn compile -q && mvn test)
echo "Running Angular build (web/src/main/ng/)..."
(cd web/src/main/ng && npm run build)
echo "Pre-PR check finished."
