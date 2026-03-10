# Deploy Cloud Functions and Firestore rules to every instance in the solution registry
# that has a non-empty firebaseProjectId. Run from repo root.
# Prerequisites: Firebase CLI logged in; node scripts/list-solutions.js --project-ids returns IDs.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
Set-Location $RootDir

Write-Host "Validating registry..."
node scripts/validate-registry.js
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Fetching deployable project IDs..."
$projectIds = node scripts/list-solutions.js --project-ids 2>&1 | Where-Object { $_ -match "\S" }
if (-not $projectIds) {
    Write-Host "No solutions with firebaseProjectId set. Add projects with: node scripts/update-solution.js <solutionId> --firebase-project-id <id>"
    exit 1
}

foreach ($projectId in $projectIds) {
    Write-Host "--- Deploying to $projectId ---"
    firebase use $projectId
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    firebase deploy --only functions,firestore:rules
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Done."
