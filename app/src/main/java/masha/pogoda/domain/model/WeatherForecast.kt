package masha.pogoda.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherForecast(
    val city: String,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val cachedAt: Long
)

