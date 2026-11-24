"""Retry unit tests only when all reported failures are Robolectric download I/O errors."""
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESULTS = ROOT / "app/build/test-results/testDebugUnitTest"
RETRY_DELAYS = (10, 20, 40)
NETWORK_MARKERS = (
    "HttpURLConnection", "HttpsURLConnection", "java.net.SocketTimeoutException",
    "java.net.ConnectException", "java.net.UnknownHostException",
    "java.net.SocketException", "javax.net.ssl.SSLException",
)


def only_robolectric_download_failures(results):
    """Fail closed for absent/malformed reports, non-download errors or mixed failures."""
    failures = []
    try:
        for report in sorted(results.glob("TEST-*.xml")):
            root = ET.parse(report).getroot()
            if root.tag != "testsuite":
                return False
            reported = int(root.get("failures", "0")) + int(root.get("errors", "0"))
            actual = root.findall(".//testcase/failure") + root.findall(".//testcase/error")
            if len(actual) != reported:
                return False
            failures.extend(actual)
    except (OSError, ValueError, ET.ParseError):
        return False
    return bool(failures) and all(
        "MavenArtifactFetcher" in trace
        and ("java.io.IOException" in trace or any(
            marker in trace for marker in NETWORK_MARKERS[2:]
        ))
        and any(marker in trace for marker in NETWORK_MARKERS)
        for failure in failures
        for trace in [" ".join((failure.get("type", ""), failure.get("message", ""), failure.text or ""))]
    )


def main():
    for attempt in range(len(RETRY_DELAYS) + 1):
        # Do not use stale reports to decide whether this attempt can be retried.
        if RESULTS.exists():
            shutil.rmtree(RESULTS)
        print(f"Unit tests: attempt {attempt + 1}/{len(RETRY_DELAYS) + 1}", flush=True)
        status = subprocess.run(
            ["./gradlew", "--no-daemon", "testDebugUnitTest"], cwd=ROOT,
            check=False,
        ).returncode
        if status == 0:
            return 0
        if attempt == len(RETRY_DELAYS) or not only_robolectric_download_failures(RESULTS):
            return status if status > 0 else 1
        delay = RETRY_DELAYS[attempt]
        print(f"::warning::Robolectric dependency download failed; retrying in {delay}s", flush=True)
        time.sleep(delay)
    return 1


if __name__ == "__main__":
    sys.exit(main())
