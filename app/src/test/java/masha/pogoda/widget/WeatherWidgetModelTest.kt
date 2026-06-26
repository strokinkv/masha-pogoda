package masha.pogoda.widget

import java.time.LocalDateTime
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.HourlyWeather
import masha.pogoda.domain.model.WeatherCode
import masha.pogoda.domain.model.WeatherForecast
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherWidgetModelTest {
    @Test
    fun dailyModeTakesFirstFiveDailyForecasts() {
        val model = WeatherWidgetModel.from(forecast(days = 5))

        assertEquals("Москва", model.city)
        assertEquals(5, model.entries.size)
        assertEquals("01.07", model.entries[0].label)
        assertEquals("05.07", model.entries[4].label)
        assertEquals("11…21°", model.entries[0].temperature)
    }

    @Test
    fun hourlyModeTakesNextFiveHourlyForecasts() {
        val model = WeatherWidgetModel.from(
            forecast(days = 5, hours = 12),
            showHourly = true,
            now = LocalDateTime.parse("2026-07-01T09:20")
        )

        assertEquals(5, model.entries.size)
        assertEquals("10:00", model.entries[0].label)
        assertEquals("14:00", model.entries[4].label)
        assertEquals("14°", model.entries[4].temperature)
    }

    private fun forecast(days: Int, hours: Int = 0): WeatherForecast =
        WeatherForecast(
            city = "Москва",
            current = CurrentWeather(
                temperature = 21,
                feelsLike = 20,
                humidity = 55,
                windSpeed = 3.0,
                windDirection = "С",
                pressure = 750,
                code = WeatherCode.CLEAR,
                iconCode = "clear_day",
                description = "ясно",
                updatedAt = 1_719_816_000_000
            ),
            hourly = (0 until hours).map { hour ->
                HourlyWeather(
                    time = "2026-07-01T${(8 + hour).toString().padStart(2, '0')}:00",
                    temperature = 8 + hour,
                    code = WeatherCode.CLEAR,
                    iconCode = "clear_day",
                    precipProb = 0
                )
            },
            daily = (1..days).map { day ->
                DailyWeather(
                    date = "2026-07-${day.toString().padStart(2, '0')}",
                    tempMin = 10 + day,
                    tempMax = 20 + day,
                    feelsLike = 20,
                    humidity = 50,
                    windSpeed = 2.0,
                    windDirection = "С",
                    precipProb = 0,
                    code = WeatherCode.CLEAR,
                    iconCode = "clear_day",
                    sunrise = null,
                    sunset = null
                )
            },
            cachedAt = 1_719_816_000_000
        )
}
