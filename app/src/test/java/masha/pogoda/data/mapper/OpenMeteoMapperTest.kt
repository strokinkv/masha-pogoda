package masha.pogoda.data.mapper

import kotlinx.serialization.json.Json
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
    fun mapsHourlyToday() {
        assertTrue(load().toHourlyToday().isNotEmpty())
    }
}

