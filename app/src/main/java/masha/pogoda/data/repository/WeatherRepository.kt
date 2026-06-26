package masha.pogoda.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import masha.pogoda.data.api.GraphQlRequest
import masha.pogoda.data.api.OpenMeteoApi
import masha.pogoda.data.api.YandexWeatherApi
import masha.pogoda.data.api.buildYandexWeatherQuery
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.data.mapper.toCurrent
import masha.pogoda.data.mapper.toDaily
import masha.pogoda.data.mapper.toDailyHead
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
    private val yandexApi: YandexWeatherApi,
    private val cache: WeatherCacheManager,
    private val yandexKeyProvider: () -> String?
) {
    /**
     * Кэшированный прогноз для мгновенного показа (cache-first), если он есть.
     * `stale` отражает, истёк ли TTL — на это завязан баннер «показаны сохранённые данные».
     */
    fun cached(now: Long = System.currentTimeMillis()): WeatherResult.Success? {
        val cached = cache.load() ?: return null
        return WeatherResult.Success(cached, fromCache = true, stale = cache.isStale(cached, now))
    }

    suspend fun refresh(lat: Double, lon: Double, city: String): WeatherResult {
        val key = yandexKeyProvider()?.takeIf { it.isNotBlank() }
        return if (key == null) {
            refreshOpenMeteoOnly(lat, lon, city)
        } else {
            refreshWithYandex(lat, lon, city)
        }
    }

    private suspend fun refreshOpenMeteoOnly(
        lat: Double,
        lon: Double,
        city: String
    ): WeatherResult =
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

    private suspend fun refreshWithYandex(
        lat: Double,
        lon: Double,
        city: String
    ): WeatherResult =
        supervisorScope {
            val openMeteoDeferred = async { openMeteoApi.getForecast(lat, lon) }
            val yandexDeferred = async {
                yandexApi.query(GraphQlRequest(buildYandexWeatherQuery(lat, lon)))
            }

            val openMeteoResult = runCatching { openMeteoDeferred.await() }
            val yandexResult = runCatching { yandexDeferred.await() }

            val openMeteo = openMeteoResult.getOrNull()
            val yandex = yandexResult.getOrNull()

            when {
                yandex != null && openMeteo != null -> {
                    val forecast = merge(
                        city = city,
                        current = yandex.toCurrent(),
                        hourly = yandex.toHourly(),
                        yandexDays = yandex.toDailyHead(),
                        openMeteoDays = openMeteo.toDaily(),
                        timezone = openMeteo.timezone
                    )
                    cache.save(forecast)
                    WeatherResult.Success(forecast, fromCache = false, stale = false)
                }

                openMeteo != null -> {
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
                }

                else -> cacheFallback("Не удалось обновить прогноз")
            }
        }

    private fun cacheFallback(message: String): WeatherResult {
        val cached = cache.load()
        return if (cached != null && !cache.isStale(cached, now = System.currentTimeMillis())) {
            WeatherResult.Success(cached, fromCache = true, stale = false)
        } else {
            WeatherResult.Error(message, cached)
        }
    }
}
