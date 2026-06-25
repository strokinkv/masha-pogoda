# Техническое задание: Android-приложение «Погода»

**Версия документа:** 1.0  
**Дата:** 2026-06-25  
**Тип проекта:** Личный проект / MVP  
**Платформа:** Android  

---

## 1. Общее описание

Нативное Android-приложение для просмотра прогноза погоды на 7 дней с виджетом на рабочем столе. Визуальный стиль вдохновлён приложением Яндекс.Погода: чистый минималистичный UI, акцент на типографике и иконках состояний погоды, адаптивная тема (светлая/тёмная по системе).

Приложение использует два бесплатных источника данных:
- **Яндекс.Погода** (API умного дома, бесплатный бессрочно) — текущая погода и прогноз на **день 1 и день 2**
- **Open-Meteo** (бесплатно, без ключа и регистрации, данные ECMWF) — прогноз на **дни 3–7**

Монетизация отсутствует. Уведомления не предусмотрены.

---

## 2. Целевая платформа

| Параметр | Значение |
|---|---|
| Минимальная версия Android | Android 12 (API 31) |
| Целевой SDK | Последний стабильный (на момент разработки) |
| Архитектуры | arm64-v8a, x86_64 |
| Ориентация экрана | Портретная (только) |
| Локализация | Русский язык (единственный) |

---

## 3. Источники данных

### 3.1 Яндекс.Погода — API умного дома (бесплатный)

- **Охват:** день 1 (сегодня) и день 2 (завтра), включая почасовой прогноз на сегодня
- **Стоимость:** бесплатно бессрочно, регистрация через yandex.ru/pogoda/b2b/smarthome
- **Протокол:** REST, JSON
- **Лимит:** ~30 запросов/сутки (≈24 запроса при часовом обновлении — вписывается)
- **Предоставляемые поля:** температура, «ощущается как», ветер (скорость + направление), тип и вероятность осадков, иконка/код состояния
- ⚠️ **Ограничение:** в бесплатном тарифе **влажность не предоставляется**. Для дней 1–2 влажность берётся из Open-Meteo (там она есть), для блока «сейчас» поле заполняется оттуда же

> **Важно при регистрации:** необходим новый Яндекс ID, не связанный с бизнес-кабинетом Погоды. Создать новый аккаунт в режиме инкогнито → перейти по ссылке «Подключиться» на странице smarthome.

### 3.2 Open-Meteo — REST API (бесплатный, без ключа)

- **Охват:** дни 3–7 (среднесрочный прогноз). Дополнительно используется как источник влажности для дней 1–2
- **Стоимость:** полностью бесплатно для некоммерческого использования, без регистрации и ключа
- **Лимит:** до 10 000 запросов/сутки
- **Протокол:** REST, JSON, GET-запрос по координатам
- **Base URL:** `https://api.open-meteo.com/v1/forecast`
- **Модель данных:** ECMWF IFS (европейская модель, 9 км разрешение) — одна из наиболее точных глобальных моделей
- **Предоставляемые поля:** температура мин/макс, «ощущается как», влажность, ветер (скорость + направление), вероятность осадков, код погодного состояния (WMO), восход/закат

### 3.3 Геокодирование — Nominatim (OSM)

- Бесплатно, без ключа, обязателен `User-Agent`
- Endpoint: `https://nominatim.openstreetmap.org/search?q={city}&format=json&limit=1`
- Используется только при ручном вводе города; координаты кэшируются в SharedPreferences

---

## 4. Функциональные требования

### 4.1 Определение местоположения

- При первом запуске запрашивается разрешение `ACCESS_FINE_LOCATION` и `ACCESS_COARSE_LOCATION`
- Если разрешение выдано — автоматически определяется текущее местоположение (GPS + Network)
- Если разрешение отклонено — отображается поле ручного поиска города
- Поддерживается переключение между GPS и ручным вводом в любой момент через настройки
- Хранится только одна локация (множественные города не поддерживаются)

### 4.2 Главный экран

Главный экран состоит из вертикально прокручиваемого списка блоков:

**Блок «Сейчас»**
- Название города и дата/время последнего обновления
- Большая иконка состояния погоды
- Текущая температура (°С, крупный шрифт)
- «Ощущается как X°С»
- Краткое текстовое описание (напр. «Облачно», «Дождь»)
- Строка с параметрами: влажность, ветер (скорость + направление стрелкой), вероятность осадков

**Блок «Сегодня по часам»** *(источник: Яндекс)*
- Горизонтальный скролл с шагом 1–3 часа
- Каждая карточка: время, иконка, температура, значок осадков если > 20%

**Блок «7 дней»**
- 7 строк: день недели + дата, иконка, мин/макс температура, иконка осадков, вероятность осадков
- Строки 1–2: данные Яндекс.Погоды, строки 3–7: данные Росгидромета
- Визуальный разделитель или метка источника не отображается пользователю (склейка прозрачная)

