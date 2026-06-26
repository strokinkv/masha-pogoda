package masha.pogoda.data.repository

import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.HourlyWeather
import masha.pogoda.domain.model.WeatherForecast

fun merge(
    city: String,
    current: CurrentWeather,
    hourly: List<HourlyWeather>,
    yandexDays: List<DailyWeather>,
    openMeteoDays: List<DailyWeather>,
    timezone: String? = null
): WeatherForecast {
    val yandexDates = yandexDays.mapTo(mutableSetOf()) { it.date }
    val mergedDays = (yandexDays + openMeteoDays.filterNot { it.date in yandexDates })
        .sortedBy { it.date }
        .take(7)

    return WeatherForecast(
        city = city,
        current = current,
        hourly = hourly,
        daily = mergedDays,
        cachedAt = System.currentTimeMillis(),
        timezone = timezone
    )
}

