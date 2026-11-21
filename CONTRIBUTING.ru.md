# Участие в разработке

[English](CONTRIBUTING.md) | [Русский](CONTRIBUTING.ru.md)

## Локальная настройка

Используйте JDK 17 и Android SDK 35 или новее. Откройте проект в Android Studio либо выполните:

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

В Windows используйте `gradlew.bat`.

## Pull request

- Отделяйте сбор системного состояния от интерфейса Compose.
- Не добавляйте обходные пути для фонового запуска Activity.
- Добавляйте тесты для поведения состояния и сохранения настроек.
- Размещайте пользовательские строки в `values/strings.xml`, `values-ru/strings.xml` и `values-tr/strings.xml`.
- Проверяйте компактные альбомные экраны и увеличенный системный шрифт.
- Не добавляйте в Git ключи подписи, API-ключи, `local.properties` и результаты сборки.
- При изменении документации обновляйте обе версии: английскую `.md` и русскую `.ru.md`. Сохраняйте переключатель языка и ссылки на документы выбранного языка.

## Проверка перед выпуском

1. Обновите `versionCode`, `versionName` и обе версии `CHANGELOG`.
2. Выполните unit-тесты, Lint и сборки debug и release.
3. Проверьте активацию на переднем плане, предпросмотр и DreamService на физическом устройстве.
4. Проверьте обновление приложения и миграцию настроек DataStore.
5. Подпишите release-сборку данными, хранящимися вне репозитория.

Архитектура защиты описана в [amoled-protection.ru.md](docs/amoled-protection.ru.md), план проверок — в [testing.ru.md](docs/testing.ru.md), настройка публикации — в [github-release-pipeline.ru.md](docs/github-release-pipeline.ru.md).
