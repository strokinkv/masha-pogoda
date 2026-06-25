package masha.pogoda.data.mapper

import masha.pogoda.domain.model.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconMapTest {
    @Test
    fun dayNight() {
        assertEquals("clear_day", weatherCodeToIcon(WeatherCode.CLEAR, true))
        assertEquals("clear_night", weatherCodeToIcon(WeatherCode.CLEAR, false))
        assertEquals("overcast", weatherCodeToIcon(WeatherCode.OVERCAST, true))
        assertEquals("thunderstorm", weatherCodeToIcon(WeatherCode.THUNDERSTORM, true))
        assertEquals("fog_night", weatherCodeToIcon(WeatherCode.FOG, false))
    }
}

