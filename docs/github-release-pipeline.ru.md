# Публикация сборок через GitHub

[English](github-release-pipeline.md) | [Русский](github-release-pipeline.ru.md)

Теги `v*` используют `release.yml` и публикуют `StandBy.apk`.
Каждый push/merge в `dev` запускает `dev-prerelease.yml`: `StandBy-dev.apk`
публикуется как `dev-<run>-<short-sha>` с `--prerelease --latest=false`.
Один push собирает головной коммит; предыдущие запуски не отменяются. Повтор запуска
обновляет его APK. Идентификатор и версия приложения не меняются.
Подпись и публикация разрешены только для запусков от `mrbastim`; остальные могут
выполнять сборку без подписи и публикации.

## Границы сборки, проверки и подписи

1. Job сборки использует checkout с `persist-credentials: false`, токен только для
   чтения и не получает секреты подписи. Выполняются тесты, Lint, debug/release.
   Результат: артефакт `unsigned-release/app-release-unsigned.apk`.
2. `release-smoke.yml` подписывает тот же minified APK временным тестовым ключом
   и дважды запускает MainActivity на эмуляторе API 28. Процесс должен продолжать
   работу без фатальных ошибок AndroidRuntime. Это проверяет мост Compose/Lifecycle
   после R8. Тот же сценарий выполняется в PR CI. Релизный ключ сюда не передаётся.
3. `publish-apk.yml` работает на отдельном runner с environment `release-signing`.
   Здесь нет checkout, Gradle, восстановления кешей сборки, запуска кода проекта
   или APK. SDK apksigner подписывает скачанный артефакт и проверяет подпись;
   временный keystore удаляется. Токен с правом записи передаётся только шагу
   публикации. Подписанный артефакт: `StandBy-signed-<sha>`.

Release и dev используют один релизный ключ. Локальный debug обычно имеет другой
сертификат и напрямую не обновляется этим APK.
При ошибке сборки, запуска на эмуляторе или подписи публикации нет; запасной
неподписанный APK не выпускается.

## Обязательные настройки владельца (коммит YAML их не применяет)

Текущее GitHub-подключение не имеет прав администрирования веток и environments
(HTTP 403). Следующие настройки применяет владелец репозитория. Одно имя environment
в workflow **не** задаёт ограничения развёртывания и не защищает repository secrets.

1. Откройте **Settings → Rules → Rulesets → Import a ruleset** и импортируйте
   [.github/dev-branch-ruleset.json](../.github/dev-branch-ruleset.json).
   Оставьте **Active**, цель — только `dev`. Обновление, удаление и force push
   ограничены администраторами репозитория. В этом личном репозитории оставьте
   административный доступ только владельцу; уберите лишний write/admin у участников.
   Admin bypass сохраняет автоматические push/merge владельца без самоодобрения.
2. Создайте **Settings → Environments → release-signing**. В **Deployment branches
   and tags** выберите **Selected branches and tags**: ветки `dev`, `main`
   (для ручной стабильной публикации), теги `v*`. До разрешения стабильных ссылок
   примените такое же ограничение администратором к `main` и создайте tag ruleset,
   ограничивающий создание, изменение и удаление `v*` администраторами. Пока они
   не защищены, разрешите в environment **только `dev`**. Не разрешайте все ветки.
   Для автоматической публикации каждого push владельца не требуйте ручного
   одобрения; если включить его, каждая job подписи будет ждать решения.
3. Добавьте в этот environment четыре секрета:
   `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
   `ANDROID_KEY_PASSWORD`. Используйте существующий ключ и значения.
   Затем удалите их копии из repository secrets. Пока они доступны всему
   репозиторию, изменённый workflow другой ветки может запросить их в обход изоляции.
4. Проверяйте изменения всех workflow и Gradle перед включением в `dev`.
   Проверка владельца запуска — дополнительная мера, а не замена защиты ветки.

Храните исходный keystore и пароли вне Git. Кодирование base64 в Linux:
`base64 -w 0 release-key.jks`; в macOS:
`base64 < release-key.jks | tr -d '\n'`.

В `main` изменения workflow попадут только после слияния PR.
Это изменение не создаёт стабильный релиз.

[Защита веток GitHub](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches),
[deployment environments](https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments).