**Поведение:**
- Pull-to-refresh — принудительное обновление данных
- При отсутствии сети отображается баннер «Нет подключения — данные на X время» с кэшированными данными
- При ошибке получения данных — toast-сообщение с описанием ошибки

### 4.3 Обновление данных в фоне

- Периодичность: каждые **60 минут** через `WorkManager` (ConstraintedWork с сетью)
- При обновлении: сначала запрос к Яндекс.API, затем к Росгидромету, результат сливается и сохраняется в локальный кэш
- Виджет обновляется сразу после успешного получения новых данных
- При неудаче — экспоненциальный retry, максимум 3 попытки, после чего используется кэш

### 4.4 Оффлайн-режим

- Последний успешный ответ каждого API сохраняется локально (Room Database или JSON-файл на диске)
- Время жизни кэша: **24 часа** (по истечении отображается предупреждение об устаревших данных)
- Приложение запускается и показывает данные без сети, если кэш актуален

---

## 5. Виджет рабочего стола

### 5.1 Характеристики

| Параметр | Значение |
|---|---|
| Тип | AppWidgetProvider |
| Минимальный размер | 4×2 ячейки |
| Изменяемый размер | Нет (фиксированный) |

### 5.2 Состав виджета

**Верхняя половина — текущая погода:**
- Название города
- Иконка состояния + текущая температура (крупно)
- «Ощущается как», влажность, ветер в одну строку

**Нижняя половина — прогноз на 3 дня:**
- Три колонки (сегодня / завтра / послезавтра)
- Каждая колонка: день недели, иконка, мин–макс °С

**Footer:**
- Время последнего обновления (мелко, справа)

**Поведение:**
- Tap по виджету — открывает приложение
- Обновление: вместе с основным циклом WorkManager (каждый час)
- Тема виджета: следует системной теме (светлая/тёмная)

---

## 6. Настройки

Экран настроек минимален (Intent → SettingsActivity):

| Настройка | Тип | Описание |
|---|---|---|
| Источник местоположения | Переключатель | GPS-автоопределение / Ручной ввод города |
| Город (ручной ввод) | Текстовое поле с поиском | Активно только при выборе ручного ввода |
| О приложении | Статичный блок | Версия, источники данных, ссылки на API |

**Фиксированные единицы измерения (не изменяются пользователем):**
- Температура: °С
- Скорость ветра: м/с
- Формат времени: 24-часовой

---

## 7. UI/UX требования

### 7.1 Общий стиль

- Вдохновение: Яндекс.Погода (минималистичный, чистый, акцент на данных)
- Компоновка: Material Design 3 (Material You)
- Тема: `DayNight` — автоматически переключается по системной теме
- Фон главного экрана: градиент, отражающий время суток (утро/день/вечер/ночь)
- Иконки погоды: единый собственный набор SVG-иконок, покрывающий все состояния обоих источников

### 7.2 Типографика

- Основной шрифт: системный (Roboto / sans-serif)
- Температура «сейчас»: 72sp, bold
- Параметры текущей погоды: 14sp
- Почасовой и недельный прогнозы: 13–14sp

### 7.3 Цветовая схема

- Генерируется на базе Material You (Dynamic Color, API 31+)
- Запасная (fallback) палитра для случаев, когда Dynamic Color недоступен:
  - Светлая: primary `#1A73E8`, background `#F8F9FA`
  - Тёмная: primary `#8AB4F8`, background `#121212`

### 7.4 Анимации

- Смена блоков при загрузке: fade-in, 200 мс
- Pull-to-refresh: стандартный Material индикатор
- Переход на экран настроек: slide-in

---

## 8. Технический стек

Приоритет: **минимальный размер APK и низкое потребление ресурсов.**

| Компонент | Выбор | Обоснование |
|---|---|---|
| Язык | Kotlin | Официальный язык Android |
| UI | **XML Layouts + ViewBinding** | Меньший overhead vs Compose для MVP-масштаба |
| Архитектура | MVVM (ViewModel + LiveData/StateFlow) | Стандарт Android Jetpack |
| Фоновые задачи | WorkManager | Надёжная фоновая синхронизация |
| Сеть | Retrofit 2 + OkHttp | Минимальные зависимости |
| Парсинг JSON | Gson или kotlinx.serialization | Лёгкий |
| Парсинг CSV | Kotlin stdlib (`readLines`) | Без лишних библиотек |
| Локальное хранение | SharedPreferences (настройки) + JSON-файл на диске (кэш погоды) | Проще Room для одного города |
| Геолокация | Google Play Services — FusedLocationProviderClient | Стандарт |
| DI | Hilt (опционально) или ручной DI | Hilt для масштабируемости |
| Виджет | AppWidgetProvider + RemoteViews | Нативный Android API |

