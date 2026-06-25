package masha.pogoda.data.mapper

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import masha.pogoda.data.api.OpenMeteoResponse
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.HourlyWeather
import masha.pogoda.domain.model.WeatherCode

fun OpenMeteoResponse.toDaily(): List<DailyWeather> =
    daily.time.indices.map { index ->
        val code = mapWmoCode(daily.weatherCode[index])
        DailyWeather(
            date = daily.time[index],
            tempMin = daily.temperatureMin[index].roundToInt(),
            tempMax = daily.temperatureMax[index].roundToInt(),
            feelsLike = daily.apparentTemperatureMax[index].roundToInt(),
            humidity = daily.relativeHumidityMax[index],
            windSpeed = daily.windSpeedMax[index],
            windDirection = degreesToRu(daily.windDirectionDominant[index]),
            precipProb = daily.precipitationProbabilityMax[index],
            code = code,
            iconCode = weatherCodeToIcon(code, isDay = true),
            sunrise = daily.sunrise.getOrNull(index),
            sunset = daily.sunset.getOrNull(index)
        )
    }

fun OpenMeteoResponse.toCurrent(): CurrentWeather {
    val code = mapWmoCode(current.weatherCode)
    return CurrentWeather(
        temperature = current.temperature.roundToInt(),
        feelsLike = current.apparentTemperature.roundToInt(),
        humidity = current.relativeHumidity,
        windSpeed = current.windSpeed,
        windDirection = degreesToRu(current.windDirection),
        pressure = current.surfacePressure.roundToInt(),
        code = code,
        iconCode = weatherCodeToIcon(code, current.isDay == 1),
        description = code.toRuDescription(),
        updatedAt = current.time.toEpochMillis(timezone)
    )
}

fun OpenMeteoResponse.toHourlyToday(): List<HourlyWeather> {
    val today = daily.time.firstOrNull() ?: return emptyList()
    return hourly.time.indices
        .filter { hourly.time[it].startsWith(today) }
        .map { index ->
            val code = mapWmoCode(hourly.weatherCode[index])
            HourlyWeather(
                time = hourly.time[index],
                temperature = hourly.temperature[index].roundToInt(),
                code = code,
                iconCode = weatherCodeToIcon(code, hourly.isDay[index] == 1),
                precipProb = hourly.precipitationProbability[index]
            )
        }
}

private fun String.toEpochMillis(timezone: String): Long =
    runCatching {
        LocalDateTime.parse(this)
            .atZone(ZoneId.of(timezone))
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(0L)

private fun WeatherCode.toRuDescription(): String = when (this) {
    WeatherCode.CLEAR -> "Ясно"
    WeatherCode.PARTLY_CLOUDY -> "Малооблачно"
    WeatherCode.CLOUDY -> "Облачно"
    WeatherCode.OVERCAST -> "Пасмурно"
    WeatherCode.FOG -> "Туман"
    WeatherCode.RAIN_LIGHT -> "Небольшой дождь"
    WeatherCode.RAIN -> "Дождь"
    WeatherCode.RAIN_HEAVY -> "Сильный дождь"
    WeatherCode.SNOW_LIGHT -> "Небольшой снег"
    WeatherCode.SNOW -> "Снег"
    WeatherCode.SNOWFALL -> "Снегопад"
    WeatherCode.THUNDERSTORM -> "Гроза"
    WeatherCode.MIXED -> "Мокрый снег"
}

