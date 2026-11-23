# Contributing

[English](CONTRIBUTING.md) | [Русский](CONTRIBUTING.ru.md)

## Local setup

Use JDK 17 and Android SDK 35 or newer. Open the project in Android Studio or run:

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

On Windows, use `gradlew.bat`.

## Pull requests

- Keep system-state collection separate from Compose presentation code.
- Do not introduce background activity-launch workarounds.
- Add tests for state or persistence behavior.
- Put user-visible copy in `values/strings.xml`, `values-ru/strings.xml`, and `values-tr/strings.xml`.
- Check compact landscape layouts and increased system font sizes.
- Never commit signing keys, API keys, `local.properties`, or generated build output.

- When changing documentation, update both the English `.md` and Russian `.ru.md` versions. Keep the language switch and links to documents in the selected language.

## Release checklist

1. Set `standbyVersionName` and increase `standbyVersionCode` in `gradle.properties`. Update both changelogs.
2. Run unit tests, lint, and both debug and release builds.
3. Test foreground activation, preview mode, and DreamService on a physical device.
4. Verify upgrade behavior and DataStore settings migration.
5. Once the release changes are on `main`, run these commands in Bash (Git Bash on Windows). Pushing the tag starts the release workflow.

```bash
git switch main && git pull --ff-only &&
  tag="v$(git show HEAD:gradle.properties | sed -n 's/^standbyVersionName=//p')" &&
  git tag "$tag" && git push origin "refs/tags/$tag"
```

Both README badges and download links follow the latest published stable release.

See [amoled-protection.md](docs/amoled-protection.md) for architecture, [testing.md](docs/testing.md) for verification, and [github-release-pipeline.md](docs/github-release-pipeline.md) for publishing setup.