**Целевой размер APK:** < 10 МБ (без ProGuard), < 6 МБ (с R8/ProGuard shrinking)

---

## 9. Разрешения Android

```xml
<!-- Геолокация -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Сеть -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Фоновая работа WorkManager -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Разрешения `ACCESS_FINE_LOCATION` и `ACCESS_COARSE_LOCATION` запрашиваются в рантайме при первом запуске.

---

## 10. Логика слияния данных источников

```
Запрос данных:
│
├── Яндекс.API → день 1 (сегодня): текущее + почасовой + дневной
│              → день 2 (завтра): дневной прогноз
│
└── Росгидромет CSV → дни 3, 4, 5, 6, 7: дневные прогнозы
                    → маппинг кода погоды → внутренний WeatherCode enum
                    → маппинг WeatherCode → иконка

Результат: единый объект WeatherForecast {
    current: CurrentWeather         // Яндекс
    hourly: List<HourlyWeather>     // Яндекс, сегодня
    daily: List<DailyWeather>       // 7 элементов: [0-1] Яндекс, [2-6] Росгидромет
}
```

**Обработка несоответствий:**
- Если Яндекс.API недоступен — дни 1–2 заполняются из кэша или помечаются как устаревшие
- Если Росгидромет недоступен — дни 3–7 заполняются из кэша или скрываются с пометкой
- Оба источника недоступны + кэш устарел — экран-заглушка с кнопкой «Повторить»

---

## 11. Структура проекта (рекомендуемая)

```
app/
├── data/
│   ├── api/
│   │   ├── YandexWeatherApi.kt
│   │   └── RoshydrometParser.kt
│   ├── cache/
│   │   └── WeatherCacheManager.kt
│   ├── location/
│   │   └── LocationProvider.kt
│   └── repository/
│       └── WeatherRepository.kt
├── domain/
│   └── model/
│       ├── CurrentWeather.kt
│       ├── HourlyWeather.kt
│       ├── DailyWeather.kt
│       └── WeatherForecast.kt
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   ├── MainViewModel.kt
│   │   └── res/layout/activity_main.xml
│   └── settings/
│       ├── SettingsActivity.kt
│       └── res/layout/activity_settings.xml
├── widget/
│   ├── WeatherWidget.kt
│   └── res/layout/widget_weather_4x2.xml
└── work/
    └── WeatherSyncWorker.kt
