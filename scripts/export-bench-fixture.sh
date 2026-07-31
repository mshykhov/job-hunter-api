#!/bin/bash
# Export a provider-benchmark fixture from production. Reads through kubectl exec
# into the CNPG primary and writes src/test/resources/bench/fixture.local.json +
# labels.local.json.
#
# Only groups that carry a user_job_groups row are eligible: matched_at alone also
# covers groups the cold filter rejected before any AI call, and scoring those would
# benchmark the models on input production never sends them. The sample is stratified
# across the incumbent scores (4 high / 3 mid / 3 low) so the fixture spans the
# relevance boundary instead of collapsing into one class, and the representative job
# per group is the longest description - the same one JobRelevanceEvaluator picks.
# Both *.local.json files are gitignored - they hold the owner's real profile
# text and must never be committed.
set -euo pipefail

NAMESPACE="job-hunter-api-prd"
POD="job-hunter-api-main-db-prd-cluster-1"
CONTAINER="postgres"
DB="jobhunter"

DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$DIR/src/test/resources/bench"
FIXTURE_FILE="$OUT_DIR/fixture.local.json"
LABELS_FILE="$OUT_DIR/labels.local.json"

command -v kubectl >/dev/null 2>&1 || { echo "ERROR: kubectl is required" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "ERROR: jq is required" >&2; exit 1; }

psql_exec() {
  kubectl exec -n "$NAMESPACE" "$POD" -c "$CONTAINER" -- psql -U postgres -d "$DB" -tAc "$1"
}

if ! psql_exec "SELECT 1;" >/dev/null 2>&1; then
  echo "ERROR: cannot reach $POD in namespace $NAMESPACE via kubectl exec." >&2
  echo "Check cluster connectivity (kubectl config current-context, Tailscale) and retry." >&2
  exit 1
fi

read -r -d '' QUERY <<'SQL' || true
WITH pref AS (
    SELECT about, categories, locations, excluded_keywords, remote_only, custom_prompt
    FROM user_preferences
    ORDER BY updated_at DESC NULLS LAST
    LIMIT 1
),
scored_groups AS (
    SELECT DISTINCT ON (j.group_id)
        j.id, j.title, j.company, j.url, j.description, j.location, j.salary, j.remote, j.matched_at,
        u.ai_relevance_score
    FROM jobs j
    JOIN user_job_groups u ON u.group_id = j.group_id
    WHERE j.matched_at IS NOT NULL
    ORDER BY j.group_id, length(j.description) DESC
),
top_jobs AS (
    (SELECT * FROM scored_groups WHERE ai_relevance_score >= 70 ORDER BY matched_at DESC LIMIT 4)
    UNION ALL
    (SELECT * FROM scored_groups WHERE ai_relevance_score BETWEEN 40 AND 69 ORDER BY matched_at DESC LIMIT 3)
    UNION ALL
    (SELECT * FROM scored_groups WHERE ai_relevance_score < 40 ORDER BY matched_at DESC LIMIT 3)
)
SELECT json_build_object(
    'preference', (SELECT json_build_object(
        'categories', COALESCE(categories, '[]'::jsonb),
        'remoteOnly', remote_only,
        'locations', COALESCE(to_jsonb(locations), '[]'::jsonb),
        'excludedKeywords', COALESCE(to_jsonb(excluded_keywords), '[]'::jsonb),
        'about', about,
        'customPrompt', custom_prompt
    ) FROM pref),
    'jobs', COALESCE((SELECT json_agg(json_build_object(
        'id', id,
        'title', title,
        'company', company,
        'location', location,
        'salary', salary,
        'remote', remote,
        'description', description
    ) ORDER BY matched_at DESC) FROM top_jobs), '[]'::json)
);
SQL

mkdir -p "$OUT_DIR"

if ! psql_exec "$QUERY" | jq . > "$FIXTURE_FILE.tmp"; then
  echo "ERROR: query did not return valid JSON, nothing written." >&2
  rm -f "$FIXTURE_FILE.tmp"
  exit 1
fi
mv "$FIXTURE_FILE.tmp" "$FIXTURE_FILE"

JOB_COUNT=$(jq '.jobs | length' "$FIXTURE_FILE")
if [ "$JOB_COUNT" -eq 0 ]; then
  echo "ERROR: no matched job groups found; fixture would be empty." >&2
  rm -f "$FIXTURE_FILE"
  exit 1
fi

echo "Wrote $FIXTURE_FILE ($JOB_COUNT jobs)"

if [ -e "$LABELS_FILE" ]; then
  echo "Skipped $LABELS_FILE (already exists - not overwriting your labels)"
else
  jq '[.jobs[].id] | map({(.): {relevant: null, score: null}}) | add // {}' "$FIXTURE_FILE" > "$LABELS_FILE"
  echo "Wrote $LABELS_FILE (fill in relevant/score for each job id)"
fi

echo
echo "Next steps:"
echo "  1. Open $LABELS_FILE and set relevant (true/false) and score (0-100) for every job."
echo "  2. Run: JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew bench"
