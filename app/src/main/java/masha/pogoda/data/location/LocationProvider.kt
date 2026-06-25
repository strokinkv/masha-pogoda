package masha.pogoda.data.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import masha.pogoda.data.api.NominatimApi
import masha.pogoda.data.prefs.AppPrefs
import kotlinx.coroutines.suspendCancellableCoroutine

data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val displayName: String
)

class LocationProvider(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val nominatimApi: NominatimApi,
    private val prefs: AppPrefs
) {
    @SuppressLint("MissingPermission")
    suspend fun current(): Pair<Double, Double>? =
        suspendCancellableCoroutine { continuation ->
            val task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            )

            task.addOnSuccessListener { location ->
                continuation.resume(location?.let { it.latitude to it.longitude })
            }
            task.addOnFailureListener {
                continuation.resume(null)
            }
            task.addOnCanceledListener {
                continuation.resume(null)
            }
        }

    suspend fun geocode(city: String): GeoPoint? =
        runCatching {
            val result = nominatimApi.search(city).firstOrNull() ?: return null
            val lat = result.lat.toDoubleOrNull() ?: return null
            val lon = result.lon.toDoubleOrNull() ?: return null
            val point = GeoPoint(lat = lat, lon = lon, displayName = result.displayName)

            prefs.lat = point.lat
            prefs.lon = point.lon
            prefs.city = city

            point
        }.getOrNull()
}

