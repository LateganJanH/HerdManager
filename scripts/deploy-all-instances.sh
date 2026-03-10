#!/usr/bin/env bash
# Deploy Cloud Functions and Firestore rules to every instance in the solution registry
# that has a non-empty firebaseProjectId. Run from repo root.
# Prerequisites: Firebase CLI logged in, node scripts/list-solutions.js --project-ids returns IDs.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

echo "Validating registry..."
node scripts/validate-registry.js

echo "Fetching deployable project IDs..."
PROJECT_IDS=$(node scripts/list-solutions.js --project-ids)
if [ -z "$PROJECT_IDS" ]; then
  echo "No solutions with firebaseProjectId set. Add projects with: node scripts/update-solution.js <solutionId> --firebase-project-id <id>"
  exit 1
fi

for projectId in $PROJECT_IDS; do
  echo "--- Deploying to $projectId ---"
  firebase use "$projectId" && firebase deploy --only functions,firestore:rules
done

echo "Done."
