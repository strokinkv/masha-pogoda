package masha.pogoda.widget

import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.WeatherCode
import masha.pogoda.domain.model.WeatherForecast
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherWidgetModelTest {
    @Test
    fun takesFirstThreeDailyForecasts() {
        val model = WeatherWidgetModel.from(forecast(days = 5))

        assertEquals("Москва", model.city)
        assertEquals(3, model.days.size)
        assertEquals("01.07", model.days[0].dateLabel)
        assertEquals("03.07", model.days[2].dateLabel)
    }

    private fun forecast(days: Int): WeatherForecast =
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
            hourly = emptyList(),
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