```

---

## 12. Нефункциональные требования

| Требование | Критерий |
|---|---|
| Время холодного запуска | < 2 секунды (с кэшем) |
| Потребление RAM | < 80 МБ в активном состоянии |
| Заряд батареи | WorkManager с ограничением по сети, без wake-lock |
| Размер APK | < 10 МБ |
| Крэши | 0 ANR, crashRate < 0.1% на сессию |
| Доступность сети | Корректная работа при потере сети в любой момент |

---

## 13. Не входит в MVP (Out of Scope)

- Множественные города / локации
- Пуш-уведомления о погоде
- Карта осадков (радар)
- Прогноз более 7 дней
- Монетизация, реклама
- Публикация в Google Play (магазин)
- Планшетная/landscape-адаптация
- Виджеты других размеров (2×1, 4×1)

---

## 14. Зависимости и API-ключи

| Зависимость | Источник | Условия |
|---|---|---|
| Яндекс.Погода API (умный дом) | yandex.ru/pogoda/b2b/smarthome | Бесплатно, новый Яндекс ID, некоммерческое |
| Open-Meteo | api.open-meteo.com | Бесплатно, без ключа, некоммерческое, CC BY 4.0 |
| Nominatim (геокодер) | nominatim.openstreetmap.org | Бесплатно, лимит 1 req/sec, User-Agent обязателен |

**Хранение ключей:** API-ключ Яндекс хранится в `local.properties` (не в репозитории), передаётся в BuildConfig через `build.gradle`. Open-Meteo ключей не требует.

---

*Документ составлен на основе собранных требований. Все Out of Scope пункты могут быть включены в последующие версии приложения.*

---

## 15. Детали интеграции с API

### 15.1 Яндекс.Погода — API умного дома (бесплатный)

#### Регистрация и получение ключа

1. Создать **новый** Яндекс ID (отдельный от основного — иначе попадёшь в бизнес-кабинет)
2. В режиме инкогнито авторизоваться под новым аккаунтом
3. Перейти на yandex.ru/pogoda/b2b/smarthome и нажать «Подключиться»
4. API-ключ появится в личном кабинете умного дома
5. Сохранить в `local.properties` как `YANDEX_SMARTHOME_KEY=...`

#### Endpoint и аутентификация

Точный REST-endpoint смартхом API Яндекс не публикует в открытой документации — он идентичен бизнес-API v1/v2, но с ключом умного дома. Структура запроса:

```
Метод:  GET
URL:    https://api.weather.yandex.ru/v2/forecast
Params: lat={lat}&lon={lon}&lang=ru_RU&limit=2&hours=true&extra=true
Header: X-Yandex-Weather-Key: <ключ умного дома>
```

#### Пример ответа (фрагмент)

```json
{
  "fact": {
    "temp": 18,
    "feels_like": 16,
    "wind_speed": 4.5,
    "wind_dir": "nw",
    "condition": "cloudy",
    "icon": "bkn_d"
  },
  "forecasts": [
    {
      "date": "2024-06-25",
      "sunrise": "04:47",
      "sunset": "21:32",
      "parts": {
        "day": {
          "temp_min": 16,
          "temp_max": 22,
          "feels_like": 19,
          "wind_speed": 5.0,
          "wind_dir": "n",
          "condition": "partly-cloudy",
          "icon": "skc_d",
          "prec_prob": 10,
          "prec_type": 0
        },
        "night": { "..." : "..." }
      },
      "hours": [
        {
          "hour": "0",
          "temp": 15,
          "feels_like": 13,
          "wind_speed": 3.2,
          "wind_dir": "nw",
          "condition": "clear",
          "icon": "skc_n",
          "prec_prob": 0,
          "prec_type": 0
        }
      ]
    }
  ]
}
```

#### Значения `condition` (Яндекс v2)

| Значение | Русское |
|---|---|
| `clear` | Ясно |
| `partly-cloudy` | Малооблачно |
| `cloudy` | Облачно с прояснениями |
| `overcast` | Пасмурно |
| `drizzle` | Морось |
| `light-rain` | Небольшой дождь |
| `rain` | Дождь |
| `moderate-rain` | Умеренный дождь |
| `heavy-rain` | Сильный дождь |
| `showers` | Ливень |
| `hail` | Град |
| `thunderstorm` | Гроза |
| `light-snow` | Небольшой снег |
| `snow` | Снег |
| `snowfall` | Снегопад |
| `fog` | Туман |

#### Значения `prec_type`

| Значение | Тип |
|---|---|
| `0` | Без осадков |
| `1` | Дождь |
| `2` | Дождь со снегом |
| `3` | Снег |

#### Значения `wind_dir`

`n`, `ne`, `e`, `se`, `s`, `sw`, `w`, `nw`, `c` (штиль)

#### ⚠️ Ограничение бесплатного тарифа

Поле `humidity` (влажность) в бесплатном API умного дома **не возвращается**. В приложении для дней 1–2:
- В блоке «Сейчас» влажность **скрывается** или отображается как «—»
- Альтернатива: сделать отдельный запрос к Гисметео для текущей влажности (расходует 1 дополнительный запрос)

#### Kotlin — Retrofit interface

```kotlin
interface YandexSmartHomeApi {
    @GET("v2/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String = "ru_RU",
        @Query("limit") limit: Int = 2,
        @Query("hours") hours: Boolean = true
    ): YandexForecastResponse
}

// OkHttp Interceptor для ключа
val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .addHeader("X-Yandex-Weather-Key", BuildConfig.YANDEX_SMARTHOME_KEY)
                .build()
        )
    }
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.weather.yandex.ru/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

---

### 15.2 Open-Meteo — REST API (дни 3–7 + влажность для дней 1–2)

#### Endpoint и параметры

```
Метод:  GET
URL:    https://api.open-meteo.com/v1/forecast
Ключ:   не требуется
```

| Параметр | Значение | Описание |
|---|---|---|
| `latitude` | float | Широта |
| `longitude` | float | Долгота |
| `daily` | список полей | Запрашиваемые дневные данные |
| `hourly` | список полей | Почасовые данные (опционально) |
| `current` | список полей | Текущие данные (для влажности) |
| `wind_speed_unit` | `ms` | Скорость ветра в м/с |
| `timezone` | `auto` | Автоопределение по координатам |
| `forecast_days` | `7` | Количество дней прогноза |
| `models` | `best_match` | Автовыбор лучшей модели (ECMWF для России) |

#### Пример запроса

```
GET https://api.open-meteo.com/v1/forecast
    ?latitude=55.7558
    &longitude=37.6176
    &daily=temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_probability_max,precipitation_sum,weathercode,windspeed_10m_max,winddirection_10m_dominant,sunrise,sunset,relative_humidity_2m_max
    &current=relative_humidity_2m,apparent_temperature,temperature_2m
    &wind_speed_unit=ms
    &timezone=auto
    &forecast_days=7
    &models=best_match
```

#### Структура ответа

