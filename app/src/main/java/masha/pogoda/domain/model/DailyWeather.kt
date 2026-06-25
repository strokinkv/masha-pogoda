package masha.pogoda.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyWeather(
    val date: String,
    val tempMin: Int,
    val tempMax: Int,
    val feelsLike: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: String,
    val precipProb: Int,
    val code: WeatherCode,
    val iconCode: String,
    val sunrise: String?,
    val sunset: String?
)

