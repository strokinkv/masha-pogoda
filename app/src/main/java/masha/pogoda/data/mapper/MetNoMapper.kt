package masha.pogoda.data.mapper

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import masha.pogoda.data.api.MetNoResponse
import masha.pogoda.data.api.MetNoTimeseries
import masha.pogoda.domain.model.CurrentWeather
import masha.pogoda.domain.model.DailyWeather
import masha.pogoda.domain.model.HourlyWeather
import masha.pogoda.domain.model.WeatherCode

private val LOCAL_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val LOCAL_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

fun MetNoResponse.toCurrent(): CurrentWeather {
    val first = properties.timeseries.first()
    val d = first.data.instant.details
    val symbol = first.data.next1Hours?.summary?.symbolCode
        ?: first.data.next6Hours?.summary?.symbolCode
        ?: first.data.next12Hours?.summary?.symbolCode
        ?: "cloudy"
    val localHour = first.time.utcToLocalDateTime().hour
    val code = symbolToWeatherCode(symbol)
    return CurrentWeather(
        temperature = d.airTemperature.roundToInt(),
        feelsLike = apparentTemperature(d.airTemperature, d.relativeHumidity, d.windSpeed),
        humidity = d.relativeHumidity.roundToInt(),
        windSpeed = d.windSpeed,
        windDirection = degreesToRu(d.windFromDirection.roundToInt()),
        pressure = d.airPressure?.roundToInt() ?: 0,
        code = code,
        iconCode = weatherCodeToIcon(code, symbolIsDay(symbol, localHour)),
        description = code.toRuDescription(),
        updatedAt = Instant.parse(properties.meta.updatedAt).toEpochMilli()
    )
}

fun MetNoResponse.toHourly(): List<HourlyWeather> =
    properties.timeseries
        .filter { it.data.next1Hours?.summary != null }
        .map { entry ->
            val d = entry.data.instant.details
            val symbol = entry.data.next1Hours!!.summary!!.symbolCode
            val localDt = entry.time.utcToLocalDateTime()
            val code = symbolToWeatherCode(symbol)
            HourlyWeather(
                time = localDt.format(LOCAL_DT_FMT),
                temperature = d.airTemperature.roundToInt(),
                code = code,
                iconCode = weatherCodeToIcon(code, symbolIsDay(symbol, localDt.hour)),
                precipProb = maxOf(
                    symbolToPrecipProb(symbol),
                    precipProbFromAmount(entry.data.next1Hours.details?.precipitationAmount ?: 0.0)
                )
            )
        }

fun MetNoResponse.toDaily(): List<DailyWeather> {
    val zone = ZoneId.systemDefault()
    return properties.timeseries
        .groupBy { entry -> Instant.parse(entry.time).atZone(zone).toLocalDate() }
        .entries
        .sortedBy { it.key }
        .take(7)
        .map { (date, entries) -> buildDailyWeather(date, entries) }
}

private fun buildDailyWeather(date: LocalDate, entries: List<MetNoTimeseries>): DailyWeather {
    val temps = entries.map { it.data.instant.details.airTemperature }
    val zone = ZoneId.systemDefault()

    val noonEntry = entries.minByOrNull { entry ->
        abs(Instant.parse(entry.time).atZone(zone).hour - 12)
    } ?: entries.first()

    val symbol = noonEntry.data.next12Hours?.summary?.symbolCode
        ?: noonEntry.data.next6Hours?.summary?.symbolCode
        ?: noonEntry.data.next1Hours?.summary?.symbolCode
        ?: "cloudy"

    val nd = noonEntry.data.instant.details
    val code = symbolToWeatherCode(symbol)

    val maxWindEntry = entries.maxByOrNull { it.data.instant.details.windSpeed } ?: entries.first()
    val avgHumidity = entries.map { it.data.instant.details.relativeHumidity }.average()

    val maxPrecipProb = entries.maxOf { entry ->
        val sym = entry.data.next1Hours?.summary?.symbolCode
            ?: entry.data.next6Hours?.summary?.symbolCode ?: ""
        val amount = entry.data.next1Hours?.details?.precipitationAmount
            ?: entry.data.next6Hours?.details?.precipitationAmount ?: 0.0
        maxOf(symbolToPrecipProb(sym), precipProbFromAmount(amount))
    }

    return DailyWeather(
        date = date.format(LOCAL_DATE_FMT),
        tempMin = temps.min().roundToInt(),
        tempMax = temps.max().roundToInt(),
        feelsLike = apparentTemperature(nd.airTemperature, nd.relativeHumidity, nd.windSpeed),
        humidity = avgHumidity.roundToInt(),
        windSpeed = maxWindEntry.data.instant.details.windSpeed,
        windDirection = degreesToRu(maxWindEntry.data.instant.details.windFromDirection.roundToInt()),
        precipProb = maxPrecipProb,
        code = code,
        iconCode = weatherCodeToIcon(code, isDay = true),
        sunrise = null,
        sunset = null
    )
}

internal fun symbolToWeatherCode(symbolCode: String): WeatherCode = when {
    "thunder" in symbolCode -> WeatherCode.THUNDERSTORM
    "heavyrain" in symbolCode -> WeatherCode.RAIN_HEAVY
    "lightrain" in symbolCode -> WeatherCode.RAIN_LIGHT
    "rain" in symbolCode -> WeatherCode.RAIN
    "heavysnow" in symbolCode -> WeatherCode.SNOWFALL
    "lightsnow" in symbolCode -> WeatherCode.SNOW_LIGHT
    "snow" in symbolCode -> WeatherCode.SNOW
    "sleet" in symbolCode -> WeatherCode.MIXED
    "fog" in symbolCode -> WeatherCode.FOG
    symbolCode.startsWith("cloudy") -> WeatherCode.OVERCAST
    "partlycloudy" in symbolCode -> WeatherCode.PARTLY_CLOUDY
    symbolCode.startsWith("fair") || symbolCode.startsWith("clearsky") -> WeatherCode.CLEAR
    else -> WeatherCode.CLOUDY
}

internal fun symbolIsDay(symbolCode: String, localHour: Int): Boolean = when {
    "_day" in symbolCode -> true
    "_night" in symbolCode -> false
    else -> localHour in 6..20
}

internal fun symbolToPrecipProb(symbolCode: String): Int = when {
    "thunder" in symbolCode -> 90
    "heavyrain" in symbolCode || "heavysnow" in symbolCode || "heavysleet" in symbolCode -> 95
    "lightrain" in symbolCode || "lightsnow" in symbolCode || "lightsleet" in symbolCode -> 65
    "rain" in symbolCode || "snow" in symbolCode || "sleet" in symbolCode -> 80
    "fog" in symbolCode || "cloudy" in symbolCode -> 10
    "partlycloudy" in symbolCode -> 5
    else -> 0
}

internal fun precipProbFromAmount(amountMm: Double): Int = when {
    amountMm >= 5.0 -> 90
    amountMm >= 2.0 -> 75
    amountMm >= 0.5 -> 60
    amountMm > 0.0 -> 40
    else -> 0
}

internal fun apparentTemperature(tempC: Double, relativeHumidity: Double, windSpeedMs: Double): Int {
    // Steadman AT = T + 0.33*e - 0.70*ws - 4.00, e = vapour pressure (hPa)
    val e = relativeHumidity / 100.0 * 6.105 * exp(17.27 * tempC / (237.7 + tempC))
    return (tempC + 0.33 * e - 0.70 * windSpeedMs - 4.00).roundToInt()
}

private fun String.utcToLocalDateTime(): LocalDateTime =
    Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
