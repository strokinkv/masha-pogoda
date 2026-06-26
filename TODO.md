# TODO

## Пожелания по фичам (от пользователя)

---

# Code review (2026-06-26)

Статус сборки: `./gradlew testDebugUnitTest assembleDebug` — проходят (тесты + сборка APK).
Архитектура чистая и слоистая (data / domain / ui / widget / work), DI через `ServiceLocator`,
деградация Yandex → Open-Meteo реализована корректно и покрыта тестами. Ниже — замечания по убыванию важности.

> **Статус выполнения (2026-06-26):** все 16 пунктов реализованы. Решения пользователя:
> #12 — оставлен период 120 мин, из названий убрано «hourly», строка-ID задачи WorkManager не менялась;
> #13 — в `.gitignore` добавлена только `screenshots/` (файлы `2.png`/`for_icon.png` не трогали).
> Оговорки по проверке: #11 (исключение prefs из бэкапа) — XML структурно корректен и проходит сборку,
> но фактическое поведение бэкапа на устройстве не проверялось; #3 (осадки из ближайшего часа) — логика
> в `MainActivity.bindForecast`, юнит-тестом не покрыта (UI-уровень). Виджет после ручного обновления
> перерисовывается вне UI-потока и только при новом `cachedAt` (без повторов на каждый возврат в приложение).

## Баги / корректность

1. **Виджет не обновляется после ручного refresh в приложении.**
   `MainViewModel.refresh()` сохраняет прогноз в кэш, но `WeatherWidget.updateAll()` вызывается
   только из `WeatherSyncWorker`. После pull-to-refresh в приложении виджет остаётся устаревшим
   до следующей фоновой синхронизации (раз в 2 часа).
   → `MainViewModel.kt:28`, `WeatherWidget.kt:53`. Фикс: дёргать `WeatherWidget.updateAll(context)`
   после успешного `repository.refresh` (например, из Activity после `Success`).

2. **Кэш никогда не показывается мгновенно — всегда сначала сеть.**
   `WeatherRepository.refresh()` при каждом запуске идёт в сеть; кэш используется только при ошибке
   (`cacheFallback`). Каждое открытие приложения = спиннер Loading и ожидание сети, даже если есть
   свежий кэш. Плохой холодный старт/офлайн-опыт.
   → `WeatherRepository.kt:35`. Фикс: паттерн cache-first — отдать кэш сразу, затем обновить в фоне.

3. **Несогласованный горизонт в `weatherToAdvice`.**
   Совет берёт `precipProb` из `forecast.daily.firstOrNull()` (макс. за сегодня), но температуру/код/ветер —
   из `current`. Дневная вероятность осадков может выдать «возьми зонт» при ясной текущей погоде.
   → `MainActivity.kt:137`, `WeatherCodeMapper.kt:50`. Фикс: брать осадки из current/ближайших часов.

4. **Параллельный доступ по индексу в `OpenMeteoMapper`.**
   `toDaily`/`toHourly` обращаются к нескольким спискам по `[index]`, предполагая равную длину
   (защищены через `getOrNull` только `sunrise`/`sunset`). Если API вернёт массив короче — `IndexOutOfBoundsException`.
   Исключение поглотится `runCatching` в репозитории и тихо деградирует в ошибку с потерей данных.
   → `OpenMeteoMapper.kt:12`. Фикс: `getOrNull` для всех полей или валидация длины.

5. **Несоответствие часовых поясов в окне «следующих часов».**
   `HourlyForecastWindow.nextHours` сравнивает локальное время прогноза с `LocalDateTime.now()` устройства.
   Open-Meteo отдаёт время в TZ локации (`timezone=auto`); при отличии TZ устройства от TZ локации
   окно смещается. Краевой случай (GPS в поездке), но реальный.
   → `HourlyForecastWindow.kt:13`.

## Архитектура / поддерживаемость

6. **Глобальный мутабельный `ServiceLocator.yandexKeyProvider`.**
   Присваивается из `MainActivity` и `WeatherSyncWorker`, читается OkHttp-интерсептором с произвольных
   потоков. Гонка при одновременной работе воркера и UI (сейчас безвредна — значение одинаковое, но хрупко).
   → `ServiceLocator.kt:13`. Фикс: читать prefs прямо в интерсепторе или внедрять зависимость.

7. **Дублирование сборки зависимостей.**
   Одинаковая инициализация `WeatherRepository`/`LocationProvider` скопирована в
   `MainActivity.viewModelFactory` и `WeatherSyncWorker.doWork`. Риск расхождения.
   → `MainActivity.kt:159`, `WeatherSyncWorker.kt:18`. Фикс: одна фабрика в `ServiceLocator`/`App`.

8. **Две идентичные функции `toRuDescription`.**
   Тела `WeatherCode.toRuDescription()` в `OpenMeteoMapper` и `toYandexRuDescription()` в `YandexMapper`
   полностью совпадают. → `OpenMeteoMapper.kt:69`, `YandexMapper.kt:60`. Свести в одну общую.

## Мелочи / полировка

9. **Двойной refresh при первом запуске.** `MainViewModel.init{refresh()}` стартует с дефолтной локацией
   (Москва), затем `onRequestPermissionsResult` → второй `refresh()`. Два сетевых запроса подряд.
   → `MainViewModel.kt:24`, `MainActivity.kt:79`.

10. **Неиспользуемое разрешение `RECEIVE_BOOT_COMPLETED`.** Boot-ресивера нет; WorkManager и `App.onCreate`
    и так перепланируют работу. Удалить разрешение либо добавить задуманный ресивер. → `AndroidManifest.xml`.

11. **`allowBackup="true"` бэкапит Yandex-ключ в открытом виде.** SharedPreferences (включая пользовательский
    API-ключ) попадают в облачный бэкап. Рассмотреть `dataExtractionRules`/исключение или EncryptedSharedPreferences.
    → `AndroidManifest.xml`.

12. **Расхождение имени и значения политики синка.** `REPEAT_MINUTES = 120`, но имя `weather_hourly_sync`
    и коммит «hourly background sync». → `WeatherSyncPolicy.kt`. Привести в соответствие.

13. **Гигиена репозитория.** Неотслеживаемые `2.png` (похоже на случайный артефакт) и `screenshots/`,
    удалён `for_icon.png`. Решить: добавить в `.gitignore` или закоммитить/удалить.

14. **Нет явных таймаутов OkHttp** (дефолт 10с). Для запроса с видимым спиннером стоит задать явно. → `ServiceLocator.kt:22`.

15. **`SimpleDateFormat` создаётся на каждый вызов** в `MainActivity.formatTime` и `WeatherWidgetModel.formatTime`
    (не потокобезопасен, лишние аллокации). Вынести/использовать `DateTimeFormatter`.

16. **Пробелы в тестах.** Нет тестов на `cacheFallback`/stale-ветку репозитория, `MainViewModel`,
    `LocationProvider`, и на рассогласование длин массивов в мапперах.

