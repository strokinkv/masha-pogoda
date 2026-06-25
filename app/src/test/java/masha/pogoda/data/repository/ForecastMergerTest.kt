package masha.pogoda.data.repository

import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastMergerTest {
    @Test
    fun joinsByDate_sevenUniqueDays() {
        val yandexDays = listOf(day("2026-06-25"), day("2026-06-26"))
        val openMeteoDays = (25..31).map { day("2026-06-%02d".format(it)) }

        val forecast = merge("Москва", current(), emptyList(), yandexDays, openMeteoDays)

        assertEquals(7, forecast.daily.size)
        assertEquals(forecast.daily.map { it.date }, forecast.daily.map { it.date }.distinct())
        assertEquals("2026-06-25", forecast.daily[0].date)
    }

    private fun current() = CurrentWeather(
        temperature = 20,
        feelsLike = 19,
        humidity = 60,
        windSpeed = 3.0,
        windDirection = "С",
        pressure = 1000,
        code = WeatherCode.CLEAR,
        iconCode = "clear_day",
        description = "Ясно",
        updatedAt = 1000L
    )

    private fun day(date: String) = DailyWeather(
        date = date,
        tempMin = 10,
        tempMax = 20,
        feelsLike = 19,
        humidity = 60,
        windSpeed = 3.0,
        windDirection = "С",
        precipProb = 0,
        code = WeatherCode.CLEAR,
        iconCode = "clear_day",
        sunrise = null,
        sunset = null
    )
}

