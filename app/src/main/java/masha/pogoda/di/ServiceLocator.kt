package masha.pogoda.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import masha.pogoda.data.api.NominatimApi
import masha.pogoda.data.api.OpenMeteoApi
import masha.pogoda.data.api.YandexWeatherApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object ServiceLocator {
    var yandexKeyProvider: () -> String? = { null }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val jsonConverterFactory = json.asConverterFactory("application/json".toMediaType())

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                when (original.url.host) {
                    "api.weather.yandex.ru" -> {
                        yandexKeyProvider()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { builder.header("X-Yandex-Weather-Key", it) }
                    }

                    "nominatim.openstreetmap.org" -> {
                        builder.header("User-Agent", "WeatherApp/1.0")
                    }
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    val yandexApi: YandexWeatherApi by lazy {
        retrofit("https://api.weather.yandex.ru/").create(YandexWeatherApi::class.java)
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        retrofit("https://api.open-meteo.com/").create(OpenMeteoApi::class.java)
    }

    val nominatimApi: NominatimApi by lazy {
        retrofit("https://nominatim.openstreetmap.org/").create(NominatimApi::class.java)
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(jsonConverterFactory)
            .build()
}

