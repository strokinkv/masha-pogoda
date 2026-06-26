package masha.pogoda.data.mapper

import masha.pogoda.domain.model.WeatherCode

fun mapWmoCode(code: Int): WeatherCode = when (code) {
    0, 1 -> WeatherCode.CLEAR
    2 -> WeatherCode.PARTLY_CLOUDY
    3 -> WeatherCode.OVERCAST
    45, 48 -> WeatherCode.FOG
    51, 53, 80 -> WeatherCode.RAIN_LIGHT
    55, 61, 63, 81 -> WeatherCode.RAIN
    65, 82 -> WeatherCode.RAIN_HEAVY
    71, 73, 77 -> WeatherCode.SNOW_LIGHT
    75, 85, 86 -> WeatherCode.SNOW
    95, 96, 99 -> WeatherCode.THUNDERSTORM
    else -> WeatherCode.CLOUDY
}

fun weatherCodeToIcon(code: WeatherCode, isDay: Boolean): String {
    val suffix = if (isDay) "day" else "night"
    return when (code) {
        WeatherCode.CLEAR -> "clear_$suffix"
        WeatherCode.PARTLY_CLOUDY, WeatherCode.CLOUDY -> "cloudy_$suffix"
        WeatherCode.OVERCAST -> "overcast"
        WeatherCode.FOG -> "fog_$suffix"
        WeatherCode.RAIN_LIGHT -> "rain_light_$suffix"
        WeatherCode.RAIN, WeatherCode.RAIN_HEAVY -> "rain"
        WeatherCode.SNOW_LIGHT -> "snow_light_$suffix"
        WeatherCode.SNOW, WeatherCode.SNOWFALL -> "snow"
        WeatherCode.THUNDERSTORM -> "thunderstorm"
        WeatherCode.MIXED -> "sleet"
    }
}

fun WeatherCode.toRuDescription(): String = when (this) {
    WeatherCode.CLEAR -> "Ясно"
    WeatherCode.PARTLY_CLOUDY -> "Малооблачно"
    WeatherCode.CLOUDY -> "Облачно"
    WeatherCode.OVERCAST -> "Пасмурно"
    WeatherCode.FOG -> "Туман"
    WeatherCode.RAIN_LIGHT -> "Небольшой дождь"
    WeatherCode.RAIN -> "Дождь"
    WeatherCode.RAIN_HEAVY -> "Сильный дождь"
    WeatherCode.SNOW_LIGHT -> "Небольшой снег"
    WeatherCode.SNOW -> "Снег"
    WeatherCode.SNOWFALL -> "Снегопад"
    WeatherCode.THUNDERSTORM -> "Гроза"
    WeatherCode.MIXED -> "Мокрый снег"
}

fun weatherToAdvice(
    tempC: Int,
    code: WeatherCode,
    precipProb: Int,
    windMs: Double
): String = when {
    code in setOf(WeatherCode.RAIN, WeatherCode.RAIN_HEAVY, WeatherCode.THUNDERSTORM) || precipProb >= 60 ->
        "Возьми зонт ☔ — может пойти дождь"
    code in setOf(WeatherCode.SNOW, WeatherCode.SNOWFALL, WeatherCode.SNOW_LIGHT) ->
        "Снег! Надевай тёплую куртку и шапку ❄"
    tempC <= 0 -> "Очень холодно — тёплая куртка и шапка 🧣"
    tempC <= 10 -> "Прохладно — надень курточку 🧥"
    tempC > 25 -> "Жарко ☀ — лёгкая одежда и вода"
    windMs >= 10.0 -> "Ветрено — застегни куртку"
    else -> "Хорошая погода — можно гулять! 🙂"
}
