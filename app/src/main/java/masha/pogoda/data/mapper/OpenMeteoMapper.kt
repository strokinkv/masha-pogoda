package masha.pogoda.data.mapper

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import masha.pogoda.data.api.OpenMeteoResponse
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.HourlyWeather

fun OpenMeteoResponse.toDaily(): List<DailyWeather> {
    // API возвращает параллельные массивы; берём пересечение длин, чтобы
    // не словить IndexOutOfBounds, если какой-то массив окажется короче.
    val count = listOf(
        daily.time.size,
        daily.weatherCode.size,
        daily.temperatureMin.size,
        daily.temperatureMax.size,
        daily.apparentTemperatureMax.size,
        daily.relativeHumidityMax.size,
        daily.windSpeedMax.size,
        daily.windDirectionDominant.size,
        daily.precipitationProbabilityMax.size
    ).min()
    return (0 until count).map { index ->
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

fun OpenMeteoResponse.toHourly(): List<HourlyWeather> {
    val count = listOf(
        hourly.time.size,
        hourly.weatherCode.size,
        hourly.temperature.size,
        hourly.isDay.size,
        hourly.precipitationProbability.size
    ).min()
    return (0 until count).map { index ->
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
