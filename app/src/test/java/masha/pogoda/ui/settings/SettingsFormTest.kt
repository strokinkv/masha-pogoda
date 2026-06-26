package masha.pogoda.ui.settings

import masha.pogoda.data.prefs.AppPrefs.LocationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFormTest {
    @Test
    fun normalizesBlankKeyAndCity() {
        val snapshot = SettingsSnapshot(
            city = "Москва",
            locationMode = LocationMode.GPS,
            userYandexKey = "old"
        )

        val result = SettingsForm.normalize(
            current = snapshot,
            cityInput = "  Казань  ",
            locationMode = LocationMode.MANUAL,
            yandexKeyInput = "   "
        )

        assertEquals("Казань", result.snapshot.city)
        assertEquals(LocationMode.MANUAL, result.snapshot.locationMode)
        assertNull(result.snapshot.userYandexKey)
        assertTrue(result.changed)
    }
}