```json
{
  "latitude": 55.75,
  "longitude": 37.625,
  "timezone": "Europe/Moscow",
  "current": {
    "time": "2024-06-25T14:00",
    "relative_humidity_2m": 58,
    "temperature_2m": 22.1,
    "apparent_temperature": 21.3
  },
  "daily": {
    "time":                          ["2024-06-25", "2024-06-26", "..."],
    "temperature_2m_max":            [24.1, 21.5, 19.8, 22.3, 25.1, 20.4, 18.9],
    "temperature_2m_min":            [14.2, 13.1, 12.5, 15.0, 16.2, 11.8, 10.3],
    "apparent_temperature_max":      [23.0, 20.1, 18.5, 21.0, 24.0, 19.2, 17.8],
    "apparent_temperature_min":      [13.0, 12.0, 11.5, 14.0, 15.0, 10.8,  9.5],
    "precipitation_probability_max": [10, 40, 70, 20, 5, 55, 80],
    "precipitation_sum":             [0.0, 2.1, 8.5, 0.5, 0.0, 4.2, 12.0],
    "weathercode":                   [1, 61, 63, 3, 0, 80, 95],
    "windspeed_10m_max":             [4.2, 6.8, 9.1, 3.5, 2.8, 7.3, 11.2],
    "winddirection_10m_dominant":    [270, 315, 300, 180, 90, 320, 340],
    "sunrise":                       ["2024-06-25T04:47", "..."],
    "sunset":                        ["2024-06-25T21:32", "..."],
    "relative_humidity_2m_max":      [65, 80, 90, 60, 50, 85, 95]
  }
}
```

#### WMO Weather Code → WeatherCode enum

Open-Meteo использует стандартные коды WMO. Маппинг на внутренний `WeatherCode`:

```kotlin
fun mapWmoCode(code: Int): WeatherCode = when (code) {
    0            -> WeatherCode.CLEAR
    1            -> WeatherCode.CLEAR
    2            -> WeatherCode.PARTLY_CLOUDY
    3            -> WeatherCode.OVERCAST
    45, 48       -> WeatherCode.FOG
    51, 53       -> WeatherCode.RAIN_LIGHT
    55           -> WeatherCode.RAIN
    61, 63       -> WeatherCode.RAIN
    65           -> WeatherCode.RAIN_HEAVY
    71, 73       -> WeatherCode.SNOW_LIGHT
    75           -> WeatherCode.SNOW
    77           -> WeatherCode.SNOW_LIGHT
    80, 81       -> WeatherCode.RAIN_LIGHT
    82           -> WeatherCode.RAIN_HEAVY
    85, 86       -> WeatherCode.SNOW
    95           -> WeatherCode.THUNDERSTORM
    96, 99       -> WeatherCode.THUNDERSTORM
    else         -> WeatherCode.CLOUDY
}
```

#### Направление ветра — градусы → строка

```kotlin
fun degreesToDirection(deg: Int): String = when ((deg + 22) / 45 % 8) {
    0 -> "С"
    1 -> "СВ"
    2 -> "В"
    3 -> "ЮВ"
    4 -> "Ю"
    5 -> "ЮЗ"
    6 -> "З"
    7 -> "СЗ"
    else -> "—"
}
```

#### Маппинг полей Open-Meteo → внутренняя модель

| Поле Open-Meteo | Внутреннее поле | Примечание |
|---|---|---|
| `temperature_2m_max/min` | `DailyWeather.tempMax/Min` | °С |
| `apparent_temperature_max` | `DailyWeather.feelsLike` | °С |
| `relative_humidity_2m_max` | `DailyWeather.humidity` | % |
| `windspeed_10m_max` | `DailyWeather.windSpeed` | м/с |
| `winddirection_10m_dominant` | `DailyWeather.windDirection` | градусы → строка |
| `precipitation_probability_max` | `DailyWeather.precipProb` | % |
| `weathercode` | `DailyWeather.condition` | WMO → WeatherCode |
| `sunrise` / `sunset` | `DailyWeather.sunrise/sunset` | строка времени |
| `current.relative_humidity_2m` | `CurrentWeather.humidity` | для блока «сейчас» |

#### Kotlin — Retrofit interface

```kotlin
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude")                        lat: Double,
        @Query("longitude")                       lon: Double,
        @Query("daily") @JvmField               daily: String =
            "temperature_2m_max,temperature_2m_min," +
            "apparent_temperature_max,apparent_temperature_min," +
            "precipitation_probability_max,precipitation_sum," +
            "weathercode,windspeed_10m_max," +
            "winddirection_10m_dominant,sunrise,sunset," +
            "relative_humidity_2m_max",
        @Query("current")                       current: String =
            "relative_humidity_2m,apparent_temperature,temperature_2m",
        @Query("wind_speed_unit")         windUnit: String = "ms",
        @Query("timezone")                  timezone: String = "auto",
        @Query("forecast_days")          forecastDays: Int    = 7,
        @Query("models")                    models: String = "best_match"
    ): OpenMeteoResponse
}

val openMeteoRetrofit = Retrofit.Builder()
    .baseUrl("https://api.open-meteo.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
// Никаких заголовков аутентификации не нужно
```

