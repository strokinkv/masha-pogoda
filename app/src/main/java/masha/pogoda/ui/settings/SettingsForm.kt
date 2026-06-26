package masha.pogoda.ui.settings

import masha.pogoda.data.prefs.AppPrefs.LocationMode

data class SettingsSnapshot(
    val city: String,
    val locationMode: LocationMode,
    val userYandexKey: String?,
    val widgetHourlyForecast: Boolean
)

data class SettingsFormResult(
    val snapshot: SettingsSnapshot,
    val changed: Boolean
)

object SettingsForm {
    fun normalize(
        current: SettingsSnapshot,
        cityInput: String,
        locationMode: LocationMode,
        yandexKeyInput: String,
        widgetHourlyForecast: Boolean
    ): SettingsFormResult {
        val normalized = SettingsSnapshot(
            city = cityInput.trim().ifBlank { current.city },
            locationMode = locationMode,
            userYandexKey = yandexKeyInput.trim().takeIf { it.isNotBlank() },
            widgetHourlyForecast = widgetHourlyForecast
        )
        return SettingsFormResult(
            snapshot = normalized,
            changed = normalized != current
        )
    }
}
