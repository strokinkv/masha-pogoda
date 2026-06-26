package masha.pogoda.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface MetNoApi {
    @GET("weatherapi/locationforecast/2.0/compact")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): MetNoResponse
}

@Serializable
data class MetNoResponse(
    val properties: MetNoProperties
)

@Serializable
data class MetNoProperties(
    val meta: MetNoMeta,
    val timeseries: List<MetNoTimeseries>
)

@Serializable
data class MetNoMeta(
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class MetNoTimeseries(
    val time: String,
    val data: MetNoTimeseriesData
)

@Serializable
data class MetNoTimeseriesData(
    val instant: MetNoInstant,
    @SerialName("next_1_hours") val next1Hours: MetNoNextHours? = null,
    @SerialName("next_6_hours") val next6Hours: MetNoNextHours? = null,
    @SerialName("next_12_hours") val next12Hours: MetNoNextHours? = null
)

@Serializable
data class MetNoInstant(
    val details: MetNoInstantDetails
)

@Serializable
data class MetNoInstantDetails(
    @SerialName("air_temperature") val airTemperature: Double,
    @SerialName("relative_humidity") val relativeHumidity: Double,
    @SerialName("wind_speed") val windSpeed: Double,
    @SerialName("wind_from_direction") val windFromDirection: Double,
    @SerialName("air_pressure_at_sea_level") val airPressure: Double? = null,
    @SerialName("cloud_area_fraction") val cloudAreaFraction: Double? = null
)

@Serializable
data class MetNoNextHours(
    val summary: MetNoSummary? = null,
    val details: MetNoNextDetails? = null
)

@Serializable
data class MetNoSummary(
    @SerialName("symbol_code") val symbolCode: String
)

@Serializable
data class MetNoNextDetails(
    @SerialName("precipitation_amount") val precipitationAmount: Double? = null
)
