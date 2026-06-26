package masha.pogoda.data.mapper

import kotlinx.serialization.json.Json
import masha.pogoda.data.api.MetNoResponse
import masha.pogoda.domain.model.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetNoMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun load(): MetNoResponse =
        json.decodeFromString(javaClass.getResource("/fixtures/met_no.json")!!.readText())

    @Test
    fun mapsSevenDays() {
        val daily = load().toDaily()
        assertEquals(7, daily.size)
        daily.forEach { day ->
            assertTrue(day.tempMax >= day.tempMin)
            assertTrue(day.humidity in 0..100)
            assertTrue(day.precipProb in 0..100)
        }
    }

    @Test
    fun mapsCurrent() {
        val current = load().toCurrent()
        assertTrue(current.humidity in 0..100)
        assertTrue(current.pressure > 0)
        assertTrue(current.windSpeed >= 0)
    }

    @Test
    fun mapsHourlyForecast() {
        val hourly = load().toHourly()
        assertTrue(hourly.isNotEmpty())
        hourly.forEach { h ->
            assertTrue(h.precipProb in 0..100)
        }
    }

    @Test
    fun symbolToWeatherCodeCoversCommonCodes() {
        assertEquals(WeatherCode.CLEAR, symbolToWeatherCode("clearsky_day"))
        assertEquals(WeatherCode.CLEAR, symbolToWeatherCode("fair_night"))
        assertEquals(WeatherCode.PARTLY_CLOUDY, symbolToWeatherCode("partlycloudy_day"))
        assertEquals(WeatherCode.OVERCAST, symbolToWeatherCode("cloudy"))
        assertEquals(WeatherCode.FOG, symbolToWeatherCode("fog"))
        assertEquals(WeatherCode.RAIN_LIGHT, symbolToWeatherCode("lightrain"))
        assertEquals(WeatherCode.RAIN_LIGHT, symbolToWeatherCode("lightrainshowers_day"))
        assertEquals(WeatherCode.RAIN, symbolToWeatherCode("rain"))
        assertEquals(WeatherCode.RAIN, symbolToWeatherCode("rainshowers_night"))
        assertEquals(WeatherCode.RAIN_HEAVY, symbolToWeatherCode("heavyrain"))
        assertEquals(WeatherCode.RAIN_HEAVY, symbolToWeatherCode("heavyrainshowers_day"))
        assertEquals(WeatherCode.SNOW_LIGHT, symbolToWeatherCode("lightsnow"))
        assertEquals(WeatherCode.SNOW, symbolToWeatherCode("snow"))
        assertEquals(WeatherCode.SNOWFALL, symbolToWeatherCode("heavysnow"))
        assertEquals(WeatherCode.MIXED, symbolToWeatherCode("sleet"))
        assertEquals(WeatherCode.MIXED, symbolToWeatherCode("lightsleetshowers_day"))
        assertEquals(WeatherCode.THUNDERSTORM, symbolToWeatherCode("lightrainandthunder"))
        assertEquals(WeatherCode.THUNDERSTORM, symbolToWeatherCode("heavyrainandthunder"))
    }

    @Test
    fun apparentTemperatureIsReasonable() {
        // 20°C, 60% RH, 3 m/s wind → should be less than 20°C
        val at = apparentTemperature(20.0, 60.0, 3.0)
        assertTrue("AT should be below air temp in moderate conditions", at < 20)
        assertTrue("AT should not be wildly off", at > 10)
    }
}
