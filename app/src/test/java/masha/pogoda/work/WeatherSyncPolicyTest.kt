package masha.pogoda.work

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherSyncPolicyTest {
    @Test
    fun usesHourlyUniqueConnectedWork() {
        assertEquals("weather_hourly_sync", WeatherSyncPolicy.UNIQUE_WORK_NAME)
        assertEquals(60L, WeatherSyncPolicy.REPEAT_MINUTES)
    }
}
