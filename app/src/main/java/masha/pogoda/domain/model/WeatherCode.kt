package masha.pogoda.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherCode {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    OVERCAST,
    FOG,
    RAIN_LIGHT,
    RAIN,
    RAIN_HEAVY,
    SNOW_LIGHT,
    SNOW,
    SNOWFALL,
    THUNDERSTORM,
    MIXED
}

