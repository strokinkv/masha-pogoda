package masha.pogoda.work

object WeatherSyncPolicy {
    // Непрозрачный идентификатор периодической задачи WorkManager.
    // Менять строку нельзя: иначе уже запланированная задача останется «осиротевшей».
    const val UNIQUE_WORK_NAME = "weather_hourly_sync"

    // Период фоновой синхронизации погоды (минуты).
    const val REPEAT_MINUTES = 120L
}
