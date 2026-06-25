package masha.pogoda.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeather(
    val temperature: Int,
    val feelsLike: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: String,
    val pressure: Int,
    val code: WeatherCode,
    val iconCode: String,
    val description: String,
    val updatedAt: Long
)

