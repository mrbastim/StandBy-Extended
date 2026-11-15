# Публикация сборок через GitHub

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

Workflow `.github/workflows/release.yml` собирает подписанный release APK и прикрепляет его к GitHub Release для тега `v*`. Его также можно вручную запустить для существующего тега на вкладке Actions.

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
