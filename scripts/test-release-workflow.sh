#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - "$repo_root/.github/workflows/release.yml" <<'PY'
from pathlib import Path
import sys
import yaml

document = yaml.safe_load(Path(sys.argv[1]).read_text())
for job_name in ("snapshot", "release"):
    steps = document["jobs"][job_name]["steps"]
    installs = [step for step in steps if step.get("name") == "Install Android SDK 37.1"]
    if len(installs) != 1:
        raise SystemExit(f"FAIL: {job_name} must install Android SDK 37.1 exactly once")
    run = installs[0]["run"]
    if 'cmdline-tools/latest/bin/sdkmanager' not in run or '"platforms;android-37.1"' not in run:
        raise SystemExit(f"FAIL: {job_name} does not install platforms;android-37.1")
    print(f"PASS: {job_name} installs platforms;android-37.1")
PY
