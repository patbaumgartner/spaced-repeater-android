#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workflow="$repo_root/.github/workflows/dependabot-auto-merge.yml"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/dependabot-update-type.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

python3 - "$workflow" "$tmp_dir" <<'PY'
from pathlib import Path
import sys
import yaml

workflow_path = Path(sys.argv[1])
out = Path(sys.argv[2])
document = yaml.safe_load(workflow_path.read_text())
steps = document["jobs"]["auto-merge"]["steps"]
by_name = {step.get("name"): step for step in steps}
validation = by_name["Verify update is patch or minor"]
(out / "validate-update-type.sh").write_text(validation["run"])

expected = (
    "steps.metadata.outputs.update-type == 'version-update:semver-patch' "
    "|| steps.metadata.outputs.update-type == 'version-update:semver-minor'"
)
for name in (
    "Verify required merge protection",
    "Verify auto-merge token",
    "Enable auto-merge for patch and minor updates",
):
    condition = " ".join(by_name[name]["if"].split())
    if condition != expected:
        raise SystemExit(f"FAIL: {name} has an ineligible-update condition: {condition}")
print("PASS: preflight, token guard, and merge require explicit semver-patch/minor")

fetch = by_name["Fetch Dependabot metadata"]
preflight = by_name["Verify required merge protection"]
guard = by_name["Verify auto-merge token"]
merge = by_name["Enable auto-merge for patch and minor updates"]
if fetch["with"]["github-token"] != "${{ secrets.GITHUB_TOKEN }}":
    raise SystemExit("FAIL: metadata fetching must keep GITHUB_TOKEN")
if preflight["env"]["GH_TOKEN"] != "${{ secrets.GITHUB_TOKEN }}":
    raise SystemExit("FAIL: branch-rules preflight must keep GITHUB_TOKEN")
if guard["env"]["AUTO_MERGE_TOKEN"] != "${{ secrets.AUTO_MERGE_TOKEN }}":
    raise SystemExit("FAIL: token guard must read AUTO_MERGE_TOKEN")
if merge["env"]["GH_TOKEN"] != "${{ secrets.AUTO_MERGE_TOKEN }}":
    raise SystemExit("FAIL: only the actual merge must use AUTO_MERGE_TOKEN")
names = [step.get("name") for step in steps]
if names.index("Verify auto-merge token") + 1 != names.index("Enable auto-merge for patch and minor updates"):
    raise SystemExit("FAIL: nonempty-token guard must immediately precede the merge")
(out / "verify-auto-merge-token.sh").write_text(guard["run"])
print("PASS: metadata/preflight use GITHUB_TOKEN; only merge uses AUTO_MERGE_TOKEN")
PY

accept_update() {
    local update_type="$1"
    local label="$2"
    if ! UPDATE_TYPE="$update_type" bash "$tmp_dir/validate-update-type.sh" > /dev/null; then
        echo "FAIL: $label should be accepted" >&2
        return 1
    fi
    echo "PASS: accepted $label"
}

reject_update() {
    local update_type="$1"
    local label="$2"
    local status
    if UPDATE_TYPE="$update_type" bash "$tmp_dir/validate-update-type.sh" > /dev/null 2>&1; then
        echo "FAIL: $label should be rejected" >&2
        return 1
    else
        status=$?
    fi
    if [ "$status" -ne 1 ]; then
        echo "FAIL: could not evaluate $label (exit $status)" >&2
        return "$status"
    fi
    echo "PASS: rejected $label"
}

accept_update 'version-update:semver-patch' patch
accept_update 'version-update:semver-minor' minor
reject_update '' empty
reject_update 'version-update:semver-major' major
reject_update 'version-update:semver-unknown' unknown

if ! AUTO_MERGE_TOKEN='mock-not-a-secret' bash "$tmp_dir/verify-auto-merge-token.sh"; then
    echo 'FAIL: nonempty token guard rejected a placeholder value' >&2
    exit 1
fi
echo 'PASS: token guard accepted a nonempty placeholder'

output=''
status=0
if output="$(AUTO_MERGE_TOKEN='' bash "$tmp_dir/verify-auto-merge-token.sh" 2>&1)"; then
    echo 'FAIL: empty AUTO_MERGE_TOKEN should be rejected' >&2
    exit 1
else
    status=$?
fi
if [ "$status" -ne 1 ]; then
    echo "FAIL: token guard returned an unexpected error (exit $status)" >&2
    exit "$status"
fi
case "$output" in
    *'AUTO_MERGE_TOKEN is not set or is empty.'*'CONTRIBUTING.md#dependabot-auto-merge-token'*)
        echo 'PASS: empty token rejected with actionable setup guidance'
        ;;
    *)
        echo 'FAIL: empty-token error is not actionable' >&2
        exit 1
        ;;
esac
