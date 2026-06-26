package masha.pogoda.data.repository

import masha.pogoda.data.api.MetNoApi
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.data.mapper.toCurrent
import masha.pogoda.data.mapper.toDaily
import masha.pogoda.data.mapper.toHourly
import masha.pogoda.domain.model.WeatherForecast

sealed class WeatherResult {
    data class Success(
        val forecast: WeatherForecast,
        val fromCache: Boolean,
        val stale: Boolean
    ) : WeatherResult()

    data class Error(
        val message: String,
        val cached: WeatherForecast?
    ) : WeatherResult()
}

class WeatherRepository(
    private val metNoApi: MetNoApi,
    private val cache: WeatherCacheManager
) {
    fun cached(now: Long = System.currentTimeMillis()): WeatherResult.Success? {
        val cached = cache.load() ?: return null
        return WeatherResult.Success(cached, fromCache = true, stale = cache.isStale(cached, now))
    }

    suspend fun refresh(lat: Double, lon: Double, city: String): WeatherResult =
        runCatching {
            val response = metNoApi.getForecast(lat, lon)
            val forecast = WeatherForecast(
                city = city,
                current = response.toCurrent(),
                hourly = response.toHourly(),
                daily = response.toDaily(),
                cachedAt = System.currentTimeMillis(),
                timezone = null
            )
            cache.save(forecast)
            WeatherResult.Success(forecast, fromCache = false, stale = false)
        }.getOrElse { cacheFallback("Не удалось обновить прогноз") }

    private fun cacheFallback(message: String): WeatherResult {
        val cached = cache.load()
        return if (cached != null && !cache.isStale(cached, now = System.currentTimeMillis())) {
            WeatherResult.Success(cached, fromCache = true, stale = false)
        } else {
            WeatherResult.Error(message, cached)
        }
    }
}
