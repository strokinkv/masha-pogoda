package masha.pogoda.data.repository

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import masha.pogoda.data.api.GraphQlRequest
import masha.pogoda.data.api.OpenMeteoApi
import masha.pogoda.data.api.OpenMeteoResponse
import masha.pogoda.data.api.YandexResponse
import masha.pogoda.data.api.YandexWeatherApi
import masha.pogoda.data.cache.WeatherCacheManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun noYandexKey_usesOpenMeteoOnly() = runBlocking {
        val openMeteo = FakeOpenMeteoApi(openMeteoFixture())
        val yandex = FakeYandexApi(yandexFixture())
        val repository = WeatherRepository(
            openMeteoApi = openMeteo,
            yandexApi = yandex,
            cache = WeatherCacheManager(tempDir()),
            yandexKeyProvider = { null }
        )

        val result = repository.refresh(55.7558, 37.6176, "Москва")

        assertTrue(result is WeatherResult.Success)
        assertEquals(1, openMeteo.calls)
        assertEquals(0, yandex.calls)
        assertEquals(7, (result as WeatherResult.Success).forecast.daily.size)
    }

    @Test
    fun yandexFailure_degradesToOpenMeteo() = runBlocking {
        val openMeteo = FakeOpenMeteoApi(openMeteoFixture())
        val yandex = FakeYandexApi(yandexFixture(), fail = true)
        val repository = WeatherRepository(
            openMeteoApi = openMeteo,
            yandexApi = yandex,
            cache = WeatherCacheManager(tempDir()),
            yandexKeyProvider = { "key" }
        )

        val result = repository.refresh(55.7558, 37.6176, "Москва")

        assertTrue(result is WeatherResult.Success)
        assertEquals(1, yandex.calls)
        assertEquals(1, openMeteo.calls)
        assertEquals(7, (result as WeatherResult.Success).forecast.daily.size)
    }

    private fun openMeteoFixture(): OpenMeteoResponse =
        json.decodeFromString(
            javaClass.getResource("/fixtures/open_meteo.json")!!.readText()
        )

    private fun yandexFixture(): YandexResponse =
        json.decodeFromString(
            javaClass.getResource("/fixtures/yandex.json")!!.readText()
        )

    private fun tempDir(): File =
        kotlin.io.path.createTempDirectory("weather-repository-test").toFile()

    private class FakeOpenMeteoApi(
        private val response: OpenMeteoResponse,
        private val fail: Boolean = false
    ) : OpenMeteoApi {
        var calls = 0

        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            windSpeedUnit: String,
            timezone: String,
            forecastDays: Int
        ): OpenMeteoResponse {
            calls++
            if (fail) error("Open-Meteo failed")
            return response
        }
    }

    private class FakeYandexApi(
        private val response: YandexResponse,
        private val fail: Boolean = false
    ) : YandexWeatherApi {
        var calls = 0

        override suspend fun query(body: GraphQlRequest): YandexResponse {
            calls++
            if (fail) error("Yandex failed")
            return response
        }
    }
}

