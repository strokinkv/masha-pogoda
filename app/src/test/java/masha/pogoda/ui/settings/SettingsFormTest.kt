package masha.pogoda.ui.settings

import masha.pogoda.data.prefs.AppPrefs.LocationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFormTest {
    @Test
    fun normalizesBlankCity() {
        val snapshot = SettingsSnapshot(
            city = "Москва",
            locationMode = LocationMode.GPS,
            widgetHourlyForecast = false
        )

        val result = SettingsForm.normalize(
            current = snapshot,
            cityInput = "  Казань  ",
            locationMode = LocationMode.MANUAL,
            widgetHourlyForecast = true
        )

        assertEquals("Казань", result.snapshot.city)
        assertEquals(LocationMode.MANUAL, result.snapshot.locationMode)
        assertTrue(result.snapshot.widgetHourlyForecast)
        assertTrue(result.changed)
    }
}
