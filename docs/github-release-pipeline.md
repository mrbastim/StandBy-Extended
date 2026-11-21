# GitHub release pipeline

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

Stable tags `v*` use `release.yml` and publish `standby-android-extended.apk`.
Every push/merge to `dev` uses `dev-prerelease.yml`. After successful checks it updates
one prerelease tagged `dev`, with `--prerelease --latest=false`, replacing
`standby-android-extended-dev.apk`. The existing release is edited in place, keeping
its ID and download URL. The lightweight tag moves to the exact tested commit.
The title/notes identify that commit and its CI run; stable Latest is preserved.

Builds run independently. Publication is serialized; it checks the current dev head
before signing and again before publication, skipping superseded commits. Pending
publication jobs may be coalesced by GitHub concurrency; there is only one dev channel.
Retries replace the same asset. App ID/version are unchanged. Publication is limited
to runs triggered by `mrbastim`; other actors can build but cannot sign/publish.

Permanent dev download:
[standby-android-extended-dev.apk](https://github.com/mrbastim/StandBy-Extended/releases/download/dev/standby-android-extended-dev.apk).
After the first successful rolling publication, the four old automatic prereleases
`dev-1-ffae1ad`, `dev-2-f5a5cd5`, `dev-3-03a3f22`, `dev-4-40441d0` are removed
with owner authorization. Their Git tags remain; old APK download URLs stop working.
This cleanup only removes those exact tags' releases if they are marked prerelease.

## Build, test and signing boundaries

1. Build jobs check out source with `persist-credentials: false`, read-only token,
   no signing secrets, and run tests, lint and debug/release assembly.
   They upload `unsigned-release/app-release-unsigned.apk`.
2. `release-smoke.yml` signs that same minified APK with a disposable key and launches
   MainActivity twice on an API 28 emulator. The process must survive and have no fatal
   AndroidRuntime error. This exercises the Compose/Lifecycle reflection path after R8.
   PR CI runs the same check. Neither job receives the production key.
3. The signing job in `publish-apk.yml` has `contents: read` and uses the
   `release-signing` environment. It signs and verifies the APK using apksigner,
   removes the temporary keystore and uploads `standby-android-extended-signed-<sha>`.
4. A separate job downloads the signed APK and publishes it with `contents: write`.
   The job does not use the signing environment or check out the repository.
   The moving `dev` tag must stay mutable. Apply stable tag protection to `v*` only.

Both stable and dev APKs use the same release key. Local debug installs normally use
another certificate and cannot be updated by these APKs directly.
Release publication fails if any build/smoke/signing step fails; no unsigned fallback.

## Required owner settings (not applied by committing YAML)

The current GitHub App cannot administer branch protection or environments (HTTP 403).
The following settings must be applied by the repository owner. A workflow's environment
name alone does **not** create deployment restrictions or protect existing repository secrets.

1. Open **Settings → Rules → Rulesets → Import a ruleset** and import
   [.github/dev-branch-ruleset.json](../.github/dev-branch-ruleset.json).
   Keep it **Active** and target only `dev`. Only the GitHub user `mrbastim`
   (ID `78547482`) can bypass restrictions on creating, updating, deleting or
   force-pushing this branch. The bypass applies to direct pushes and PR merges.
2. Create **Settings → Environments → release-signing**. Under **Deployment branches
   and tags**, select **Selected branches and tags**, allow branch `dev`, branch `main`
   (for manual stable publication), and tags `v*`. Before permitting stable refs,
   apply the same owner-only branch rule to `main` and a tag ruleset restricting
   creation, updates and deletion of `v*` tags to administrators. Until those are
   protected, allow **only `dev`** in the environment. Do not choose unrestricted access.
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
