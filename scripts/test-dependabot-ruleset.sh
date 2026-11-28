#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workflow="$repo_root/.github/workflows/dependabot-auto-merge.yml"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/dependabot-ruleset.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

python3 - "$workflow" "$tmp_dir" <<'PY'
import json
from pathlib import Path
import sys
import yaml

workflow_path = Path(sys.argv[1])
out = Path(sys.argv[2])
document = yaml.safe_load(workflow_path.read_text())
step = next(
    step for step in document["jobs"]["auto-merge"]["steps"]
    if step.get("name") == "Verify required merge protection"
)
(out / "predicate.jq").write_text(step["env"]["BRANCH_RULES_PREDICATE"])

run = step["run"]
if run.count("gh api ") != 1:
    raise SystemExit("FAIL: preflight must make exactly one branch-rules API call")
if 'repos/$GITHUB_REPOSITORY/rules/branches/main?per_page=100' not in run:
    raise SystemExit("FAIL: paginated main branch-rules endpoint is missing")
if "--paginate --slurp" not in run:
    raise SystemExit("FAIL: main branch rules must be read with pagination")
for forbidden in ("branches/main/protection", "/rulesets"):
    if forbidden in run:
        raise SystemExit(f"FAIL: removed protection endpoint is still referenced: {forbidden}")
print("PASS: only the paginated main branch-rules endpoint is used")

required_rule = {
    "type": "required_status_checks",
    "parameters": {
        "required_status_checks": [{"context": "Test, lint, and package"}],
    },
}
fixtures = {
    "valid-main": [[required_rule]],
    "valid-second-page": [[{"type": "pull_request", "parameters": {}}], [required_rule]],
    "missing-context": [[{
        "type": "required_status_checks",
        "parameters": {"required_status_checks": [{"context": "Build and unit tests"}]},
    }]],
    "wrong-type": [[required_rule | {"type": "pull_request"}]],
    "empty": [[]],
}
for name, fixture in fixtures.items():
    (out / f"{name}.json").write_text(json.dumps(fixture))
PY

accept() {
    local name="$1"
    if ! jq -e --arg required 'Test, lint, and package' \
        -f "$tmp_dir/predicate.jq" "$tmp_dir/$name.json" > /dev/null; then
        echo "FAIL: $name should be accepted" >&2
        return 1
    fi
    echo "PASS: accepted $name"
}

reject() {
    local name="$1"
    local status
    if jq -e --arg required 'Test, lint, and package' \
        -f "$tmp_dir/predicate.jq" "$tmp_dir/$name.json" > /dev/null; then
        echo "FAIL: $name should be rejected" >&2
        return 1
    else
        status=$?
    fi
    if [ "$status" -ne 1 ]; then
        echo "FAIL: jq could not evaluate $name (exit $status)" >&2
        return "$status"
    fi
    echo "PASS: rejected $name"
}

accept valid-main
accept valid-second-page
reject missing-context
reject wrong-type
reject empty
