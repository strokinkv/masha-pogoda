# Weather App (Погода) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Нативное Android-приложение «Погода» с прогнозом на 7 дней и виджетом 4×2, в стиле Яндекс.Погоды, на двух бесплатных источниках.

**Architecture:** MVVM (ViewModel + StateFlow), слой данных Repository с единой моделью. **Open-Meteo — базовый источник** (без ключа, закрывает все 7 дней + «сейчас» + почасовой). **Яндекс.Погода — опциональное улучшение**: если пользователь ввёл ключ в настройках, «сейчас»/почасовой/дни 1–2 берутся от Яндекса, а дни 3–7 склеиваются из Open-Meteo **по дате**. WorkManager раз в час обновляет кэш (JSON на диске) и виджет.

**Tech Stack:** Kotlin, XML + ViewBinding, Retrofit 2 + OkHttp, kotlinx.serialization, WorkManager, FusedLocationProviderClient, AppWidgetProvider + RemoteViews, ручной DI.

---

## Corrections to source TZ (эмпирически проверено 2026-06-25)

Эти правки получены реальными запросами к API с боевым ключом и интроспекцией GraphQL-схемы. Они **отменяют** соответствующие места `weather_app_tz.md`, который был собран из нескольких черновиков и содержит противоречия.

| # | Что в исходном ТЗ | Проверенный факт | Влияние |
|---|---|---|---|
| C1 | Яндекс REST `GET v2/forecast` (§15.1) | REST `v2/forecast` → **HTTP 403 forbidden**. Работает только **GraphQL `POST /graphql/query`** | Слой Яндекс — только GraphQL |
| C2 | Бесплатный ключ **не отдаёт влажность**; влажность дней 1–2 берётся из Open-Meteo (§3.1, §15.1) | Ключ **отдаёт `humidity` (NON_NULL Int) и `pressure`** в `now`, `Daypart`, `ForecastHour` | **Удаляется** вся логика «влажность из Open-Meteo для дней 1–2». Open-Meteo нужен только для дней 3–7 |
| C3 | Поле осадков `precProb` (§15.x, GraphQL-блок) | Такого поля нет. Правильно: `precProbability` (Float, **nullable**), плюс `prec`, `precType`, `precStrength` | DTO и запрос используют `precProbability` |
| C4 | `icon` запрашивается без аргументов | `icon` требует обязательный аргумент: `icon(format: SVG)` (enum `IconFormat`) | В запросе указывать формат; точный enum уточнить при реализации |
| C5 | Дни 3–7: «Росгидромет» (§4.2, §10) / «ДАНИО-пресс `lb1.hmn.ru`» (§15.2 второй) / Open-Meteo (§3.2, §15.2 первый) | Тройное противоречие. **Выбран Open-Meteo**: без ключа и регистрации; «Росгидромет» не имеет спецификации интеграции; ДАНИО требует `cid`, которого нет | Дни 3–7 = Open-Meteo. **Решение можно отклонить — см. ниже** |
| C6 | `wind_dir` строкой `nw` (REST) | GraphQL: `windDirection` — enum (`NORTH_WEST`…`CALM`), `windAngle` — Int (градусы), `windSpeed` — Float | Маппинг enum, а не строки |
| C7 | Лимит «~30/сутки» как комфортный | Бёрст-запросы упёрлись в лимит после ~9 вызовов за сессию. Лимит жёсткий | Тесты — только на сохранённых фикстурах, не на живом API |

> **Решение, требующее подтверждения (C5):** источник дней 3–7 — **Open-Meteo**. Если у вас есть `cid` для ДАНИО-пресс или конкретный endpoint Росгидромета, скажите — заменю слой `ForecastSecondaryApi` без изменения остального плана (интерфейс изолирован в Task 6).

---

## Global Constraints

Эти ограничения неявно входят в требования каждой задачи.

- **Целевая аудитория:** приложение для **ребёнка — девочки 7 лет** (отсюда имя `masha.pogoda` и иконка с девочкой). Следствия для всего UI: мультяшный дружелюбный стиль, крупные читаемые цифры и иконки, простые русские слова без терминов, яркая «детская» палитра, крупные зоны нажатия. «Взрослые» настройки (ключ API, источник локации) выносим в отдельный раздел, не загромождая главный экран. Никаких сторонних ссылок/рекламы/сбора данных.
- **Платформа:** minSdk = 31 (Android 12), targetSdk = последний стабильный. ABI: arm64-v8a, x86_64. Только портретная ориентация. Локализация — только русский.
- **Язык/UI:** Kotlin; XML Layouts + ViewBinding (не Compose).
- **Архитектура:** MVVM, ViewModel + StateFlow, ручной DI (без Hilt в MVP).
- **Сеть:** Retrofit 2 + OkHttp. JSON — kotlinx.serialization.
- **Хранение:** настройки — SharedPreferences; кэш погоды — JSON-файл на диске (без Room).
- **Единицы (фиксированы):** °C, ветер м/с, время 24ч.
- **Ключ Яндекс — пользовательский и опциональный.** Вводится в **настройках приложения** и хранится в `SharedPreferences`. Если ключ **не задан** → Яндекс не вызывается, **все 7 дней + почасовой + «сейчас» берутся из Open-Meteo**. Если задан → дни 1–2/«сейчас»/почасовой от Яндекса, дни 3–7 от Open-Meteo.
- **Ключ из `.env`** (`3ccbfe2a-5f7a-49b8-90d0-574bad737d8c`) — **только для разработки/тестирования** маппинга Яндекса. Кладётся в `local.properties`→`BuildConfig.YANDEX_DEV_KEY` как дефолт для debug-сборки; расходовать экономно (лимит ~30/сут, нужен лишь при изменении логики обращения к Яндекс API). В проде ключ всегда из настроек пользователя.
- **Единая модель данных:** обе ветки (Яндекс и Open-Meteo) заполняют один и тот же набор полей — пересечение возможностей обоих API (см. раздел «Единый набор полей» ниже). UI не знает источник.
- **Читаемость UI:** тексты и шрифты — высокий контраст, без «тонких» начертаний для значимых данных; **никакой личной информации пользователя** на экранах (ни email, ни аккаунт, ни координаты в открытом виде). `User-Agent` для Nominatim — обезличенный (`WeatherApp/1.0`), без личного email.
- **Яндекс API:** `POST https://api.weather.yandex.ru/graphql/query`, заголовок `X-Yandex-Weather-Key`, тело `{"query": "..."}`. Лимит жёсткий (~30/сут) → **обновление раз в 60 мин (24/сут)**, тесты на фикстурах.
- **Open-Meteo:** `GET https://api.open-meteo.com/v1/forecast`, без ключа. `wind_speed_unit=ms`, `timezone=auto`, `forecast_days=7`.
- **Nominatim:** `GET https://nominatim.openstreetmap.org/search`, обязателен `User-Agent`, лимит 1 req/sec, кэш координат в SharedPreferences.
- **Слияние:** дни 1–2 (Яндекс) и 3–7 (Open-Meteo) склеиваются **по дате (ISO `time`), а не по индексу** — Open-Meteo отдаёт 7 дней от сегодня, и при сдвиге часового пояса/полуночи наивный срез `[2..6]` промахнётся.
- **Производительность:** холодный старт < 2с (с кэшем), RAM < 80 МБ, APK < 10 МБ.
- **Тесты:** JVM unit-тесты (JUnit) только на сохранённых JSON-фикстурах. UI/виджет/локация/WorkManager проверяются вручную/инструментально, без фейковых failing-тестов.

---

## Единый набор полей (пересечение Open-Meteo и Яндекса)

Чтобы день, пришедший из любого источника, рендерился одинаково, используем **только поля, которые есть в обоих API**. Источник для UI прозрачен.

