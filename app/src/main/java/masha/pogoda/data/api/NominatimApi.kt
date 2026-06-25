package masha.pogoda.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Query("accept-language") acceptLanguage: String = "ru"
    ): List<NominatimResult>
}

@Serializable
data class NominatimResult(
    @SerialName("display_name") val displayName: String,
    val lat: String,
    val lon: String
)

