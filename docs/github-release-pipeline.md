# GitHub release pipeline

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

The workflow in `.github/workflows/release.yml` builds a signed release APK, names the attached file `StandBy.apk`, and publishes it to the GitHub Release for a `v*` tag. It can also be run manually for an existing tag from the Actions tab.

## Development APKs from dev

`main` remains stable. Feature branches are merged into `dev`. The separate
`.github/workflows/dev-prerelease.yml` runs on every push to `dev`, including a merge.
One push produces one build of its exact head commit; multiple commits pushed together
produce one prerelease for the final commit.

The workflow runs unit tests, Android Lint and debug/release assembly before publishing.
It uses the same four signing secrets listed below to sign the release-variant APK;
“prerelease” is its GitHub publication status, not an Android build type.
The application ID and version remain unchanged.

Each successful run creates a separate prerelease named `dev-<run>-<short-sha>`,
targets the exact triggering commit, attaches `StandBy-dev.apk`, and also saves it as an
Actions artifact named `StandBy-dev-<sha>`. Earlier runs are not cancelled by later pushes. Retrying the same run
updates its own prerelease and asset. The workflow explicitly sets `--prerelease`
and `--latest=false`, so the stable Latest release is preserved. Development tags do not
match the stable workflow's `v*` trigger. No unsigned fallback is published if secrets,
tests, lint or assembly fail.

See [development prereleases](https://github.com/mrbastim/StandBy-Extended/releases)
and [workflow runs](https://github.com/mrbastim/StandBy-Extended/actions?query=branch%3Adev).

## Required repository secrets

In GitHub, open **Settings → Secrets and variables → Actions → New repository secret** and add:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of the `.jks` or `.keystore` signing file.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: signing key alias.
- `ANDROID_KEY_PASSWORD`: signing key password.

On Linux or macOS, encode the keystore without line breaks:

```sh
base64 -w 0 release-key.jks
```

On macOS, use `base64 < release-key.jks | tr -d '\n'` instead. Copy the output into the `ANDROID_KEYSTORE_BASE64` secret. Keep the original keystore and passwords somewhere safe; never commit them to the repository.

To publish the existing `v1.0.5` tag after configuring secrets, run **Actions → Build and publish Android release → Run workflow**, select branch `main`, and enter `v1.0.5`. Future release tags trigger the workflow automatically when pushed.