### CurrentWeather («Сейчас»)
| Поле | Open-Meteo (`current=`) | Яндекс (`now`) |
|---|---|---|
| `temperature` | `temperature_2m` | `temperature` |
| `feelsLike` | `apparent_temperature` | `feelsLike` |
| `humidity` | `relative_humidity_2m` | `humidity` |
| `windSpeed` | `wind_speed_10m` | `windSpeed` |
| `windDirection` | `wind_direction_10m` (°→строка) | `windDirection` (enum→строка) |
| `pressure` | `surface_pressure` | `pressure` |
| `code` | `weather_code`→WMO | `condition`→enum |

### DailyWeather (день прогноза)
| Поле | Open-Meteo (`daily=`) | Яндекс (`parts.day`) |
|---|---|---|
| `date` | `time[i]` | `days[i].time` |
| `tempMin` / `tempMax` | `temperature_2m_min/max` | `minTemperature/maxTemperature` |
| `feelsLike` | `apparent_temperature_max` | `feelsLike` |
| `humidity` | `relative_humidity_2m_max` | `humidity` |
| `windSpeed` | `wind_speed_10m_max` | `windSpeed` |
| `windDirection` | `wind_direction_10m_dominant` | `windDirection` |
| `precipProb` | `precipitation_probability_max` | `precProbability` (nullable→0) |
| `code` | `weather_code`→WMO | `condition`→enum |
| `sunrise` / `sunset` | `sunrise/sunset[i]` | `sunriseTime/sunsetTime` |

### HourlyWeather (почасовой, сегодня)
| Поле | Open-Meteo (`hourly=`) | Яндекс (`days[0].hours`) |
|---|---|---|
| `time` | `time[i]` (фильтр на сегодня) | `time` |
| `temperature` | `temperature_2m` | `temperature` |
| `code` | `weather_code` | `condition` |
| `precipProb` | `precipitation_probability` | `precProbability` |

> **Исключено из общей модели:** `pressure` на уровне дня (у Open-Meteo нет дневного агрегата давления) — давление показываем только в «Сейчас». Поля только-Яндекса (`precType`, `windAngle`, UV, и т.п.) в MVP не используются.

---

## Иконки погоды — собственный мультяшный набор

Иконки Яндекса берём **только как ориентир по наглядности** (что именно изображать для каждого состояния) и **перерисовываем в собственном мультяшном стиле** — оригинальный арт, без проприетарных ассетов (юридически чисто, можно публиковать). Набор един для всех источников: и для Яндекса, и для Open-Meteo рисуется одна и та же иконка.

**Стиль (для целевой аудитории — девочка 7 лет):**
- Дружелюбные мультяшные формы: толстый скруглённый контур, мягкие тени, яркая «детская» палитра.
- Солнце/луна — с лёгким «лицом» (по желанию), капли/снежинки крупные и читаемые.
- День/ночь — разный фон/цвет светила; согласовано с DayNight-темой приложения.
- Формат — **SVG в `assets/weather/`** (рендер через androidsvg), масштабируется без потерь и в виджете (через Bitmap).

**Модель:** каждое наблюдение несёт `iconCode: String` — имя нашего ассета. Выводится **из `WeatherCode` (+ `isDay`)** одинаково для обоих источников через `weatherCodeToIcon(code, isDay)`. Никакой зависимости от кодов/URL Яндекса в рантайме.

### Таблица `WeatherCode (+isDay)` → имя нашего ассета
| WeatherCode | День | Ночь |
|---|---|---|
| CLEAR | `clear_day` | `clear_night` |
| PARTLY_CLOUDY | `cloudy_day` | `cloudy_night` |
| CLOUDY | `cloudy_day` | `cloudy_night` |
| OVERCAST | `overcast` | `overcast` |
| FOG | `fog_day` | `fog_night` |
| RAIN_LIGHT | `rain_light_day` | `rain_light_night` |
| RAIN | `rain` | `rain` |
| RAIN_HEAVY | `rain` | `rain` |
| SNOW_LIGHT | `snow_light_day` | `snow_light_night` |
| SNOW | `snow` | `snow` |
| SNOWFALL | `snow` | `snow` |
| THUNDERSTORM | `thunderstorm` | `thunderstorm` |
| MIXED | `sleet` | `sleet` |

**Полный набор ассетов (15 SVG):** `clear_day clear_night cloudy_day cloudy_night overcast fog_day fog_night rain_light_day rain_light_night rain snow_light_day snow_light_night snow thunderstorm sleet`.

> **`isDay`:** Open-Meteo → поле `is_day` (0/1) в `current`/`hourly`; для дневных карточек прогноза `isDay=true`. Яндекс → день/ночь определяем по суффиксу его кода `icon(format: CODE)` (`_n` → ночь), но **рисуем свой ассет** (Яндекс-арт не используется).
> **Кэш:** ассеты лежат в APK (`assets/`) — доступны офлайн и без ключа всегда; в рантайме LRU-кэш распарсенных SVG.

---

## Milestones

1. **M1 — Каркас + иконка приложения + свой набор иконок погоды** (Task 1, 1A, 1B)
2. **M2 — Доменные модели + мапперы** (Tasks 2–3) — TDD
3. **M3 — Сетевой слой обоих API** (Tasks 4–6) — TDD на фикстурах
4. **M4 — Слияние + кэш + Repository** (Tasks 7–9) — TDD
5. **M5 — Локация + геокодер** (Task 10)
6. **M6 — Главный экран** (Tasks 11–13) — ручная проверка
7. **M7 — Настройки** (Task 14)
8. **M8 — Фоновая синхронизация** (Task 15)
9. **M9 — Виджет 4×2** (Task 16) — ручная проверка
10. **M10 — Оффлайн/ошибки/полировка** (Task 17)

---

## File Structure

```
app/src/main/java/masha/pogoda/
├── data/
│   ├── api/
│   │   ├── YandexWeatherApi.kt          # Retrofit: POST graphql/query
│   │   ├── YandexQuery.kt               # GraphQL-строка + DTO ответа
│   │   ├── OpenMeteoApi.kt              # Retrofit: GET v1/forecast + DTO
│   │   └── NominatimApi.kt             # Retrofit: GET search + DTO
│   ├── mapper/
│   │   ├── WeatherCodeMapper.kt         # WMO→WeatherCode, Yandex Condition→WeatherCode
│   │   ├── WindDirectionMapper.kt       # enum/градусы→строка (С/СВ/…)
│   │   ├── YandexMapper.kt              # YandexDto→Current/Hourly/Daily
│   │   └── OpenMeteoMapper.kt           # OpenMeteoDto→Daily
│   ├── cache/WeatherCacheManager.kt     # save/load JSON, TTL 24ч
│   ├── location/LocationProvider.kt     # FusedLocation
│   ├── prefs/AppPrefs.kt                # SharedPreferences-обёртка
│   └── repository/WeatherRepository.kt  # merge + кэш + источники
├── domain/model/
│   ├── WeatherCode.kt                   # enum состояний
│   ├── CurrentWeather.kt
│   ├── HourlyWeather.kt
│   ├── DailyWeather.kt
│   └── WeatherForecast.kt
├── ui/
│   ├── main/ MainActivity.kt, MainViewModel.kt, адаптеры hourly/daily
│   ├── icon/ WeatherIconLoader.kt        # SVG-иконки из assets/weather
│   └── settings/ SettingsActivity.kt
├── (assets) app/src/main/assets/weather/*.svg  # собственный мультяшный набор (15 SVG)
├── widget/ WeatherWidget.kt
├── work/ WeatherSyncWorker.kt
└── di/ ServiceLocator.kt                # ручной DI

app/src/test/java/masha/pogoda/                 # JVM unit-тесты
app/src/test/resources/fixtures/         # сохранённые JSON-ответы
```

---

# TASKS

