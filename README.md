# Pogoda

Android-приложение прогноза погоды с дружелюбными к детям подсказками по одежде,
почасовым и недельным прогнозом и виджетом на главный экран.

## Возможности

- Текущая погода, почасовой прогноз и прогноз на 7 дней
- Понятный совет по погоде («возьми зонт», «надень куртку» и т. п.)
- Виджет 5×2 на главный экран (режим «по часам» или «по дням»)
- Два источника данных:
  - **Open-Meteo** — используется всегда, без ключа;
  - **Yandex Weather** (опционально) — при наличии собственного API-ключа,
    с автоматической деградацией на Open-Meteo при ошибке.
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

### Ключ Yandex Weather (опционально)

- В debug-сборке: добавьте в `local.properties` строку
  `YANDEX_DEV_KEY=ваш_ключ` (файл в `.gitignore`).
- В готовом приложении: введите ключ в настройках.

Без ключа приложение работает на Open-Meteo.

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
