# Full onboarding: create solution + GCP project + (optional) add Firebase and deploy.
# Use with INSTANCE-PER-FARM-STRATEGY and PROVISIONING-RUNBOOK.
#
# Usage (from repo root):
#   .\scripts\onboard-farm.ps1 -FarmName "Farm Name" -ProjectId <gcp-project-id> -ParentId <numeric-id> -ParentType org|folder [-BillingAccount "billingAccounts/XXX"] [-Deploy]
#
# Example:
#   $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\sa.json"
#   .\scripts\onboard-farm.ps1 -FarmName "Acme Farm" -ProjectId acme-farm-prod -ParentId 123456789 -ParentType org -Deploy
#
# Prerequisites: solution-registry.json exists or will be created; GOOGLE_APPLICATION_CREDENTIALS set;
# ParentId is numeric org or folder ID; service account has resourcemanager.projects.create (and Firebase perms if -Deploy).

param(
    [Parameter(Mandatory = $true)]
    [string]$FarmName,
    [Parameter(Mandatory = $true)]
    [string]$ProjectId,
    [Parameter(Mandatory = $true)]
    [string]$ParentId,
    [Parameter(Mandatory = $true)]
    [ValidateSet("org", "folder")]
    [string]$ParentType,
    [string]$BillingAccount = "",
    [switch]$Deploy
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
Set-Location $RootDir

Write-Host "Creating solution for: $FarmName"
$SolutionId = node scripts/create-solution.js --name $FarmName --print-solution-id
$SolutionId = $SolutionId.Trim()
Write-Host "Solution ID: $SolutionId"
Write-Host ""

$CreateArgs = @(
    "scripts/create-gcp-project.js",
    $SolutionId,
    "--project-id", $ProjectId,
    "--name", $FarmName
)
if ($ParentType -eq "org") {
    $CreateArgs += "--org-id", $ParentId
} else {
    $CreateArgs += "--folder-id", $ParentId
}
if ($BillingAccount) {
    $CreateArgs += "--billing-account", $BillingAccount
}
if ($Deploy) {
    $CreateArgs += "--deploy"
}

Write-Host "Creating GCP project and (optionally) adding Firebase..."
& node $CreateArgs
Write-Host ""
Write-Host "Onboarding complete for solution: $SolutionId"
Write-Host "Next: node scripts/env-for-solution.js $SolutionId [--support-url <url>]"
