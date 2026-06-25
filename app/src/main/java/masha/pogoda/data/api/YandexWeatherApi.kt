package masha.pogoda.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface YandexWeatherApi {
    @POST("graphql/query")
    suspend fun query(@Body body: GraphQlRequest): YandexResponse
}

@Serializable
data class GraphQlRequest(val query: String)

@Serializable
data class YandexResponse(val data: YandexData)

@Serializable
data class YandexData(val weatherByPoint: YandexWeatherByPoint)

@Serializable
data class YandexWeatherByPoint(
    val now: YandexNow,
    val forecast: YandexForecast
)

@Serializable
data class YandexNow(
    val temperature: Int,
    val feelsLike: Int,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windAngle: Int,
    val windDirection: String,
    val condition: String,
    val icon: String
)

@Serializable
data class YandexForecast(val days: List<YandexForecastDay>)

@Serializable
data class YandexForecastDay(
    val time: String,
    val sunriseTime: String? = null,
    val sunsetTime: String? = null,
    val parts: YandexDayParts,
    val hours: List<YandexForecastHour> = emptyList()
)

@Serializable
data class YandexDayParts(val day: YandexDaypart)

@Serializable
data class YandexDaypart(
    val minTemperature: Int,
    val maxTemperature: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: String,
    val condition: String,
    val precProbability: Double? = null,
    val precType: String? = null,
    val icon: String
)

@Serializable
data class YandexForecastHour(
    val time: String,
    val temperature: Int,
    val condition: String,
    val precProbability: Double? = null,
    val icon: String
)

