package masha.pogoda.data.mapper

import masha.pogoda.domain.model.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMapperTest {
    @Test
    fun wmo() {
        assertEquals(WeatherCode.CLEAR, mapWmoCode(0))
        assertEquals(WeatherCode.PARTLY_CLOUDY, mapWmoCode(2))
        assertEquals(WeatherCode.OVERCAST, mapWmoCode(3))
        assertEquals(WeatherCode.FOG, mapWmoCode(45))
        assertEquals(WeatherCode.RAIN, mapWmoCode(63))
        assertEquals(WeatherCode.RAIN_HEAVY, mapWmoCode(65))
        assertEquals(WeatherCode.SNOW, mapWmoCode(75))
        assertEquals(WeatherCode.THUNDERSTORM, mapWmoCode(95))
    }

    @Test
    fun yandexCond() {
        assertEquals(WeatherCode.CLEAR, mapYandexCondition("CLEAR"))
        assertEquals(WeatherCode.OVERCAST, mapYandexCondition("OVERCAST"))
        assertEquals(WeatherCode.RAIN, mapYandexCondition("RAIN"))
        assertEquals(WeatherCode.THUNDERSTORM, mapYandexCondition("THUNDERSTORM"))
        assertEquals(WeatherCode.RAIN_LIGHT, mapYandexCondition("light_rain"))
    }
}

