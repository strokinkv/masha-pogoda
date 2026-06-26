package masha.pogoda.work

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherSyncPolicyTest {
    @Test
    fun usesTwoHourUniqueConnectedWork() {
        assertEquals("weather_hourly_sync", WeatherSyncPolicy.UNIQUE_WORK_NAME)
        assertEquals(120L, WeatherSyncPolicy.REPEAT_MINUTES)
    }
}