### Task 1: Каркас проекта Android + git

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `app/src/main/AndroidManifest.xml`, `.gitignore`, `local.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `app/proguard-rules.pro`, `app/src/main/java/masha/pogoda/App.kt`
- Create: `app/src/main/res/values/strings.xml`, `themes.xml`

**Interfaces:**
- Produces: рабочий gradle-проект, `BuildConfig.YANDEX_SMARTHOME_KEY`, пакет приложения, `compileDebug` собирается.

- [x] **Step 1: git init и .gitignore**

```bash
cd /c/Users/strokin/projects/pogoda
git init
printf '%s\n' '*.iml' '.gradle/' 'build/' 'local.properties' '.idea/' '.env' > .gitignore
```

- [x] **Step 2: Gradle-скрипты**

`app/build.gradle.kts` (ключевые места):

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization") version "2.0.0"
}
android {
    namespace = "masha.pogoda"
    compileSdk = 35
    defaultConfig {
        applicationId = "masha.pogoda"
        minSdk = 31
        targetSdk = 35
        versionCode = 1; versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        // Только дев-дефолт для debug-тестирования маппинга Яндекса; в проде ключ из настроек
        buildConfigField("String", "YANDEX_DEV_KEY",
            "\"${(project.findProperty("YANDEX_DEV_KEY") ?: "")}\"")
    }
    buildFeatures { viewBinding = true; buildConfig = true }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.caverock:androidsvg-aar:1.4")   // рендер SVG-иконок погоды
    testImplementation("junit:junit:4.13.2")
}
```

- [x] **Step 3: local.properties с ключом**

```properties
YANDEX_DEV_KEY=3ccbfe2a-5f7a-49b8-90d0-574bad737d8c
```

> Этот ключ — только для разработки. Боевой ключ пользователь вводит в настройках (Task 14), он хранится в `SharedPreferences`.

- [x] **Step 4: AndroidManifest с разрешениями + Application**

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<!-- в <application> указать android:name=".App" (Task 9 наполнит ServiceLocator/WorkManager) -->
```

Создать пустой пока `App : Application()` (наполняется в Task 9) и прописать `android:name=".App"`.

- [x] **Step 5: Gradle wrapper** (обязательно — все задачи зовут `./gradlew`). Если в системе есть gradle: `gradle wrapper --gradle-version 8.7`. Иначе вручную создать `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip`), `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`. Проверить: `./gradlew --version`.

- [x] **Step 6: R8 keep-rules** `app/proguard-rules.pro` (иначе release-сборка молча ломает парсинг JSON):

```proguard
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keep,includedescriptorclasses class masha.pogoda.**$$serializer { *; }
-keepclassmembers class masha.pogoda.** { *** Companion; }
-keepclasseswithmembers class masha.pogoda.** { kotlinx.serialization.KSerializer serializer(...); }
# Retrofit/OkHttp
-keep,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
# androidsvg
-keep class com.caverock.androidsvg.** { *; }
```

- [x] **Step 7: Сборка** `./gradlew :app:assembleDebug` — Expected: BUILD SUCCESSFUL.

- [x] **Step 8: Commit**

```bash
git add -A && git commit -m "chore: android project scaffold + wrapper + r8 rules"
```

---

### Task 1A: Иконка приложения (adaptive + legacy)

**Источник:** `Gemini_Generated_Image_w0dz4w0dz4w0dz4w.png` (256×256, девочка с зонтом; пользователь называл его «for_icon.png» — если положите файл с этим именем, используйте его). Картинка уже квадратная со скруглением и светло-голубым/жёлтым фоном.

**Files:**
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.png` (+ density-варианты), `res/values/ic_launcher_background.xml`
- Create: `art/app_icon_source.png` (копия источника в репозитории)
- Modify: `AndroidManifest.xml` (`android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`)

**Verification (ручная — графика):**

- [x] **Step 1: Скопировать источник в проект**

```bash
cd /c/Users/strokin/projects/pogoda
mkdir -p art
cp "Gemini_Generated_Image_w0dz4w0dz4w0dz4w.png" art/app_icon_source.png
```

- [x] **Step 2: Сгенерировать density-варианты foreground** (ImageMagick). Adaptive foreground — 108dp canvas с safe-zone 66dp по центру, поэтому изображение масштабируем до ~72% и центрируем на прозрачном холсте, чтобы маска не срезала лицо:

```bash
# размеры foreground (px) по плотностям: mdpi=108, hdpi=162, xhdpi=216, xxhdpi=324, xxxhdpi=432
for d in "mdpi:108" "hdpi:162" "xhdpi:216" "xxhdpi:324" "xxxhdpi:432"; do
  name="${d%%:*}"; size="${d##*:}"; inner=$(( size * 72 / 100 ))
  mkdir -p "app/src/main/res/mipmap-${name}"
  magick art/app_icon_source.png -resize ${inner}x${inner} \
    -background none -gravity center -extent ${size}x${size} \
    "app/src/main/res/mipmap-${name}/ic_launcher_foreground.png"
  # legacy квадратная и круглая иконки (полный кадр)
  magick art/app_icon_source.png -resize ${size}x${size} \
    "app/src/main/res/mipmap-${name}/ic_launcher.png"
  magick art/app_icon_source.png -resize ${size}x${size} \
    \( +clone -alpha extract -fill white -colorize 0 -draw "circle $((size/2)),$((size/2)) $((size/2)),0" \) \
    -alpha off -compose CopyOpacity -composite \
    "app/src/main/res/mipmap-${name}/ic_launcher_round.png"
done
```

- [x] **Step 3: Фон adaptive-иконки** `res/values/ic_launcher_background.xml` — сэмплировать преобладающий цвет источника (тёплый светлый, ≈`#FBE7B6`):

```xml
<resources><color name="ic_launcher_background">#FBE7B6</color></resources>
```

