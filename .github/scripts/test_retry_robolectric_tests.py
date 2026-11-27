import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from types import SimpleNamespace
import xml.etree.ElementTree as ET

import retry_robolectric_tests as retry

NETWORK = "java.lang.AssertionError: Failed to fetch artifact\n at org.robolectric.internal.dependency.MavenArtifactFetcher.fetchArtifact\nCaused by: java.io.IOException: HTTP 503\n at java.net.HttpURLConnection.getInputStream(HttpURLConnection.java:2059)"


class RetryRobolectricTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.results = Path(self.tmp.name) / "reports"

    def report(self, *traces):
        self.results.mkdir(exist_ok=True)
        root = ET.Element("testsuite", failures=str(len(traces)), errors="0")
        for index, trace in enumerate(traces):
            test = ET.SubElement(root, "testcase", name=str(index))
            ET.SubElement(test, "failure").text = trace
        ET.ElementTree(root).write(self.results / "TEST-example.xml")

    def test_only_download_failures(self):
        self.report(NETWORK, NETWORK)
        self.assertTrue(retry.only_robolectric_download_failures(self.results))

    def test_mixed_and_regular_errors_are_not_retried(self):
        for traces in [(NETWORK, "java.lang.AssertionError: expected 1 but was 2"),
                       ("java.io.IOException at HttpURLConnection.java:2059",),
                       ("MavenArtifactFetcher checksum mismatch",)]:
            with self.subTest(traces=traces):
                self.report(*traces)
                self.assertFalse(retry.only_robolectric_download_failures(self.results))

    def test_missing_empty_and_malformed_report(self):
        self.assertFalse(retry.only_robolectric_download_failures(self.results))
        self.report()
        self.assertFalse(retry.only_robolectric_download_failures(self.results))
        (self.results / "TEST-example.xml").write_text("<broken")
        self.assertFalse(retry.only_robolectric_download_failures(self.results))

    def run_attempts(self, statuses, trace=NETWORK):
        statuses = iter(statuses)
        def gradle(*args, **kwargs):
            result = next(statuses)
            if result:
                self.report(trace)
            return SimpleNamespace(returncode=result)
        with patch.object(retry, "RESULTS", self.results), \
             patch.object(retry.subprocess, "run", side_effect=gradle) as run, \
             patch.object(retry.time, "sleep") as sleep:
            status = retry.main()
            return status, run.call_count, [call.args[0] for call in sleep.call_args_list]

    def test_three_retries_then_success(self):
        self.assertEqual((0, 4, [10, 20, 40]), self.run_attempts([1, 1, 1, 0]))

    def test_retries_exhausted_fails(self):
        self.assertEqual((1, 4, [10, 20, 40]), self.run_attempts([1, 1, 1, 1]))

    def test_real_failure_stops_immediately(self):
        self.assertEqual((1, 1, []), self.run_attempts([1], "java.lang.AssertionError"))
