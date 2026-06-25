package masha.pogoda.data.mapper

import kotlinx.serialization.json.Json
import masha.pogoda.data.api.YandexResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YandexMapperTest {
    private fun load() = Json { ignoreUnknownKeys = true }.decodeFromString<YandexResponse>(
        javaClass.getResource("/fixtures/yandex.json")!!.readText()
    )

    @Test
    fun current() {
        val current = load().toCurrent()
        assertTrue(current.humidity in 0..100)
        assertTrue(current.windDirection.isNotEmpty())
    }

    @Test
    fun head() {
        assertEquals(2, load().toDailyHead().size)
    }
}

