package masha.pogoda.data.mapper

import kotlinx.serialization.json.Json
import masha.pogoda.data.api.OpenMeteoCurrent
import masha.pogoda.data.api.OpenMeteoDaily
import masha.pogoda.data.api.OpenMeteoHourly
import masha.pogoda.data.api.OpenMeteoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoMapperTest {
    private fun load() = Json { ignoreUnknownKeys = true }.decodeFromString<OpenMeteoResponse>(
        javaClass.getResource("/fixtures/open_meteo.json")!!.readText()
    )

    @Test
    fun mapsSevenDays() {
        val response = load()
        val daily = response.toDaily()
        assertEquals(7, daily.size)
        assertEquals(response.daily.time[0], daily[0].date)
        assertTrue(daily[0].tempMax >= daily[0].tempMin)
    }

    @Test
    fun mapsCurrent() {
        val current = load().toCurrent()
        assertTrue(current.humidity in 0..100)
        assertTrue(current.pressure > 0)
    }

    @Test
    fun mapsHourlyForecast() {
        val hourly = load().toHourly()

        assertTrue(hourly.isNotEmpty())
        assertTrue(hourly.any { it.time.startsWith("2026-06-26") })
    }

    @Test
    fun toDailyDoesNotThrowOnMismatchedArrayLengths() {
        // time содержит 3 элемента, но temperatureMin — только 2: берём пересечение длин.
        val daily = responseWith(
            daily = OpenMeteoDaily(
                time = listOf("2026-06-26", "2026-06-27", "2026-06-28"),
                temperatureMax = listOf(20.0, 21.0, 22.0),
                temperatureMin = listOf(10.0, 11.0),
                apparentTemperatureMax = listOf(20.0, 21.0, 22.0),
                relativeHumidityMax = listOf(50, 55, 60),
                windSpeedMax = listOf(3.0, 4.0, 5.0),
                windDirectionDominant = listOf(180, 200, 220),
                precipitationProbabilityMax = listOf(0, 10, 20),
                weatherCode = listOf(0, 1, 2),
                sunrise = listOf("2026-06-26T04:00"),
                sunset = listOf("2026-06-26T22:00")
            )
        ).toDaily()

        assertEquals(2, daily.size)
    }

    @Test
    fun toHourlyDoesNotThrowOnMismatchedArrayLengths() {
        val hourly = responseWith(
            hourly = OpenMeteoHourly(
                time = listOf("2026-06-26T00:00", "2026-06-26T01:00", "2026-06-26T02:00"),
                temperature = listOf(10.0, 11.0),
                weatherCode = listOf(0, 1, 2),
                precipitationProbability = listOf(0, 5, 10),
                isDay = listOf(1, 1, 1)
            )
        ).toHourly()

        assertEquals(2, hourly.size)
    }

    private fun responseWith(
        daily: OpenMeteoDaily = emptyDaily(),
        hourly: OpenMeteoHourly = emptyHourly()
    ): OpenMeteoResponse =
        OpenMeteoResponse(
            latitude = 55.0,
            longitude = 37.0,
            timezone = "Europe/Moscow",
            current = OpenMeteoCurrent(
                time = "2026-06-26T00:00",
                temperature = 10.0,
                apparentTemperature = 9.0,
                relativeHumidity = 50,
                windSpeed = 3.0,
                windDirection = 180,
                surfacePressure = 1000.0,
                weatherCode = 0,
                isDay = 1
            ),
            hourly = hourly,
            daily = daily
        )

    private fun emptyDaily() = OpenMeteoDaily(
        time = emptyList(),
        temperatureMax = emptyList(),
        temperatureMin = emptyList(),
        apparentTemperatureMax = emptyList(),
        relativeHumidityMax = emptyList(),
        windSpeedMax = emptyList(),
        windDirectionDominant = emptyList(),
        precipitationProbabilityMax = emptyList(),
        weatherCode = emptyList(),
        sunrise = emptyList(),
        sunset = emptyList()
    )

    private fun emptyHourly() = OpenMeteoHourly(
        time = emptyList(),
        temperature = emptyList(),
        weatherCode = emptyList(),
        precipitationProbability = emptyList(),
        isDay = emptyList()
    )
}
