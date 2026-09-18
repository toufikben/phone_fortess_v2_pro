#!/usr/bin/env python3
"""Verify that the release AAB declares 16 KB native-library page alignment."""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_AAB = ROOT / "app/build/outputs/bundle/release/app-release.aab"

def fail(message: str) -> "NoReturn":
    print(f"AAB 16 KB verification failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def resolve_aab() -> Path:
    candidate = Path(os.environ.get("AAB_PATH", DEFAULT_AAB))
    if not candidate.is_absolute():
        candidate = ROOT / candidate
    if not candidate.is_file() or candidate.stat().st_size == 0:
        fail(f"release AAB is missing or empty: {candidate}")
    return candidate


def resolve_bundletool() -> Path:
    configured = os.environ.get("BUNDLETOOL_JAR")
    if configured:
        jar = Path(configured)
        if not jar.is_file() or jar.stat().st_size == 0:
            fail(f"BUNDLETOOL_JAR does not point to a usable file: {jar}")
        return jar
    bundled = ROOT / "tools" / "bundletool.jar"
    if bundled.is_file() and bundled.stat().st_size:
        return bundled
    fail("BUNDLETOOL_JAR is not set; refusing to claim AAB verification without bundletool")


def main() -> None:
    aab = resolve_aab()
    bundletool = resolve_bundletool()
    java = shutil.which("java")
    if java is None:
        fail("java executable is unavailable")

    command = [java, "-jar", str(bundletool), "dump", "config", "--bundle", str(aab)]
    print(f"bundletool: {bundletool}")
    print(f"AAB: {aab}")
    print(f"command: {' '.join(command)}")
    result = subprocess.run(command, text=True, capture_output=True)
    if result.returncode != 0:
        fail(result.stderr.strip() or f"bundletool exited with status {result.returncode}")

    output = result.stdout
    if "PAGE_ALIGNMENT_16K" not in output:
        print(output, end="")
        fail("bundletool output does not contain PAGE_ALIGNMENT_16K")

    lines = [line for line in output.splitlines() if "PAGE_ALIGNMENT_16K" in line]
    print("AAB PAGE_ALIGNMENT_16K: VERIFIED")
    print("Evidence:")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
