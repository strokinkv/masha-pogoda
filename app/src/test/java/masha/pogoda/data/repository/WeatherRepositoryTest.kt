package masha.pogoda.data.repository

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import masha.pogoda.data.api.MetNoApi
import masha.pogoda.data.api.MetNoResponse
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.WeatherCode
import masha.pogoda.domain.model.WeatherForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun refresh_usesMetNo() = runBlocking {
        val api = FakeMetNoApi(metNoFixture())
        val repository = WeatherRepository(
            metNoApi = api,
            cache = WeatherCacheManager(tempDir())
        )

        val result = repository.refresh(55.7558, 37.6176, "Москва")

        assertTrue(result is WeatherResult.Success)
        assertEquals(1, api.calls)
        assertEquals(7, (result as WeatherResult.Success).forecast.daily.size)
    }

    @Test
    fun cachedReturnsFreshSuccessWithinTtl() {
        val cache = WeatherCacheManager(tempDir())
        cache.save(sampleForecast(cachedAt = System.currentTimeMillis()))
        val repository = repositoryWith(cache)

        val cached = repository.cached()

        assertTrue(cached is WeatherResult.Success)
        cached as WeatherResult.Success
        assertTrue(cached.fromCache)
        assertFalse(cached.stale)
    }

    @Test
    fun cachedMarksStaleWhenTtlExpired() {
        val cache = WeatherCacheManager(tempDir())
        val expired = System.currentTimeMillis() - 25L * 60 * 60 * 1000
        cache.save(sampleForecast(cachedAt = expired))
        val repository = repositoryWith(cache)

        val cached = repository.cached()

        assertTrue(cached is WeatherResult.Success)
        assertTrue((cached as WeatherResult.Success).stale)
    }

    @Test
    fun cachedReturnsNullWhenEmpty() {
        val repository = repositoryWith(WeatherCacheManager(tempDir()))
        assertEquals(null, repository.cached())
    }

    @Test
    fun networkFailureWithFreshCacheReturnsCachedSuccess() = runBlocking {
        val cache = WeatherCacheManager(tempDir())
        cache.save(sampleForecast(cachedAt = System.currentTimeMillis()))
        val repository = WeatherRepository(
            metNoApi = FakeMetNoApi(metNoFixture(), fail = true),
            cache = cache
        )

        val result = repository.refresh(55.7558, 37.6176, "Москва")

        assertTrue(result is WeatherResult.Success)
        assertTrue((result as WeatherResult.Success).fromCache)
    }

    @Test
    fun networkFailureWithStaleCacheReturnsError() = runBlocking {
        val cache = WeatherCacheManager(tempDir())
        cache.save(sampleForecast(cachedAt = System.currentTimeMillis() - 25L * 60 * 60 * 1000))
        val repository = WeatherRepository(
            metNoApi = FakeMetNoApi(metNoFixture(), fail = true),
            cache = cache
        )

        val result = repository.refresh(55.7558, 37.6176, "Москва")

        assertTrue(result is WeatherResult.Error)
        assertTrue((result as WeatherResult.Error).cached != null)
    }

    @Test
    fun networkFailureWithoutCacheReturnsError() = runBlocking {
        val repository = WeatherRepository(
            metNoApi = FakeMetNoApi(metNoFixture(), fail = true),
            cache = WeatherCacheManager(tempDir())
        )

        val result = repository.refresh(55.7558, 37.6176, "Москва")

        assertTrue(result is WeatherResult.Error)
        assertEquals(null, (result as WeatherResult.Error).cached)
    }

    private fun repositoryWith(cache: WeatherCacheManager): WeatherRepository =
        WeatherRepository(
            metNoApi = FakeMetNoApi(metNoFixture()),
            cache = cache
        )

    private fun sampleForecast(cachedAt: Long): WeatherForecast =
        WeatherForecast(
            city = "Москва",
            current = CurrentWeather(
                temperature = 20,
                feelsLike = 19,
                humidity = 50,
                windSpeed = 3.0,
                windDirection = "С",
                pressure = 750,
                code = WeatherCode.CLEAR,
                iconCode = "clear_day",
                description = "Ясно",
                updatedAt = cachedAt
            ),
            hourly = emptyList(),
            daily = emptyList(),
            cachedAt = cachedAt
        )

    private fun metNoFixture(): MetNoResponse =
        json.decodeFromString(
            javaClass.getResource("/fixtures/met_no.json")!!.readText()
        )

    private fun tempDir(): File =
        kotlin.io.path.createTempDirectory("weather-repository-test").toFile()

    private class FakeMetNoApi(
        private val response: MetNoResponse,
        private val fail: Boolean = false
    ) : MetNoApi {
        var calls = 0

        override suspend fun getForecast(lat: Double, lon: Double): MetNoResponse {
            calls++
            if (fail) error("met.no failed")
            return response
        }
    }
}
