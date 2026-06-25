package masha.pogoda.data.cache

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import masha.pogoda.domain.model.WeatherForecast

class WeatherCacheManager(dir: File) {
    private val cacheFile = File(dir, "forecast.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
    }

    fun save(forecast: WeatherForecast) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(json.encodeToString(forecast), Charsets.UTF_8)
    }

    fun load(): WeatherForecast? =
        runCatching {
            if (!cacheFile.exists()) return null
            json.decodeFromString<WeatherForecast>(cacheFile.readText(Charsets.UTF_8))
        }.getOrNull()

    fun isStale(forecast: WeatherForecast, now: Long): Boolean =
        now - forecast.cachedAt > TTL_MS

    private companion object {
        const val TTL_MS = 24 * 3600_000L
    }
}

