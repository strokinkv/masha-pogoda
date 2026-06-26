package masha.pogoda.domain.model

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HourlyForecastWindowTest {
    @Test
    fun nowInZoneUsesLocationZoneWhenValid() {
        val expected = LocalDateTime.now(ZoneId.of("Asia/Vladivostok")).hour
        assertEquals(expected, HourlyForecastWindow.nowInZone("Asia/Vladivostok").hour)
    }

    @Test
    fun nowInZoneFallsBackToDeviceZoneWhenNullOrInvalid() {
        val deviceHour = LocalDateTime.now().hour
        assertEquals(deviceHour, HourlyForecastWindow.nowInZone(null).hour)
        assertEquals(deviceHour, HourlyForecastWindow.nowInZone("Not/AZone").hour)
    }

    @Test
    fun takesNextHoursAfterCurrentHourAcrossDayBoundary() {
        val hourly = (0..27).map { offset ->
            val time = LocalDateTime.parse("2026-07-01T00:00").plusHours(offset.toLong())
            HourlyWeather(
                time = time.toString(),
                temperature = offset,
                code = WeatherCode.CLEAR,
                iconCode = "clear_day",
                precipProb = 0
            )
        }

        val result = HourlyForecastWindow.nextHours(
            hourly = hourly,
            now = LocalDateTime.parse("2026-07-01T22:15"),
            limit = 4
        )

        assertEquals(listOf("2026-07-01T23:00", "2026-07-02T00:00", "2026-07-02T01:00", "2026-07-02T02:00"), result.map { it.time })
    }
}
