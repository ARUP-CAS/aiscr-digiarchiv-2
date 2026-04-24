# Pre-PR check: compile Java, run Maven test, build Angular.
# Run from repository root. See CONTRIBUTING.md and AGENTS.md.
$ErrorActionPreference = 'Stop'
Write-Host "Running Java compile and test (web/)..."
Push-Location web
try {
    mvn compile -q
    mvn test
} finally {
    Pop-Location
}
Write-Host "Running Angular build (web/src/main/ng/)..."
Push-Location web/src/main/ng
try {
    npm run build
} finally {
    Pop-Location
}
Write-Host "Pre-PR check finished."
