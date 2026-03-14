#!/usr/bin/env bash
#
# Full onboarding: create solution + GCP project + (optional) add Firebase and deploy.
# Use with INSTANCE-PER-FARM-STRATEGY and PROVISIONING-RUNBOOK.
#
# Usage (from repo root):
#   ./scripts/onboard-farm.sh "Farm Name" <gcp-project-id> <parent-id> [org|folder] [--billing billingAccounts/XXX] [--deploy]
#
# Example:
#   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/sa.json"
#   ./scripts/onboard-farm.sh "Acme Farm" acme-farm-prod 123456789 org --deploy
#
# Prerequisites: solution-registry.json exists or will be created; GOOGLE_APPLICATION_CREDENTIALS set;
# parent-id is numeric org or folder ID; service account has resourcemanager.projects.create (and Firebase perms if --deploy).

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

FARM_NAME="${1:?Usage: onboard-farm.sh \"Farm Name\" <gcp-project-id> <parent-id> org|folder [--billing ...] [--deploy]}"
PROJECT_ID="${2:?}"
PARENT_ID="${3:?}"
PARENT_TYPE="${4:-org}"
shift 4 || true
BILLING=""
DEPLOY=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --billing) BILLING="$2"; shift 2 ;;
    --deploy)  DEPLOY="--deploy"; shift ;;
    *) shift ;;
  esac
done

if [[ "$PARENT_TYPE" != "org" && "$PARENT_TYPE" != "folder" ]]; then
  echo "Parent type must be 'org' or 'folder'"
  exit 1
fi

echo "Creating solution for: $FARM_NAME"
SOLUTION_ID="$(node scripts/create-solution.js --name "$FARM_NAME" --print-solution-id)"
echo "Solution ID: $SOLUTION_ID"
echo ""

ARGS="node scripts/create-gcp-project.js $SOLUTION_ID --project-id $PROJECT_ID --name \"$FARM_NAME\""
if [[ "$PARENT_TYPE" == "org" ]]; then
  ARGS="$ARGS --org-id $PARENT_ID"
else
  ARGS="$ARGS --folder-id $PARENT_ID"
fi
[[ -n "$BILLING" ]] && ARGS="$ARGS --billing-account $BILLING"
[[ -n "$DEPLOY" ]] && ARGS="$ARGS $DEPLOY"

echo "Creating GCP project and (optionally) adding Firebase..."
eval "$ARGS"
echo ""
echo "Onboarding complete for solution: $SOLUTION_ID"
echo "Next: node scripts/env-for-solution.js $SOLUTION_ID [--support-url <url>]"
