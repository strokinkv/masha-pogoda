package masha.pogoda.data.cache

import java.io.File
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.WeatherCode
import masha.pogoda.domain.model.WeatherForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCacheManagerTest {
    @Test
    fun roundTripAndTtl() {
        val manager = WeatherCacheManager(createTempDir())
        val forecast = sampleForecast(cachedAt = 1000L)

        manager.save(forecast)

        assertEquals(forecast.city, manager.load()!!.city)
        assertFalse(manager.isStale(forecast, now = 1000L + 23 * 3600_000L))
        assertTrue(manager.isStale(forecast, now = 1000L + 25 * 3600_000L))
    }

    private fun createTempDir(): File =
        kotlin.io.path.createTempDirectory("weather-cache-test").toFile()

    private fun sampleForecast(cachedAt: Long) = WeatherForecast(
        city = "Москва",
        current = CurrentWeather(
            temperature = 20,
            feelsLike = 19,
            humidity = 60,
            windSpeed = 3.0,
            windDirection = "С",
            pressure = 1000,
            code = WeatherCode.CLEAR,
            iconCode = "clear_day",
            description = "Ясно",
            updatedAt = cachedAt
        ),
        hourly = emptyList(),
        daily = listOf(
            DailyWeather(
                date = "2026-06-25",
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
        ),
        cachedAt = cachedAt
    )
}

