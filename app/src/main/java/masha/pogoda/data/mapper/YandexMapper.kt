package masha.pogoda.data.mapper

import masha.pogoda.data.api.YandexResponse
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.HourlyWeather

fun YandexResponse.toCurrent(): CurrentWeather {
    val now = data.weatherByPoint.now
    val code = mapYandexCondition(now.condition)
    return CurrentWeather(
        temperature = now.temperature,
        feelsLike = now.feelsLike,
        humidity = now.humidity,
        windSpeed = now.windSpeed,
        windDirection = yandexWindToRu(now.windDirection),
        pressure = now.pressure,
        code = code,
        iconCode = weatherCodeToIcon(code, now.icon.isDayIcon()),
        description = code.toRuDescription(),
        updatedAt = System.currentTimeMillis()
    )
}

fun YandexResponse.toHourly(): List<HourlyWeather> =
    data.weatherByPoint.forecast.days.flatMap { it.hours }.map { hour ->
        val code = mapYandexCondition(hour.condition)
        HourlyWeather(
            time = hour.time,
            temperature = hour.temperature,
            code = code,
            iconCode = weatherCodeToIcon(code, hour.icon.isDayIcon()),
            precipProb = hour.precProbability?.toInt() ?: 0
        )
    }

fun YandexResponse.toDailyHead(): List<DailyWeather> =
    data.weatherByPoint.forecast.days.map { day ->
        val part = day.parts.day
        val code = mapYandexCondition(part.condition)
        DailyWeather(
            date = day.time,
            tempMin = part.minTemperature,
            tempMax = part.maxTemperature,
            feelsLike = part.maxTemperature,
            humidity = part.humidity,
            windSpeed = part.windSpeed,
            windDirection = yandexWindToRu(part.windDirection),
            precipProb = part.precProbability?.toInt() ?: 0,
            code = code,
            iconCode = weatherCodeToIcon(code, part.icon.isDayIcon()),
            sunrise = day.sunriseTime,
            sunset = day.sunsetTime
        )
    }

private fun String.isDayIcon(): Boolean = !endsWith("_n")
