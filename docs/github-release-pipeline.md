# GitHub release pipeline

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

The workflow in `.github/workflows/release.yml` builds a signed release APK and attaches it to the GitHub Release for a `v*` tag. It can also be run manually for an existing tag from the Actions tab.

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