---

### 15.3 Геокодирование — Nominatim (OSM)

#### Endpoint

```
GET https://nominatim.openstreetmap.org/search
    ?q={city_name}
    &format=json
    &limit=1
    &accept-language=ru
```

**Обязательный заголовок:** `User-Agent: WeatherApp/1.0 (your@email.com)`

#### Пример ответа

```json
[
  {
    "display_name": "Москва, Центральный федеральный округ, Россия",
    "lat": "55.7504461",
    "lon": "37.6174943"
  }
]
```

**Ограничения:** максимум 1 запрос/сек; результат кэшируется в SharedPreferences до смены города.

---

### 15.4 Схема взаимодействия компонентов

```
WeatherSyncWorker (WorkManager, каждый час)
│
├─► YandexSmartHomeApi.getForecast(lat, lon, limit=2, hours=true)
│       GET https://api.weather.yandex.ru/v2/forecast
│       → YandexForecastDto {
│           fact:      CurrentWeather (без humidity)
│           forecasts: [day1, day2] + hourly
│         }
│
├─► OpenMeteoApi.getForecast(lat, lon, days=7)
│       GET https://api.open-meteo.com/v1/forecast
│       → OpenMeteoDto {
│           current.relative_humidity_2m   ← влажность «сейчас»
│           daily[0..6]: прогноз на 7 дней включая humidity
│         }
│
└─► WeatherRepository.merge(yandexDto, openMeteoDto)
        → WeatherForecast {
            current: CurrentWeather {
                       ...яндекс fields...,
                       humidity: openMeteoDto.current.relative_humidity_2m
                     }
            hourly:  List<HourlyWeather>    ← из Яндекс day1.hours
            daily:   List<DailyWeather> [7 элементов]:
                       [0]: Яндекс day1 + humidity из OpenMeteo daily[0]
                       [1]: Яндекс day2 + humidity из OpenMeteo daily[1]
                       [2..6]: OpenMeteo daily[2..6]
          }
        → WeatherCacheManager.save(forecast)
        → AppWidgetManager.update()
```

---

### 15.5 Обработка ошибок API

| Ситуация | Поведение |
|---|---|
| Яндекс недоступен (timeout / 5xx) | Дни 1–2 из кэша, показать время кэша |
| Open-Meteo недоступен | Дни 3–7 из кэша; дни 1–2 без humidity (показать «—») |
| Оба недоступны, кэш актуален (<24ч) | Все данные из кэша + баннер «Нет соединения» |
| Оба недоступны, кэш устарел (>24ч) | Экран-заглушка с кнопкой «Повторить» |
| Nominatim недоступен | Сообщение «Не удалось найти город», сохранить прежний |
| HTTP 401 от Яндекс | «Неверный API-ключ» в настройках |
| HTTP 429 / лимит исчерпан (Яндекс ~30/сутки) | Использовать кэш до следующих суток, не показывать ошибку пользователю |
| HTTP 429 от Open-Meteo (>10 000/сутки) | Практически невозможно при личном использовании; fallback на кэш |

---

### 15.6 Хранение ключей

```properties
# local.properties (не коммитить в git, добавить в .gitignore)
YANDEX_SMARTHOME_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
# Open-Meteo ключей не требует
```

```kotlin
// build.gradle (app)
android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "YANDEX_SMARTHOME_KEY",
            "\"${properties["YANDEX_SMARTHOME_KEY"]}\"")
    }
}
```

---

*Документ составлен на основе собранных требований. Все Out of Scope пункты могут быть включены в последующие версии приложения.*

#### Регистрация и получение ключа

1. Зайти на https://yandex.ru/pogoda/b2b/console/home
2. Авторизоваться через Яндекс ID (при использовании бесплатного API умного дома — создать **новый** аккаунт, не связанный с бизнес-кабинетом)
3. Создать ключ в разделе «API Погодных Данных»
4. Сохранить ключ в `local.properties` как `YANDEX_WEATHER_KEY=...`

#### Endpoint и аутентификация

```
Метод:  POST
URL:    https://api.weather.yandex.ru/graphql/query
Header: X-Yandex-Weather-Key: <ключ>
Header: Content-Type: application/json
Body:   { "query": "<GraphQL-запрос>" }
```

#### Запрос текущей погоды + почасовой прогноз на сегодня + дневной прогноз на 2 дня

