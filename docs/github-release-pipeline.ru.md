# Публикация сборок через GitHub

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

Workflow `.github/workflows/release.yml` собирает подписанный release APK, называет файл `StandBy.apk` и прикрепляет его к GitHub Release для тега `v*`. Его также можно вручную запустить для существующего тега на вкладке Actions.

## APK из ветки dev

`main` остаётся стабильной. Feature-ветки объединяются в `dev`. Отдельный workflow
`.github/workflows/dev-prerelease.yml` запускается на каждый push в `dev`, включая merge.
Один push создаёт одну сборку точного головного коммита; несколько коммитов, отправленных
одним push, дают один prerelease для последнего коммита.

До публикации выполняются unit-тесты, Android Lint и сборки debug/release.
Для подписи release-варианта APK используются те же четыре секрета, которые описаны ниже;
«prerelease» — статус публикации GitHub, а не тип сборки Android.
Идентификатор и версия приложения не меняются.

Каждый успешный запуск создаёт отдельный prerelease `dev-<run>-<short-sha>`,
привязывает его к точному исходному коммиту, прикрепляет `StandBy-dev.apk` и сохраняет
его как артефакт Actions `StandBy-dev-<sha>`. Новые push не отменяют предыдущие запуски. Повтор того же
запуска обновляет его собственный prerelease и APK. Явно заданы `--prerelease`
и `--latest=false`, поэтому стабильный Latest-релиз сохраняется. Теги разработки не
совпадают с `v*` из стабильного workflow. При ошибке секретов, тестов, Lint или сборки
неподписанный запасной APK не публикуется.

Ссылки: [prerelease-сборки](https://github.com/mrbastim/StandBy-Extended/releases),
[запуски workflow](https://github.com/mrbastim/StandBy-Extended/actions?query=branch%3Adev).

## Необходимые секреты репозитория

В GitHub откройте **Settings → Secrets and variables → Actions → New repository secret** и добавьте:

- `ANDROID_KEYSTORE_BASE64`: файл подписи `.jks` или `.keystore`, закодированный в base64.
- `ANDROID_KEYSTORE_PASSWORD`: пароль хранилища ключей.
- `ANDROID_KEY_ALIAS`: псевдоним ключа подписи.
- `ANDROID_KEY_PASSWORD`: пароль ключа подписи.

В Linux или macOS закодируйте хранилище ключей без переносов строк:

```sh
base64 -w 0 release-key.jks
```

В macOS вместо этого используйте `base64 < release-key.jks | tr -d '\n'`. Скопируйте результат в секрет `ANDROID_KEYSTORE_BASE64`. Храните исходное хранилище ключей и пароли в безопасном месте; не добавляйте их в репозиторий.

Чтобы опубликовать существующий тег `v1.0.5` после настройки секретов, откройте **Actions → Build and publish Android release → Run workflow**, выберите ветку `main` и укажите `v1.0.5`. Последующие release-теги автоматически запускают workflow при отправке в репозиторий.