- [x] **Step 4: Adaptive XML** `res/mipmap-anydpi-v26/ic_launcher.xml` (и `ic_launcher_round.xml` идентично):

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
```

- [x] **Step 5: Манифест** — в `<application>` указать `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`, `android:label="Погода"`.

- [ ] **Step 6: Manual verify:** `./gradlew :app:assembleDebug`, установить, проверить иконку на лаунчере (квадратный, круглый и «теардроп» маски не срезают лицо девочки).

- [x] **Step 7: Commit** `git commit -am "feat: app launcher icon (adaptive + legacy)"`

---

### Task 1B: Собственный мультяшный набор иконок погоды (15 SVG)

**Files:**
- Create: `app/src/main/assets/weather/*.svg` (15 файлов из раздела «Иконки погоды»)
- Create: `art/icons/` (рабочие исходники), `docs/icons_style.md` (стайл-гайд)

**Interfaces:**
- Produces: офлайн-набор оригинальных SVG-иконок. Имена файлов = значения `weatherCodeToIcon(...)`. Потребляется `WeatherIconLoader` (Task 12).

> Иконки рисуем сами в мультяшном стиле (ориентир — что изображает Яндекс для каждого состояния). Без day/night-папок: день/ночь различаются отдельными файлами (`*_day`/`*_night`), цвет светила/фона согласован с DayNight-темой. Размер вьюпорта 64×64, толщина контура ~3, палитра «детская».

- [x] **Step 1: Стайл-гайд** `docs/icons_style.md` — зафиксировать: viewBox `0 0 64 64`, обводка `#2B2B2B` 3px, солнце `#FFC83D`, луна `#FBE7B6`, облако день `#BFD7EA`/ночь `#7A8AA0`, дождь `#4FA3E3`, снег `#FFFFFF`+контур, молния `#FFD23F`. Скруглённые углы, дружелюбные формы.

- [x] **Step 2: Базовая иконка `clear_day.svg`** (эталон стиля):

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <g stroke="#2B2B2B" stroke-width="3" stroke-linecap="round">
    <circle cx="32" cy="32" r="13" fill="#FFC83D"/>
    <g stroke="#FFB000">
      <line x1="32" y1="6"  x2="32" y2="14"/>
      <line x1="32" y1="50" x2="32" y2="58"/>
      <line x1="6"  y1="32" x2="14" y2="32"/>
      <line x1="50" y1="32" x2="58" y2="32"/>
      <line x1="13" y1="13" x2="19" y2="19"/>
      <line x1="45" y1="45" x2="51" y2="51"/>
      <line x1="51" y1="13" x2="45" y2="19"/>
      <line x1="19" y1="45" x2="13" y2="51"/>
    </g>
    <circle cx="27" cy="30" r="1.6" fill="#2B2B2B"/>
    <circle cx="37" cy="30" r="1.6" fill="#2B2B2B"/>
    <path d="M27 36 q5 4 10 0" fill="none"/>
  </g>
</svg>
```

- [x] **Step 3: Нарисовать остальные 14** в том же стиле (`clear_night` — луна+звёзды; `cloudy_day/night` — солнце/луна за облаком; `overcast` — плотное облако; `fog_day/night` — облако+полосы тумана; `rain_light_*` — облако+2–3 капли; `rain` — облако+ливень; `snow_light_*` — облако+2–3 снежинки; `snow` — облако+снегопад; `thunderstorm` — туча+молния; `sleet` — капля+снежинка). Один общий «детский» язык форм.

- [x] **Step 4: Проверка** — открыть каждый SVG в браузере/предпросмотре; все 15 рендерятся, имена точно совпадают со списком из раздела «Иконки погоды».

- [x] **Step 5: Commit** `git commit -am "assets: original cartoon weather icon set (15 svg)"`

---

### Task 2: Доменные модели + WeatherCode

**Files:**
- Create: `domain/model/WeatherCode.kt`, `CurrentWeather.kt`, `HourlyWeather.kt`, `DailyWeather.kt`, `WeatherForecast.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class WeatherCode { CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST, FOG,
      RAIN_LIGHT, RAIN, RAIN_HEAVY, SNOW_LIGHT, SNOW, SNOWFALL, THUNDERSTORM, MIXED }

  @Serializable data class CurrentWeather(
      val temperature: Int, val feelsLike: Int, val humidity: Int,
      val windSpeed: Double, val windDirection: String, val pressure: Int,
      val code: WeatherCode, val iconCode: String, val description: String, val updatedAt: Long)

  @Serializable data class HourlyWeather(
      val time: String, val temperature: Int, val code: WeatherCode,
      val iconCode: String, val precipProb: Int)

  @Serializable data class DailyWeather(
      val date: String, val tempMin: Int, val tempMax: Int, val feelsLike: Int,
      val humidity: Int, val windSpeed: Double, val windDirection: String,
      val precipProb: Int, val code: WeatherCode, val iconCode: String,
      val sunrise: String?, val sunset: String?)

  @Serializable data class WeatherForecast(
      val city: String, val current: CurrentWeather,
      val hourly: List<HourlyWeather>, val daily: List<DailyWeather>,
      val cachedAt: Long)
  ```

- [x] **Step 1:** Создать пять файлов с указанными `data class`/`enum`, все помечены `@Serializable` (kotlinx).
- [x] **Step 2:** Run `./gradlew :app:compileDebugKotlin` — Expected: BUILD SUCCESSFUL.
- [x] **Step 3: Commit** `git commit -am "feat: domain models"`

---

### Task 3: Мапперы кодов и направления ветра (TDD)

**Files:**
- Create: `data/mapper/WeatherCodeMapper.kt`, `WindDirectionMapper.kt`
- Test: `app/src/test/java/.../WeatherCodeMapperTest.kt`, `WindDirectionMapperTest.kt`

**Interfaces:**
- Consumes: `WeatherCode` (Task 2).
- Produces:
  ```kotlin
  fun mapWmoCode(code: Int): WeatherCode
  fun mapYandexCondition(c: String): WeatherCode   // принимает "CLEAR","PARTLY_CLOUDY",... и lower-case варианты
  fun yandexWindToRu(direction: String): String    // "NORTH_WEST"->"СЗ","CALM"->"штиль"
  fun degreesToRu(deg: Int): String                 // 0->"С"
  fun weatherCodeToIcon(code: WeatherCode, isDay: Boolean): String  // -> "clear_day" и т.п. (имя нашего ассета)
  fun weatherToAdvice(tempC: Int, code: WeatherCode, precipProb: Int, windMs: Double): String  // детский совет
  ```

- [x] **Step 1: Failing-тест WMO**

```kotlin
class WeatherCodeMapperTest {
    @Test fun wmo() {
        assertEquals(WeatherCode.CLEAR, mapWmoCode(0))
        assertEquals(WeatherCode.PARTLY_CLOUDY, mapWmoCode(2))
        assertEquals(WeatherCode.OVERCAST, mapWmoCode(3))
        assertEquals(WeatherCode.FOG, mapWmoCode(45))
        assertEquals(WeatherCode.RAIN, mapWmoCode(63))
        assertEquals(WeatherCode.RAIN_HEAVY, mapWmoCode(65))
        assertEquals(WeatherCode.SNOW, mapWmoCode(75))
        assertEquals(WeatherCode.THUNDERSTORM, mapWmoCode(95))
    }
    @Test fun yandexCond() {
        assertEquals(WeatherCode.CLEAR, mapYandexCondition("CLEAR"))
        assertEquals(WeatherCode.OVERCAST, mapYandexCondition("OVERCAST"))
        assertEquals(WeatherCode.RAIN, mapYandexCondition("RAIN"))
        assertEquals(WeatherCode.THUNDERSTORM, mapYandexCondition("THUNDERSTORM"))
    }
}
```

- [x] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests *WeatherCodeMapperTest` — Expected: FAIL (unresolved reference).
- [x] **Step 3: Реализация** `WeatherCodeMapper.kt`

```kotlin
fun mapWmoCode(code: Int): WeatherCode = when (code) {
    0, 1 -> WeatherCode.CLEAR
    2 -> WeatherCode.PARTLY_CLOUDY
    3 -> WeatherCode.OVERCAST
    45, 48 -> WeatherCode.FOG
    51, 53, 80 -> WeatherCode.RAIN_LIGHT
    55, 61, 63, 81 -> WeatherCode.RAIN
    65, 82 -> WeatherCode.RAIN_HEAVY
    71, 73, 77 -> WeatherCode.SNOW_LIGHT
    75, 85, 86 -> WeatherCode.SNOW
    95, 96, 99 -> WeatherCode.THUNDERSTORM
    else -> WeatherCode.CLOUDY
}

fun mapYandexCondition(c: String): WeatherCode = when (c.uppercase()) {
    "CLEAR" -> WeatherCode.CLEAR
    "PARTLY_CLOUDY" -> WeatherCode.PARTLY_CLOUDY
    "CLOUDY" -> WeatherCode.CLOUDY
    "OVERCAST" -> WeatherCode.OVERCAST
    "DRIZZLE", "LIGHT_RAIN" -> WeatherCode.RAIN_LIGHT
    "RAIN", "MODERATE_RAIN", "SHOWERS" -> WeatherCode.RAIN
    "HEAVY_RAIN" -> WeatherCode.RAIN_HEAVY
    "LIGHT_SNOW" -> WeatherCode.SNOW_LIGHT
    "SNOW", "SNOWFALL", "SNOW_SHOWERS" -> WeatherCode.SNOW
    "HAIL", "THUNDERSTORM" -> WeatherCode.THUNDERSTORM
    "FOG" -> WeatherCode.FOG
    else -> WeatherCode.CLOUDY
}
```

- [x] **Step 4: Failing-тест ветра**

```kotlin
class WindDirectionMapperTest {
    @Test fun yandex() {
        assertEquals("СЗ", yandexWindToRu("NORTH_WEST"))
        assertEquals("штиль", yandexWindToRu("CALM"))
    }
    @Test fun degrees() {
        assertEquals("С", degreesToRu(0)); assertEquals("В", degreesToRu(90))
        assertEquals("Ю", degreesToRu(180)); assertEquals("З", degreesToRu(270))
    }
}
```

- [x] **Step 5: Реализация** `WindDirectionMapper.kt`

```kotlin
fun yandexWindToRu(direction: String): String = when (direction.uppercase()) {
    "NORTH" -> "С"; "NORTH_EAST" -> "СВ"; "EAST" -> "В"; "SOUTH_EAST" -> "ЮВ"
    "SOUTH" -> "Ю"; "SOUTH_WEST" -> "ЮЗ"; "WEST" -> "З"; "NORTH_WEST" -> "СЗ"
    "CALM" -> "штиль"; else -> "—"
}
fun degreesToRu(deg: Int): String =
    listOf("С","СВ","В","ЮВ","Ю","ЮЗ","З","СЗ")[(((deg % 360) + 22) / 45) % 8]
```

- [x] **Step 6: Failing-тест иконок**

```kotlin
class WeatherIconMapTest {
    @Test fun dayNight() {
        assertEquals("clear_day", weatherCodeToIcon(WeatherCode.CLEAR, true))
        assertEquals("clear_night", weatherCodeToIcon(WeatherCode.CLEAR, false))
        assertEquals("overcast", weatherCodeToIcon(WeatherCode.OVERCAST, true))   // без day/night
        assertEquals("thunderstorm", weatherCodeToIcon(WeatherCode.THUNDERSTORM, true))
        assertEquals("fog_night", weatherCodeToIcon(WeatherCode.FOG, false))
    }
}
```

- [x] **Step 7: Реализация** `weatherCodeToIcon` по таблице из раздела «Иконки погоды»:

```kotlin
fun weatherCodeToIcon(code: WeatherCode, isDay: Boolean): String {
    val s = if (isDay) "day" else "night"
    return when (code) {
        WeatherCode.CLEAR -> "clear_$s"
        WeatherCode.PARTLY_CLOUDY, WeatherCode.CLOUDY -> "cloudy_$s"
        WeatherCode.OVERCAST -> "overcast"
        WeatherCode.FOG -> "fog_$s"
        WeatherCode.RAIN_LIGHT -> "rain_light_$s"
        WeatherCode.RAIN, WeatherCode.RAIN_HEAVY -> "rain"
        WeatherCode.SNOW_LIGHT -> "snow_light_$s"
        WeatherCode.SNOW, WeatherCode.SNOWFALL -> "snow"
        WeatherCode.THUNDERSTORM -> "thunderstorm"
        WeatherCode.MIXED -> "sleet"
    }
}
```

- [x] **Step 8:** Run `./gradlew :app:testDebugUnitTest --tests *Mapper* --tests *IconMap*` — Expected: PASS.

- [x] **Step 9: Failing-тест совета (детская подсказка)**

```kotlin
class WeatherAdviceTest {
    @Test fun advice() {
        assertTrue(weatherToAdvice(20, WeatherCode.RAIN, 80, 3.0).contains("зонт"))
        assertTrue(weatherToAdvice(-10, WeatherCode.SNOW, 30, 2.0).contains("тёпл"))
        assertTrue(weatherToAdvice(25, WeatherCode.CLEAR, 0, 1.0).contains("гул")) // «можно гулять»
    }
}
```

- [x] **Step 10: Реализация** `weatherToAdvice` — простые правила детским языком, приоритет: осадки → холод → жара → норма:

```kotlin
fun weatherToAdvice(tempC: Int, code: WeatherCode, precipProb: Int, windMs: Double): String = when {
    code in setOf(WeatherCode.RAIN, WeatherCode.RAIN_HEAVY, WeatherCode.THUNDERSTORM) || precipProb >= 60 ->
        "Возьми зонт ☔ — может пойти дождь"
    code in setOf(WeatherCode.SNOW, WeatherCode.SNOWFALL, WeatherCode.SNOW_LIGHT) ->
        "Снег! Надевай тёплую куртку и шапку ❄"
    tempC <= 0  -> "Очень холодно — тёплая куртка и шапка 🧣"
    tempC <= 10 -> "Прохладно — надень курточку 🧥"
    tempC >= 25 -> "Жарко ☀ — лёгкая одежда и вода"
    else        -> "Хорошая погода — можно гулять! 🙂"
}
```

- [x] **Step 11:** Run `--tests *AdviceTest` — Expected: PASS.
- [x] **Step 12: Commit** `git commit -am "feat: weather mappers + child advice (TDD)"`

---

### Task 4: Open-Meteo API + DTO + маппер (TDD на фикстуре)

**Files:**
- Create: `data/api/OpenMeteoApi.kt`, `data/mapper/OpenMeteoMapper.kt`
- Test: `OpenMeteoMapperTest.kt`; fixture `app/src/test/resources/fixtures/open_meteo.json`

> **Open-Meteo — самодостаточный источник.** Когда ключ Яндекса не задан, ОДИН его ответ закрывает «сейчас» + почасовой + 7 дней. Поэтому запрашиваем `current`, `hourly` и `daily` сразу.

**Interfaces:**
- Consumes: `mapWmoCode`, `degreesToRu`, `weatherCodeToIcon`, `CurrentWeather`, `HourlyWeather`, `DailyWeather`.
- Produces:
  ```kotlin
  interface OpenMeteoApi { @GET("v1/forecast") suspend fun getForecast(...): OpenMeteoResponse }
  fun OpenMeteoResponse.toDaily(): List<DailyWeather>     // 7 элементов, date = daily.time[i]
  fun OpenMeteoResponse.toCurrent(): CurrentWeather       // из блока current
  fun OpenMeteoResponse.toHourlyToday(): List<HourlyWeather>  // hours, отфильтрованные на сегодня
  ```

- [ ] **Step 1: Сохранить фикстуру.** Реальный ответ Open-Meteo c тремя блоками в `open_meteo.json` (`is_day` нужен для выбора дневной/ночной иконки):
  - `current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_direction_10m,surface_pressure,weather_code,is_day`
  - `hourly=temperature_2m,weather_code,precipitation_probability,is_day`
  - `daily=temperature_2m_max,temperature_2m_min,apparent_temperature_max,relative_humidity_2m_max,wind_speed_10m_max,wind_direction_10m_dominant,precipitation_probability_max,weather_code,sunrise,sunset`

- [x] **Step 2: DTO** `OpenMeteoApi.kt` — `@Serializable` классы `OpenMeteoResponse(current, hourly, daily)` (параллельные массивы) + Retrofit-интерфейс с `@Query`: `latitude`, `longitude`, `current=...`, `hourly=...`, `daily=...`, `wind_speed_unit=ms`, `timezone=auto`, `forecast_days=7`.

- [x] **Step 3: Failing-тест маппера**

```kotlin
class OpenMeteoMapperTest {
    private fun load() = Json{ignoreUnknownKeys=true}.decodeFromString<OpenMeteoResponse>(
        javaClass.getResource("/fixtures/open_meteo.json")!!.readText())
    @Test fun mapsSevenDays() {
        val d = load().toDaily()
        assertEquals(7, d.size)
        assertEquals(load().daily.time[0], d[0].date)   // выравнивание по дате
        assertTrue(d[0].tempMax >= d[0].tempMin)
    }
    @Test fun mapsCurrent() {
        val c = load().toCurrent()
        assertTrue(c.humidity in 0..100); assertTrue(c.pressure > 0)
    }
    @Test fun mapsHourlyToday() { assertTrue(load().toHourlyToday().isNotEmpty()) }
}
```

- [x] **Step 4:** Run `--tests *OpenMeteoMapperTest` — Expected: FAIL.
- [x] **Step 5: Реализация** трёх мапперов: `toDaily()` — из параллельных массивов, `code=mapWmoCode(...)`, `windDirection=degreesToRu(...)`, `date=daily.time[i]`, `iconCode=weatherCodeToIcon(code, isDay=true)`; `toCurrent()` — из `current`, `iconCode=weatherCodeToIcon(code, current.is_day==1)`; `toHourlyToday()` — `hourly` по дате `daily.time[0]`, `iconCode=weatherCodeToIcon(code, hour.is_day==1)`.
- [x] **Step 6:** Run тест — Expected: PASS.
- [ ] **Step 7: Commit** `git commit -am "feat: open-meteo standalone source (current+hourly+daily, TDD)"`

---

### Task 5: Яндекс GraphQL API + DTO + маппер (TDD на фикстуре) — опциональный источник

> Ключ берётся из `AppPrefs.yandexKey` (введён пользователем) и передаётся в заголовке per-request, **не** из BuildConfig в проде. Дев-ключ из `BuildConfig.YANDEX_DEV_KEY` используется только для записи фикстуры в Step 2 — один запрос, экономно.

**Files:**
- Create: `data/api/YandexWeatherApi.kt`, `data/api/YandexQuery.kt`, `data/mapper/YandexMapper.kt`
- Test: `YandexMapperTest.kt`; fixture `app/src/test/resources/fixtures/yandex.json`

**Interfaces:**
- Consumes: `mapYandexCondition`, `yandexWindToRu`, `weatherCodeToIcon`, модели Task 2.
- Produces:
  ```kotlin
  interface YandexWeatherApi {
      @POST("graphql/query") suspend fun query(@Body body: GraphQlRequest): YandexResponse }
  data class GraphQlRequest(val query: String)
  fun YandexResponse.toCurrent(): CurrentWeather
  fun YandexResponse.toHourly(): List<HourlyWeather>
  fun YandexResponse.toDailyHead(): List<DailyWeather>   // дни 1–2
  ```

**Verified GraphQL fields (интроспекция 2026-06-25):**
- `now`: `temperature feelsLike humidity pressure windSpeed windAngle windDirection condition icon(format: CODE)`
- `forecast.days(limit:2)`: `time sunriseTime sunsetTime`
- `days.parts.day` (тип `Daypart`): `minTemperature maxTemperature humidity windSpeed windDirection condition precProbability precType icon(format: CODE)`
- `days.hours` (тип `ForecastHour`): `time temperature condition precProbability icon(format: CODE)`
- ⚠️ НЕ существует `precProb`; влажность/давление ЕСТЬ; `icon` требует `format`. Запрашиваем `icon(format: CODE)` → строка вида `bkn_n` **только чтобы взять суффикс `_d/_n`** (день/ночь); сам арт — наш (`weatherCodeToIcon`). `IconFormat` = CODE/EMOJI/PNG_24..128/SVG.

- [ ] **Step 1: GraphQL-строка** `YandexQuery.kt` — константа с запросом из проверенных полей выше (плейсхолдеры lat/lon через `String.format`).
- [ ] **Step 2: Сохранить фикстуру** `yandex.json` — реальный ответ на этот запрос (структура `{"data":{"weatherByPoint":{"now":{...},"forecast":{"days":[...]}}}}`).
- [ ] **Step 3: DTO** `@Serializable` под фикстуру (с `ignoreUnknownKeys`).
- [ ] **Step 4: Failing-тест**

```kotlin
class YandexMapperTest {
    private fun load() = Json{ignoreUnknownKeys=true}.decodeFromString<YandexResponse>(
        javaClass.getResource("/fixtures/yandex.json")!!.readText())
    @Test fun current() {
        val c = load().toCurrent()
        assertTrue(c.humidity in 0..100)      // влажность ЕСТЬ (C2)
        assertTrue(c.windDirection.isNotEmpty())
    }
    @Test fun head() { assertEquals(2, load().toDailyHead().size) }
}
```

- [ ] **Step 5:** Run — Expected: FAIL.
- [ ] **Step 6: Реализация** мапперов: `condition`→`mapYandexCondition`, `windDirection`→`yandexWindToRu`, `precProbability` (nullable)→`?.toInt() ?: 0`, `date = days[i].time` (дата из ISO). **Иконка:** `isDay = !icon.endsWith("_n")` (день/ночь из суффикса кода Яндекса), затем `iconCode = weatherCodeToIcon(code, isDay)` — рисуем свой ассет, Яндекс-арт не используем.
- [ ] **Step 7:** Run — Expected: PASS.
- [ ] **Step 8: Commit** `git commit -am "feat: yandex graphql api+mapper (TDD)"`

---

### Task 6: Networking-обвязка (ServiceLocator, OkHttp, Retrofit)

**Files:**
- Create: `di/ServiceLocator.kt`, `data/api/NominatimApi.kt`

**Interfaces:**
- Produces: `ServiceLocator.yandexApi`, `.openMeteoApi`, `.nominatimApi`, общий `OkHttpClient`.

- [ ] **Step 1:** `ServiceLocator` — три Retrofit-инстанса (разные baseUrl), общий OkHttp; интерцептор для `api.weather.yandex.ru` берёт ключ **динамически из `AppPrefs.yandexKey`** (`X-Yandex-Weather-Key`) — так введённый в настройках ключ применяется без пересоздания клиента; интерцептор `User-Agent: WeatherApp/1.0` (обезличенный, без личного email) для `nominatim.openstreetmap.org`. Конвертер — `kotlinx-serialization`.
- [ ] **Step 2:** `NominatimApi` — `@GET("search") suspend fun search(@Query("q") q, @Query("format")="json", @Query("limit")=1, @Query("accept-language")="ru"): List<NominatimResult>`.
- [ ] **Step 3:** Run `./gradlew :app:compileDebugKotlin` — Expected: SUCCESS.
- [ ] **Step 4: Commit** `git commit -am "feat: networking service locator"`

---

### Task 7: Слияние источников по дате (TDD)

**Files:**
- Create: `data/repository/ForecastMerger.kt`
- Test: `ForecastMergerTest.kt`

**Interfaces:**
- Consumes: `CurrentWeather`, `HourlyWeather`, `DailyWeather`, `WeatherForecast`.
- Produces: `fun merge(city: String, current: CurrentWeather, hourly: List<HourlyWeather>, yandexDays: List<DailyWeather>, openMeteoDays: List<DailyWeather>): WeatherForecast`

- [ ] **Step 1: Failing-тест выравнивания по дате**

```kotlin
class ForecastMergerTest {
    @Test fun joinsByDate_sevenUniqueDays() {
        val y = listOf(day("2026-06-25"), day("2026-06-26"))            // Яндекс дни 1-2
        val om = (25..31).map { day("2026-06-%02d".format(it)) }        // Open-Meteo 7 дней
        val f = merge("Москва", cur(), emptyList(), y, om)
        assertEquals(7, f.daily.size)
        assertEquals(f.daily.map{it.date}, f.daily.map{it.date}.distinct()) // нет дублей дат
        assertEquals("2026-06-25", f.daily[0].date)                      // дни 1-2 от Яндекса
    }
}
```

- [ ] **Step 2:** Run — Expected: FAIL.
- [ ] **Step 3: Реализация:** взять Яндекс-дни как есть; из `openMeteoDays` добавить только те, чья `date` отсутствует среди Яндекс-дат; отсортировать по дате; обрезать до 7. Влажность дней 1–2 — из Яндекса (C2), Open-Meteo для влажности не используется.
- [ ] **Step 4:** Run — Expected: PASS.
- [ ] **Step 5: Commit** `git commit -am "feat: date-based source merge (TDD)"`

---

### Task 8: Кэш на диске + TTL (TDD)

**Files:**
- Create: `data/cache/WeatherCacheManager.kt`
- Test: `WeatherCacheManagerTest.kt` (Robolectric или передавать `File` в конструктор — выбрать File-based для чистого JVM-теста)

**Interfaces:**
- Produces: `class WeatherCacheManager(dir: File)` с `fun save(f: WeatherForecast)`, `fun load(): WeatherForecast?`, `fun isStale(f: WeatherForecast, now: Long): Boolean` (TTL 24ч).

- [ ] **Step 1: Failing-тест round-trip + stale**

```kotlin
class WeatherCacheManagerTest {
    @Test fun roundTripAndTtl() {
        val mgr = WeatherCacheManager(createTempDir())
        val f = sampleForecast(cachedAt = 1000L)
        mgr.save(f)
        assertEquals(f.city, mgr.load()!!.city)
        assertFalse(mgr.isStale(f, now = 1000L + 23*3600_000L))
        assertTrue(mgr.isStale(f, now = 1000L + 25*3600_000L))
    }
}
```

- [ ] **Step 2:** Run — Expected: FAIL.
- [ ] **Step 3: Реализация:** сериализация `WeatherForecast` через `Json` в `dir/forecast.json`; `isStale = now - f.cachedAt > 24*3600_000`.
- [ ] **Step 4:** Run — Expected: PASS.
- [ ] **Step 5: Commit** `git commit -am "feat: disk cache with 24h ttl (TDD)"`

---

### Task 9: WeatherRepository (оркестрация + fallback)

**Files:**
- Create: `data/repository/WeatherRepository.kt`, `data/prefs/AppPrefs.kt`

**Interfaces:**
- Consumes: оба Api, `ForecastMerger.merge`, `WeatherCacheManager`, мапперы.
- Produces:
  ```kotlin
  sealed class WeatherResult {
      data class Success(val forecast: WeatherForecast, val fromCache: Boolean, val stale: Boolean): WeatherResult()
      data class Error(val message: String, val cached: WeatherForecast?): WeatherResult()
  }
  class WeatherRepository(...) { suspend fun refresh(lat: Double, lon: Double, city: String): WeatherResult }
  ```

- [ ] **Step 1:** `AppPrefs` — обёртка SharedPreferences: lat, lon, city, locationMode (GPS/MANUAL), **`yandexKey: String?`** (с дефолтом `BuildConfig.YANDEX_DEV_KEY` только в debug; в release — пусто пока пользователь не введёт). **Дефолт города — Москва** (`55.7558, 37.6176`): задаётся при первом запуске, чтобы экран никогда не был пустым, даже если разрешение на геолокацию отклонено или нет Google Play Services.
- [ ] **Step 2:** `refresh()` ветвится по наличию ключа:
  - **Ключ пуст** → один запрос к Open-Meteo: `current = om.toCurrent()`, `hourly = om.toHourlyToday()`, `daily = om.toDaily()` (все 7 дней). Яндекс не вызывается — экономим лимит.
  - **Ключ задан** → параллельно (`coroutineScope`+`async`) Яндекс (с ключом из prefs) и Open-Meteo; `current/hourly/дни1–2` от Яндекса, `merge(...)` склеивает дни 3–7 от Open-Meteo по дате.
  - В обоих случаях `cache.save` и `Success(fromCache=false)`.
  - Fallback (§15.5): Яндекс упал, но ключ был → деградировать на полный Open-Meteo (а не на кэш), при его падении — кэш; Open-Meteo упал → дни 3–7 из кэша; оба/всё упало + кэш свежий → `Success(fromCache=true)`; кэш устарел/нет → `Error`.
- [ ] **Step 3:** Run `./gradlew :app:testDebugUnitTest` — Expected: все прежние тесты PASS (компиляция целостна).
- [ ] **Step 4: Commit** `git commit -am "feat: weather repository orchestration"`

---

### Task 10: Локация (FusedLocation) + геокодер

**Files:**
- Create: `data/location/LocationProvider.kt`

**Interfaces:**
- Produces: `suspend fun LocationProvider.current(): Pair<Double,Double>?`; `suspend fun geocode(city: String): GeoPoint?` (через `NominatimApi`, кэш в `AppPrefs`).

**Verification (ручная — нет failing-теста):**
- [ ] **Step 1:** Реализовать `LocationProvider` на `FusedLocationProviderClient.getCurrentLocation(PRIORITY_BALANCED_POWER_ACCURACY, …)`, обёрнутый в `suspendCancellableCoroutine`. Проверка разрешений — caller.
- [ ] **Step 2:** `geocode()` — вызвать Nominatim, взять первый результат, сохранить lat/lon/city в `AppPrefs`, при сетевой ошибке вернуть null.
- [ ] **Step 3: Manual verify:** на устройстве/эмуляторе с GPS — лог координат; ручной ввод «Казань» → координаты ≈55.79,49.12. Зафиксировать в PR-заметке.
- [ ] **Step 4: Commit** `git commit -am "feat: location provider + nominatim geocoder"`

---

### Task 11: MainViewModel + состояние экрана

**Files:**
- Create: `ui/main/MainViewModel.kt`, `ui/main/MainUiState.kt`

**Interfaces:**
- Consumes: `WeatherRepository`, `LocationProvider`, `AppPrefs`.
- Produces: `MainViewModel.uiState: StateFlow<MainUiState>` (`Loading`/`Content(forecast, banner)`/`Empty(retry)`); `fun refresh()`.

- [ ] **Step 1:** `MainUiState` sealed-класс. ViewModel определяет координаты (GPS или сохранённый город из prefs), зовёт `repository.refresh`, мапит `WeatherResult` в `MainUiState` (баннер «Нет подключения — данные на <время>» при `stale`/`fromCache`).
- [ ] **Step 2:** Run `compileDebugKotlin` — SUCCESS.
- [ ] **Step 3: Commit** `git commit -am "feat: main viewmodel + ui state"`

---

### Task 12: Главный экран — разметка и блоки

**Files:**
- Create: `res/layout/activity_main.xml`, `item_hourly.xml`, `item_daily.xml`; `ui/main/HourlyAdapter.kt`, `DailyAdapter.kt`, `MainActivity.kt`
- Create: `ui/icon/WeatherIconLoader.kt` (рендер SVG-иконок из бандла Task 1B)

**Verification (ручная):**
- [ ] **Step 1: `WeatherIconLoader`** — грузит `assets/weather/<iconCode>.svg` через androidsvg в `ImageView`. День/ночь уже зашиты в имя ассета (`*_day`/`*_night`), отдельных theme-папок нет. LRU-кэш `Map<String, SVG>` по `iconCode`. Если файла нет — фолбэк `weatherCodeToIcon(code, isDay=true)`. Методы: `fun load(iv: ImageView, iconCode: String)` и (для виджета) `fun renderBitmap(iconCode: String, sizePx: Int): Bitmap`.
- [ ] **Step 2:** Разметка: `SwipeRefreshLayout` → `NestedScrollView` с блоками «Сейчас» (темп крупно, «ощущается как», строка влажность/ветер/осадки), **карточка-совет** (текст из `weatherToAdvice(...)`, крупно, с эмодзи — главный «детский» элемент), горизонтальный `RecyclerView` почасового, вертикальный список 7 дней. Каждой иконке — `contentDescription` (описание состояния словами, не только цвет/картинка).
- [ ] **Step 3:** Адаптеры `HourlyAdapter`/`DailyAdapter` (ListAdapter+DiffUtil); строка дня = день недели+дата, иконка (через `WeatherIconLoader.load(iv, item.iconCode)`), мин/макс, осадки.
- [ ] **Step 4:** `MainActivity` — ViewBinding, подписка на `uiState`, `requestPermissions(ACCESS_FINE/COARSE_LOCATION)` при старте, pull-to-refresh → `vm.refresh()`. Большая иконка «Сейчас» = `WeatherIconLoader.load(iv, current.iconCode)`.
- [ ] **Step 5: Manual verify:** запустить **без ключа Яндекса** — мультяшные иконки (солнце/туча/дождь) показываются для всех источников и **офлайн** (airplane mode); pull-to-refresh обновляет; toast при ошибке. Проверить, что ночные состояния показывают `*_night`-вариант.
- [ ] **Step 6: Commit** `git commit -am "feat: main screen ui + adapters + svg weather icons"`

---

### Task 13: Тема, градиентный фон по времени суток, анимации

**Files:**
- Modify: `res/values/themes.xml`, `values-night/themes.xml`; Create: `res/drawable/bg_gradient_*.xml`
- Modify: `MainActivity.kt`

**Verification (ручная):**
- [ ] **Step 1:** `Theme.Material3.DayNight`. **Детская мультяшная палитра** (фиксированная, без Dynamic Color, чтобы стиль был узнаваемым): яркие дружелюбные цвета, скруглённые карточки, крупные цифры. Светлая: акцент `#4FA3E3`, фон `#FFF7E8`; тёмная: акцент `#8AB4F8`, фон `#1B2030`. Шрифты крупнее обычного (температура «сейчас» 80sp).
- [ ] **Step 2:** Градиент фона по времени суток (утро/день/вечер/ночь), мягкий и «сказочный». Fade-in блоков 200мс; лёгкая «прыгающая» анимация большой иконки (по желанию).
- [ ] **Step 3: Manual verify:** светлая/тёмная тема по системе; фон меняется по времени; всё читаемо с расстояния, цифры крупные.
- [ ] **Step 4: Commit** `git commit -am "feat: theming, gradient background, animations"`

---

### Task 14: Экран настроек

**Files:**
- Create: `ui/settings/SettingsActivity.kt`, `res/layout/activity_settings.xml`
- Modify: `AndroidManifest.xml` (регистрация Activity)

**Verification (ручная):**
- [ ] **Step 0: Родительский замок.** Настройки — для взрослого. На входе в `SettingsActivity` показать простой барьер (диалог «Сколько будет 7 + 5?»), чтобы ребёнок 7 лет случайно не стёр ключ/город. Неверный ответ — вернуть на главный экран.
- [ ] **Step 1:** Поля настроек:
  - **Ключ Яндекс.Погоды** — текстовое поле (`TextInputEditText`) + подсказка «Оставьте пустым — прогноз только от Open-Meteo». Сохраняется в `AppPrefs.yandexKey`. Опционально кнопка «Проверить ключ» (1 тестовый запрос).
  - Переключатель «GPS / Ручной ввод».
  - Поле города (активно при ручном) + кнопка поиска → `geocode()`.
  - Блок «О приложении»: версия, источники (Open-Meteo CC BY 4.0; Яндекс.Погода — если ключ задан; Nominatim). **Без личной информации пользователя.**
- [ ] **Step 2:** Сохранение в `AppPrefs`; при изменении ключа/города/режима — `refresh()`. Очистка ключа → следующий refresh идёт только в Open-Meteo.
- [ ] **Step 3: Manual verify:** (а) без ключа — 7 дней грузятся, Яндекс не вызывается (проверить по логам OkHttp); (б) ввод валидного ключа → «сейчас»/почасовой от Яндекса; (в) смена города и режима GPS↔ручной.
- [ ] **Step 4: Commit** `git commit -am "feat: settings with optional yandex key field"`

---

### Task 15: Фоновая синхронизация WorkManager

**Files:**
- Create: `work/WeatherSyncWorker.kt`; Modify: `MainActivity`/`Application` (планирование)

**Verification (ручная):**
- [ ] **Step 1:** `WeatherSyncWorker(CoroutineWorker)` → `repository.refresh(prefs)`; при успехе обновить виджет; `Result.retry()` при ошибке.
- [ ] **Step 2:** `PeriodicWorkRequest` 60 мин, `Constraints` (NetworkType.CONNECTED), `BackoffPolicy.EXPONENTIAL`, `ExistingPeriodicWorkPolicy.KEEP`. Планировать при старте приложения.
- [ ] **Step 3: Manual verify:** `adb shell cmd jobscheduler run -f <pkg> <id>` или WorkManager-инспектор — кэш обновляется.
- [ ] **Step 4: Commit** `git commit -am "feat: hourly background sync worker"`

---

### Task 16: Виджет рабочего стола 4×2

**Files:**
- Create: `widget/WeatherWidget.kt`, `res/layout/widget_weather_4x2.xml`, `res/xml/weather_widget_info.xml`
- Modify: `AndroidManifest.xml` (receiver + meta-data)

**Verification (ручная):**
- [ ] **Step 1:** `appwidget-provider`: minWidth ≈250dp, minHeight ≈110dp (4×2), `resizeMode="none"`, updatePeriod 0 (обновляем из Worker).
- [ ] **Step 2:** RemoteViews: верх — город, иконка+темп, «ощущается/влажность/ветер»; низ — 3 колонки (сегодня/завтра/послезавтра: день, иконка, мин–макс); footer — время обновления. Тап → `PendingIntent` на `MainActivity`. **Иконки:** RemoteViews не умеет SVG/ImageView — в `WeatherIconLoader` добавить `fun renderBitmap(iconCode: String, sizePx: Int): Bitmap` (androidsvg → Canvas → Bitmap) и ставить через `RemoteViews.setImageViewBitmap`. Тему виджета определять по `Configuration.uiMode`.
- [ ] **Step 3:** `WeatherWidget(AppWidgetProvider).onUpdate` читает кэш и рендерит; статический метод `updateAll(context)` зовётся из Worker.
- [ ] **Step 4: Manual verify:** добавить виджет на рабочий стол, данные видны, тап открывает приложение, тёмная тема корректна.
- [ ] **Step 5: Commit** `git commit -am "feat: 4x2 home screen widget"`

---

### Task 17: Оффлайн, обработка ошибок, полировка

**Files:**
- Modify: `MainActivity`, `MainViewModel`, строки

**Verification (ручная):**
- [ ] **Step 1: Детские тексты ошибок.** Все user-facing строки — в `strings.xml`, дружелюбным детским языком с маскотом, без «взрослых» формулировок. Заглушка вместо технической ошибки: «Ой! Не получилось узнать погоду 🌧 Попробуем ещё разок» + кнопка «Повторить». Технические причины (401/429 и т.п.) — только в лог, ребёнку не показываем. Баннер кэша: «Показываю погоду с <время> 🕒».
- [ ] **Step 2:** Проверить холодный старт с кэшем без сети (<2с), отсутствие краша при потере сети в любой момент.
- [ ] **Step 3: Release-сборка с R8** `./gradlew :app:assembleRelease`; **установить и реально открыть** (проверить, что JSON парсится после обфускации — keep-rules работают), размер APK < 10 МБ.
- [ ] **Step 4: Manual verify (airplane mode):** приложение открывается на кэше/дефолтном городе + дружелюбный баннер; «Повторить» работает после возврата сети.
- [ ] **Step 5: Commit** `git commit -am "feat: offline handling, child-friendly error states, R8 polish"`

---

## Self-Review (покрытие спеки)

| Раздел ТЗ | Задача |
|---|---|
| §3 источники, §10/§15.4 слияние | T4, T5, T7, T9 + **C2/C5 исправлены** |
| §4.1 локация | T10 |
| §4.2 главный экран | T11–T13 |
| §4.3 фон. обновление | T15 |
| §4.4 / §15.5 оффлайн+ошибки | T8, T9, T17 |
| §5 виджет | T16 |
| §6 настройки | T14 |
| §7 UI/тема/градиент | T12, T13 |
| §7.1 иконки погоды (свой мультяшный набор) | T1B (рисуем 15 SVG), T3 (маппинг), T12 (WeatherIconLoader) |
| §8 стек, §9 разрешения, wrapper/R8/Application | T1 |
| §11 структура | File Structure |
| §12 нефункциональные | T17 |
| Детское: совет по одежде | T3 (`weatherToAdvice`), T12 (карточка-совет) |
| Детское: родительский замок настроек | T14 (Step 0) |
| Детское: дружелюбные тексты/ошибки, доступность | T17, T12 |
| Надёжность: дефолтный город (никогда не пусто) | T9 |

## Вне MVP (явно НЕ делаем — несмотря на «детское» обрамление)

Звуки/озвучка, мини-игры, анимированный маскот, несколько городов, пуш-уведомления, радар осадков, прогноз >7 дней, реклама/аналитика/сбор данных, публикация в Google Play, планшет/landscape. Подтверждает §13 ТЗ. **Маскот Маши, реагирующий на погоду** (одет по погоде на главном экране) — заманчиво, но это Phase 2 (заметная стоимость арта), в MVP не входит.

**Открытые вопросы к пользователю** перед стартом:
1. Подтвердить **Open-Meteo** как основной/единственный источник дней 3–7 (C5), либо дать `cid`/endpoint альтернативы.
2. Имя пакета — `masha.pogoda` (подтверждено).
3. Давление в «Сейчас» — из Open-Meteo `surface_pressure` (подтверждено); на уровне дня давление не показываем (нет агрегата).