```graphql
{
  weatherByPoint(request: { lat: 55.7558, lon: 37.6176 }) {
    now {
      temperature
      feelsLike
      humidity
      windSpeed
      windDirection
      condition
      icon
    }
    forecast {
      days(limit: 2) {
        time
        sunriseTime
        sunsetTime
        parts {
          day {
            avgTemperature
            minTemperature
            maxTemperature
            humidity
            windSpeed
            windDirection
            condition
            icon
            precStrength
            precType
            precProb
          }
          night {
            avgTemperature
            condition
            icon
          }
        }
        hours {
          time
          temperature
          feelsLike
          humidity
          windSpeed
          windDirection
          condition
          icon
          precProb
          precType
        }
      }
    }
  }
}
```

#### Пример ответа (фрагмент `now`)

```json
{
  "data": {
    "weatherByPoint": {
      "now": {
        "temperature": 18,
        "feelsLike": 16,
        "humidity": 72,
        "windSpeed": 4.5,
        "windDirection": "NORTH_WEST",
        "condition": "CLOUDY",
        "icon": "bkn_d"
      },
      "forecast": {
        "days": [...]
      }
    }
  }
}
```

#### Значения `windDirection`

| Значение | Русское |
|---|---|
| `NORTH` | С |
| `NORTH_EAST` | СВ |
| `EAST` | В |
| `SOUTH_EAST` | ЮВ |
| `SOUTH` | Ю |
| `SOUTH_WEST` | ЮЗ |
| `WEST` | З |
| `NORTH_WEST` | СЗ |
| `CALM` | штиль |

#### Значения `condition` (основные)

| Значение | Русское |
|---|---|
| `CLEAR` | Ясно |
| `PARTLY_CLOUDY` | Малооблачно |
| `CLOUDY` | Облачно |
| `OVERCAST` | Пасмурно |
| `DRIZZLE` | Морось |
| `LIGHT_RAIN` | Небольшой дождь |
| `RAIN` | Дождь |
| `MODERATE_RAIN` | Умеренный дождь |
| `HEAVY_RAIN` | Сильный дождь |
| `SHOWERS` | Ливень |
| `HAIL` | Град |
| `THUNDERSTORM` | Гроза |
| `LIGHT_SNOW` | Небольшой снег |
| `SNOW` | Снег |
| `SNOWFALL` | Снегопад |
| `SNOW_SHOWERS` | Снежные заряды |
| `FOG` | Туман |

#### Лимиты бесплатного API умного дома

- ~30 запросов/сутки
- Данные только на 2 дня
- Без влажности и давления в бесплатном тарифе (только температура, ветер, осадки, «ощущается как», иконка)
- При часовом обновлении: 24 запроса/сутки — укладывается в лимит

#### Реализация на Kotlin (Retrofit)

```kotlin
// retrofit interface
interface YandexWeatherApi {
    @POST("graphql/query")
    suspend fun getWeather(
        @Header("X-Yandex-Weather-Key") apiKey: String,
        @Body body: GraphQlRequest
    ): YandexWeatherResponse
}

data class GraphQlRequest(val query: String)

// OkHttp client — ключ через интерцептор
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-Yandex-Weather-Key", BuildConfig.YANDEX_WEATHER_KEY)
            .build()
        chain.proceed(request)
    }
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.weather.yandex.ru/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

---

### 15.2 ДАНИО-пресс — JSON API (дни 3–7)

#### Endpoint и параметры

```
Метод: GET
URL:   http://lb1.hmn.ru/api/dw_api_v1.php
```

| Параметр | Тип | Описание |
|---|---|---|
| `lat` | float | Широта точки |
| `lon` | float | Долгота точки |
| `type` | int | Тип прогноза: `1` — день/ночь, `2` — ночь/утро/день/вечер, `3` — по часам |
| `period` | int | Количество суток прогноза (1–10) |
| `mode` | string | `json` |
| `cid` | string | Идентификатор клиента (выдаётся при регистрации) |

#### Пример запроса (прогноз на 5 дней по частям дня)

```
GET http://lb1.hmn.ru/api/dw_api_v1.php
    ?lat=55.7558
    &lon=37.6176
    &type=2
    &period=5
    &mode=json
    &cid=YOUR_CLIENT_ID
