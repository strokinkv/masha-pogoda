package masha.pogoda.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HourlyWeather(
    val time: String,
    val temperature: Int,
    val code: WeatherCode,
    val iconCode: String,
    val precipProb: Int
)

