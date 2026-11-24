import unittest

from check_release_metadata import (
    check_progression, parse_badging, previous_stable_release,
    validate_current, version_tuple,
)


def release(tag, *, draft=False, prerelease=False):
    return {"tag_name": tag, "draft": draft, "prerelease": prerelease}


class ReleaseMetadataTest(unittest.TestCase):
    def test_reads_packaged_android_manifest(self):
        self.assertEqual(
            parse_badging("package: name='com.mrbastim.standbymode' "
                          "versionCode='8' versionName='1.0.7'\n"),
            ("com.mrbastim.standbymode", 8, "1.0.7"),
        )

    def test_tag_and_app_id_must_match(self):
        validate_current("v1.0.7", ("com.mrbastim.standbymode", 8, "1.0.7"))
        with self.assertRaises(ValueError):
            validate_current("v1.0.8", ("com.mrbastim.standbymode", 8, "1.0.7"))
        with self.assertRaises(ValueError):
            validate_current("v1.0.7", ("com.example.other", 8, "1.0.7"))

    def test_invalid_release_tag_rejected(self):
        for tag in ("dev", "v1.0.7-rc1", "v1.0", "release1.0.7"):
            with self.assertRaises(ValueError):
                version_tuple(tag)

    def test_previous_stable_not_dev_or_draft(self):
        releases = [
            release("v1.0.5"), release("v1.0.6"),
            release("v1.0.7", draft=True), release("dev", prerelease=True),
        ]
        self.assertEqual(
            previous_stable_release(releases, "v1.0.7")["tag_name"], "v1.0.6"
        )

    def test_downgrade_rejected(self):
        with self.assertRaises(ValueError):
            previous_stable_release([release("v1.0.8")], "v1.0.7")

    def test_v110_rebrand_allows_only_known_legacy_id(self):
        old = ("com.mrbastim.standby.extended", 7, "1.0.6")
        new = ("com.mrbastim.standbymode", 8, "1.1.0")
        check_progression(new, old, "v1.1.0")
        with self.assertRaises(ValueError):
            check_progression(new, old, "v1.1.1")
        with self.assertRaises(ValueError):
            check_progression(("com.other.app", 8, "1.1.0"), old, "v1.1.0")
        with self.assertRaises(ValueError):
            check_progression(new, ("com.other.app", 7, "1.0.6"), "v1.1.0")
        with self.assertRaises(ValueError):
            check_progression(("com.mrbastim.standbymode", 7, "1.1.0"), old, "v1.1.0")

    def test_version_code_must_increase(self):
        old = ("com.mrbastim.standbymode", 7, "1.0.6")
        new = ("com.mrbastim.standbymode", 8, "1.0.7")
        check_progression(new, old)
        with self.assertRaises(ValueError):
            check_progression(("com.mrbastim.standbymode", 7, "1.0.7"), old)


if __name__ == "__main__":
    unittest.main()