```

#### Структура ответа (фрагмент `forecast_6`)

```json
{
  "forecast_6": {
    "1": {
      "sunrise": "2024-06-25T04:47:00+03:00",
      "sunset":  "2024-06-25T21:32:00+03:00",
      "start_date": "2024-06-25T00:00:00+03:00",
      "night": {
        "condition":   "clear",
        "condition_s": "ясно",
        "temp_min":    12,
        "temp_max":    15,
        "temp":        13,
        "feels_like":  11,
        "humidity":    78,
        "prec_prob":   5,
        "wind_speed":  2,
        "wind_gust":   5,
        "wind_dir":    "nw"
      },
      "morning": { ... },
      "day":     { ... },
      "evening": { ... }
    },
    "2": { ... },
    "3": { ... },
    "4": { ... },
    "5": { ... }
  }
}
```

#### Маппинг полей ДАНИО-пресс → внутренняя модель

| Поле API | Внутреннее поле | Примечание |
|---|---|---|
| `temp_min` / `temp_max` | `DailyWeather.tempMin/Max` | °С |
| `feels_like` | `DailyWeather.feelsLike` | °С |
| `humidity` | `DailyWeather.humidity` | % |
| `wind_speed` | `DailyWeather.windSpeed` | м/с |
| `wind_dir` | `DailyWeather.windDirection` | строка (`n`, `ne`, `e`...) |
| `prec_prob` | `DailyWeather.precipProb` | % |
| `condition` | `DailyWeather.condition` | строка → `WeatherCode` enum |
| `condition_s` | — | для логирования/отладки |

#### Маппинг кодов состояния ДАНИО-пресс → WeatherCode

```kotlin
enum class WeatherCode {
    CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST,
    RAIN_LIGHT, RAIN, RAIN_HEAVY,
    SNOW_LIGHT, SNOW, SNOWFALL,
    THUNDERSTORM, FOG, MIXED
}

fun mapDanioCondition(condition: String): WeatherCode = when {
    condition.contains("clear")          -> WeatherCode.CLEAR
    condition.contains("partly-cloudy")  -> WeatherCode.PARTLY_CLOUDY
    condition.contains("cloudy")         -> WeatherCode.CLOUDY
    condition.contains("overcast")       -> WeatherCode.OVERCAST
    condition.contains("light_rain")     -> WeatherCode.RAIN_LIGHT
    condition.contains("rain")           -> WeatherCode.RAIN
    condition.contains("heavy_rain")     -> WeatherCode.RAIN_HEAVY
    condition.contains("light_snow")     -> WeatherCode.SNOW_LIGHT
    condition.contains("snow")           -> WeatherCode.SNOW
    condition.contains("thunderstorm")   -> WeatherCode.THUNDERSTORM
    condition.contains("fog")            -> WeatherCode.FOG
    else                                 -> WeatherCode.CLOUDY
}
```

---

### 15.3 Геокодирование — Nominatim (OSM)

Используется для преобразования названия города, введённого вручную, в координаты `lat/lon`.

#### Endpoint

```
GET https://nominatim.openstreetmap.org/search
    ?q={city_name}
    &format=json
    &limit=1
    &accept-language=ru
```

**Обязательный заголовок:** `User-Agent: WeatherApp/1.0 (your@email.com)`
(без него Nominatim может заблокировать запросы)

#### Пример запроса и ответа

```
GET https://nominatim.openstreetmap.org/search?q=Москва&format=json&limit=1
```

```json
[
  {
    "place_id": 12345,
    "display_name": "Москва, Центральный федеральный округ, Россия",
    "lat": "55.7504461",
    "lon": "37.6174943",
    "type": "city"
  }
]
```

#### Ограничения

- Максимум 1 запрос в секунду (rate limit)
- Только при ручном поиске города — не при каждом обновлении погоды
- Координаты после получения кэшируются в SharedPreferences и повторно не запрашиваются до смены города

---

### 15.4 Схема взаимодействия компонентов

```
WeatherSyncWorker (WorkManager, каждый час)
│
├─► YandexWeatherApi.getWeather(lat, lon)
│       POST https://api.weather.yandex.ru/graphql/query
│       → YandexWeatherDto (now + hourly + 2 days)
│
├─► DanioWeatherApi.getForecast(lat, lon, days=5)
│       GET http://lb1.hmn.ru/api/dw_api_v1.php
│       → DanioForecastDto (days 3–7)
│
└─► WeatherRepository.merge(yandexDto, danioDto)
        → WeatherForecast {
            current:  CurrentWeather       // из Яндекс.now
            hourly:   List<HourlyWeather>  // из Яндекс.forecast.days[0].hours
            daily:    List<DailyWeather>   // [0-1] Яндекс, [2-6] ДАНИО
          }
        → WeatherCacheManager.save(forecast)
        → AppWidgetManager.update(widgetIds)
```

---

### 15.5 Обработка ошибок API

| Ситуация | Поведение |
|---|---|
| Яндекс API недоступен (timeout / 5xx) | Использовать кэш для дней 1–2, показать время кэша |
| ДАНИО API недоступен | Использовать кэш для дней 3–7 |
| Оба API недоступны, кэш актуален | Показать все данные из кэша с баннером «Нет соединения» |
| Оба API недоступны, кэш устарел (>24ч) | Показать заглушку с кнопкой «Повторить» |
| Nominatim недоступен | Показать сообщение «Не удалось найти город» и сохранить прежний |
| HTTP 401 от Яндекс | Показать сообщение «Неверный API-ключ» в настройках |
| HTTP 429 от Яндекс | Exponential backoff, следующая попытка через 2x интервал |
