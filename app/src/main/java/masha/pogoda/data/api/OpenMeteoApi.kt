package masha.pogoda.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = OPEN_METEO_CURRENT,
        @Query("hourly") hourly: String = OPEN_METEO_HOURLY,
        @Query("daily") daily: String = OPEN_METEO_DAILY,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse
}

const val OPEN_METEO_CURRENT =
    "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m," +
        "wind_direction_10m,surface_pressure,weather_code,is_day"

const val OPEN_METEO_HOURLY =
    "temperature_2m,weather_code,precipitation_probability,is_day"

const val OPEN_METEO_DAILY =
    "temperature_2m_max,temperature_2m_min,apparent_temperature_max," +
        "relative_humidity_2m_max,wind_speed_10m_max,wind_direction_10m_dominant," +
        "precipitation_probability_max,weather_code,sunrise,sunset"

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current: OpenMeteoCurrent,
    val hourly: OpenMeteoHourly,
    val daily: OpenMeteoDaily
)

@Serializable
data class OpenMeteoCurrent(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Int,
    @SerialName("surface_pressure") val surfacePressure: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("is_day") val isDay: Int
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>,
    @SerialName("is_day") val isDay: List<Int>
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double>,
    @SerialName("relative_humidity_2m_max") val relativeHumidityMax: List<Int>,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double>,
    @SerialName("wind_direction_10m_dominant") val windDirectionDominant: List<Int>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    val sunrise: List<String>,
    val sunset: List<String>
)

