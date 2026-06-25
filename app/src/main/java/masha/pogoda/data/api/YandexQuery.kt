package masha.pogoda.data.api

import java.util.Locale

private const val YANDEX_WEATHER_QUERY_TEMPLATE = """
{
  weatherByPoint(request: { lat: %.6f, lon: %.6f }) {
    now {
      temperature
      feelsLike
      humidity
      pressure
      windSpeed
      windAngle
      windDirection
      condition
      icon(format: CODE)
    }
    forecast {
      days(limit: 2) {
        time
        sunriseTime
        sunsetTime
        parts {
          day {
            minTemperature
            maxTemperature
            humidity
            windSpeed
            windDirection
            condition
            precProbability
            precType
            icon(format: CODE)
          }
        }
        hours {
          time
          temperature
          condition
          precProbability
          icon(format: CODE)
        }
      }
    }
  }
}
"""

fun buildYandexWeatherQuery(lat: Double, lon: Double): String =
    String.format(Locale.US, YANDEX_WEATHER_QUERY_TEMPLATE, lat, lon)

