package masha.pogoda.data.repository

import masha.pogoda.data.api.OpenMeteoApi
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
    private val openMeteoApi: OpenMeteoApi,
    private val cache: WeatherCacheManager
) {
    /**
     * Кэшированный прогноз для мгновенного показа (cache-first), если он есть.
     * `stale` отражает, истёк ли TTL — на это завязан баннер «показаны сохранённые данные».
     */
    fun cached(now: Long = System.currentTimeMillis()): WeatherResult.Success? {
        val cached = cache.load() ?: return null
        return WeatherResult.Success(cached, fromCache = true, stale = cache.isStale(cached, now))
    }

    suspend fun refresh(lat: Double, lon: Double, city: String): WeatherResult =
        runCatching {
            val openMeteo = openMeteoApi.getForecast(lat, lon)
            val forecast = WeatherForecast(
                city = city,
                current = openMeteo.toCurrent(),
                hourly = openMeteo.toHourly(),
                daily = openMeteo.toDaily(),
                cachedAt = System.currentTimeMillis(),
                timezone = openMeteo.timezone
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
