# Pogoda

Android-приложение прогноза погоды с дружелюбными к детям подсказками по одежде,
почасовым и недельным прогнозом и виджетом на главный экран.

## Возможности

- Текущая погода, почасовой прогноз и прогноз на 7 дней
- Понятный совет по погоде («возьми зонт», «надень куртку» и т. п.)
- Виджет 5×2 на главный экран (режим «по часам» или «по дням»)
- Источник данных **Open-Meteo** — бесплатный, без ключа и регистрации
- Геолокация (GPS) или ручной выбор города
- Офлайн-кэш (cache-first) и фоновое обновление через WorkManager

## Технологии

Kotlin · MVVM · Coroutines/Flow · Retrofit + kotlinx.serialization · OkHttp ·
WorkManager · ViewBinding · androidsvg.

- `minSdk` 31, `targetSdk` / `compileSdk` 35, Java 17
- AGP 8.6.1, Kotlin 2.0.0, Gradle 8.7

## Сборка

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # юнит-тесты
./gradlew assembleRelease      # release APK (см. подпись ниже)
```

Приложение работает на бесплатном источнике Open-Meteo — ключи и регистрация не нужны.

## CI / Release

- **CI** (`.github/workflows/ci.yml`): на каждый push и pull request гоняет
  юнит-тесты и собирает debug APK (артефакт `app-debug`).
- **Release** (`.github/workflows/release.yml`): на тег `v*` собирает release
  APK и публикует GitHub Release с автогенерируемыми заметками.

### Подпись release-сборки

Подпись включается автоматически, если заданы переменные (env или
`local.properties`): `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. Без них собирается неподписанный release APK.

Для подписи в CI добавьте секреты репозитория:

| Секрет | Назначение |
| --- | --- |
| `KEYSTORE_BASE64` | keystore (`.jks`), закодированный в base64 |
| `KEYSTORE_PASSWORD` | пароль keystore |
| `KEY_ALIAS` | алиас ключа |
| `KEY_PASSWORD` | пароль ключа |

Получить base64 keystore: `base64 -w0 release.jks` (Linux) /
`base64 -i release.jks` (macOS).

## Выпуск версии

```bash
# обновите versionName/versionCode в app/build.gradle.kts, затем:
git tag v1.0.0
git push origin v1.0.0   # запускает workflow Release
```
