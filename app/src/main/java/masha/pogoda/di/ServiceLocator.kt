package masha.pogoda.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import masha.pogoda.data.api.NominatimApi
import masha.pogoda.data.api.OpenMeteoApi
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.data.location.LocationProvider
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.data.repository.WeatherRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object ServiceLocator {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val jsonConverterFactory = json.asConverterFactory("application/json".toMediaType())

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                if (original.url.host == "nominatim.openstreetmap.org") {
                    builder.header("User-Agent", "WeatherApp/1.0")
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        retrofit("https://api.open-meteo.com/").create(OpenMeteoApi::class.java)
    }

    val nominatimApi: NominatimApi by lazy {
        retrofit("https://nominatim.openstreetmap.org/").create(NominatimApi::class.java)
    }

    fun weatherRepository(context: Context): WeatherRepository =
        WeatherRepository(
            openMeteoApi = openMeteoApi,
            cache = WeatherCacheManager(File(context.applicationContext.filesDir, "weather_cache"))
        )

    fun locationProvider(context: Context): LocationProvider =
        LocationProvider(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context),
            nominatimApi = nominatimApi,
            prefs = AppPrefs(context.applicationContext)
        )

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(jsonConverterFactory)
            .build()
}
