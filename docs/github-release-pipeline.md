# GitHub release pipeline

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

Stable tags `v*` use `release.yml` and publish `StandBy.apk`.
Every push/merge to `dev` uses `dev-prerelease.yml` and publishes
`StandBy-dev.apk` as `dev-<run>-<short-sha>`, with `--prerelease --latest=false`.
One push builds its head commit; earlier runs are not cancelled. Retries update their
own asset. App ID and version are unchanged. Automatic publication is limited to
runs triggered by `mrbastim`; other actors can build but cannot sign/publish.

## Build, test and signing boundaries

1. Build jobs check out source with `persist-credentials: false`, read-only token,
   no signing secrets, and run tests, lint and debug/release assembly.
   They upload `unsigned-release/app-release-unsigned.apk`.
2. `release-smoke.yml` signs that same minified APK with a disposable key and launches
   MainActivity twice on an API 28 emulator. The process must survive and have no fatal
   AndroidRuntime error. This exercises the Compose/Lifecycle reflection path after R8.
   PR CI runs the same check. Neither job receives the production key.
3. `publish-apk.yml` runs on a fresh runner with environment `release-signing`.
   It does not check out or execute repository build code, restore build caches or run
   the APK. Android SDK apksigner signs the downloaded artifact, verifies its signature,
   and deletes the temporary keystore. Only the publication step receives a write token.
   The signed artifact is named `StandBy-signed-<sha>`.

Both stable and dev APKs use the same release key. Local debug installs normally use
another certificate and cannot be updated by these APKs directly.
Release publication fails if any build/smoke/signing step fails; no unsigned fallback.

## Required owner settings (not applied by committing YAML)

The current GitHub App cannot administer branch protection or environments (HTTP 403).
The following settings must be applied by the repository owner. A workflow's environment
name alone does **not** create deployment restrictions or protect existing repository secrets.

1. Open **Settings → Rules → Rulesets → Import a ruleset** and import
   [.github/dev-branch-ruleset.json](../.github/dev-branch-ruleset.json).
   Keep it **Active** and target only `dev`. It restricts updates, deletion and force
   pushes to repository administrators. For this personal repository, keep admin access
   with the owner only; remove unneeded collaborator write/admin access.
   The admin bypass preserves automated owner pushes/merges without self-approval.
2. Create **Settings → Environments → release-signing**. Under **Deployment branches
   and tags**, select **Selected branches and tags**, allow branch `dev`, branch `main`
   (for manual stable publication), and tags `v*`. Do not choose unrestricted access.
   Keep reviews optional if every owner push should publish automatically; requiring
   approval will deliberately pause each signing job.
3. Put these four secrets in that environment:
   `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
   `ANDROID_KEY_PASSWORD`. Reuse the existing key and values.
   Then remove their repository-level copies. Keeping them at repository scope allows
   a modified workflow on another branch to request them and defeats isolation.
4. Review changes to all workflows and Gradle files before allowing code into `dev`.
   The owner/actor guard is defense in depth, not a replacement for branch protection.

Keep the original keystore/passwords outside Git. Base64-encode it with
`base64 -w 0 release-key.jks` on Linux, or
`base64 < release-key.jks | tr -d '\n'` on macOS.

These workflow changes take effect on `main` only after merging the PR.
No stable release is created by this change.

[GitHub branch protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches),
[deployment environments](https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments).
