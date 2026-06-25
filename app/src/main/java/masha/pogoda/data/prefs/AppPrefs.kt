package masha.pogoda.data.prefs

import android.content.Context
import android.content.SharedPreferences
import masha.pogoda.BuildConfig

class AppPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    var lat: Double
        get() = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: DEFAULT_LAT
        set(value) = prefs.edit().putString(KEY_LAT, value.toString()).apply()

    var lon: Double
        get() = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: DEFAULT_LON
        set(value) = prefs.edit().putString(KEY_LON, value.toString()).apply()

    var city: String
        get() = prefs.getString(KEY_CITY, null).orEmpty().ifBlank { DEFAULT_CITY }
        set(value) = prefs.edit().putString(KEY_CITY, value).apply()

    var locationMode: LocationMode
        get() = runCatching {
            LocationMode.valueOf(prefs.getString(KEY_LOCATION_MODE, null) ?: LocationMode.GPS.name)
        }.getOrDefault(LocationMode.GPS)
        set(value) = prefs.edit().putString(KEY_LOCATION_MODE, value.name).apply()

    var userYandexKey: String?
        get() = prefs.getString(KEY_YANDEX_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_YANDEX_KEY, value?.takeIf { it.isNotBlank() }).apply()
        }

    val yandexKey: String?
        get() = userYandexKey ?: if (BuildConfig.DEBUG) {
            BuildConfig.YANDEX_DEV_KEY.takeIf { it.isNotBlank() }
        } else {
            null
        }

    enum class LocationMode {
        GPS,
        MANUAL
    }

    private companion object {
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_CITY = "city"
        const val KEY_LOCATION_MODE = "location_mode"
        const val KEY_YANDEX_KEY = "yandex_key"

        const val DEFAULT_LAT = 55.7558
        const val DEFAULT_LON = 37.6176
        const val DEFAULT_CITY = "Москва"
    }
}

