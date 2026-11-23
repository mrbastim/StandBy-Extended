#!/usr/bin/env python3
"""Fail closed before signing if a stable Android release is mis-versioned.

Only the preexisting public GitHub release assets are downloaded; no signing
secrets are needed. The APK's packaged manifest is the source of truth.
"""
import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path

PACKAGE = "com.mrbastim.standby.extended"
TAG_RE = re.compile(r"^v([0-9]+)\.([0-9]+)\.([0-9]+)$")
PACKAGE_RE = re.compile(
    r"^package:\s+name='([^']+)'\s+versionCode='([0-9]+)'\s+versionName='([^']+)'"
)
ALLOWED_APKS = ("standby-android-extended.apk", "app-release.apk")


def version_tuple(tag):
    match = TAG_RE.fullmatch(tag)
    if not match:
        raise ValueError(f"Invalid stable tag {tag!r}; expected vMAJOR.MINOR.PATCH")
    return tuple(int(value) for value in match.groups())


def parse_badging(text):
    for line in text.splitlines():
        match = PACKAGE_RE.match(line)
        if match:
            package, code, name = match.groups()
            return package, int(code), name
    raise ValueError("Could not read applicationId, versionCode and versionName from APK")


def inspect_apk(aapt, apk):
    result = subprocess.run(
        [aapt, "dump", "badging", str(apk)],
        check=True, capture_output=True, text=True,
    )
    return parse_badging(result.stdout)


def previous_stable_release(releases, tag):
    candidate = version_tuple(tag)
    valid = []
    for release in releases:
        if release.get("draft") or release.get("prerelease"):
            continue
        try:
            version = version_tuple(release.get("tag_name", ""))
        except ValueError:
            continue
        valid.append((version, release))
    newer = [v for v, release in valid if v > candidate]
    if newer:
        raise ValueError(
            f"Refusing to publish {tag} after a newer stable version {max(newer)}"
        )
    older = [(v, release) for v, release in valid if v < candidate]
    return max(older, key=lambda item: item[0])[1] if older else None


def validate_current(tag, identity):
    package, code, name = identity
    if package != PACKAGE:
        raise ValueError(f"Unexpected applicationId: {package}")
    expected = tag[1:]
    if name != expected:
        raise ValueError(f"APK versionName {name!r} does not match tag {tag!r}")
    if code <= 0:
        raise ValueError(f"Invalid versionCode: {code}")


def check_progression(current, previous):
    if current[0] != previous[0]:
        raise ValueError("Previous APK has a different applicationId")
    if current[1] <= previous[1]:
        raise ValueError(
            f"versionCode must increase: current={current[1]}, previous={previous[1]}"
        )


def read_release_list(repo):
    releases = []
    for page in range(1, 11):
        result = subprocess.run(
            ["gh", "api", f"repos/{repo}/releases?per_page=100&page={page}"],
            check=True, capture_output=True, text=True,
        )
        batch = json.loads(result.stdout)
        if not isinstance(batch, list):
            raise ValueError("Unexpected GitHub releases API response")
        releases.extend(batch)
        if len(batch) < 100:
            return releases
    raise ValueError("More than 1000 GitHub releases; adjust pagination")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aapt", required=True)
    args = parser.parse_args()

    version_tuple(args.tag)
    current = inspect_apk(args.aapt, args.apk)
    validate_current(args.tag, current)
    previous_release = previous_stable_release(read_release_list(args.repo), args.tag)

    if previous_release is None:
        print(f"Verified first stable APK {args.tag}: {current}")
        return

    asset = next(
        (asset for candidate in ALLOWED_APKS
         for asset in previous_release.get("assets", [])
         if asset.get("name") == candidate),
        None,
    )
    if asset is None:
        raise ValueError(
            f"Previous release {previous_release['tag_name']} has no expected APK asset"
        )

    with tempfile.TemporaryDirectory() as temp:
        subprocess.run(
            ["gh", "release", "download", previous_release["tag_name"],
             "--repo", args.repo, "--pattern", asset["name"], "--dir", temp],
            check=True,
        )
        previous_apk = Path(temp) / asset["name"]
        previous = inspect_apk(args.aapt, previous_apk)
        check_progression(current, previous)
    print(
        f"Verified {args.tag} ({current[1]}) is newer than "
        f"{previous_release['tag_name']} ({previous[1]})"
    )


if __name__ == "__main__":
    main()
