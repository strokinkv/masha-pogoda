package masha.pogoda.widget

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import masha.pogoda.domain.model.HourlyForecastWindow
import masha.pogoda.domain.model.WeatherForecast

data class WeatherWidgetEntry(
    val label: String,
    val iconCode: String,
    val temperature: String
)

data class WeatherWidgetModel(
    val city: String,
    val iconCode: String,
    val temperature: String,
    val feelsLike: Int,
    val todayMin: Int,
    val todayMax: Int,
    val windSpeed: Double,
    val updatedAt: String,
    val entries: List<WeatherWidgetEntry>
) {
    companion object {
        fun from(
            forecast: WeatherForecast,
            showHourly: Boolean = false,
            now: LocalDateTime? = null
        ): WeatherWidgetModel {
            val current = forecast.current
            val today = forecast.daily.firstOrNull()
            val effectiveNow = now ?: HourlyForecastWindow.nowInZone(forecast.timezone)
            return WeatherWidgetModel(
                city = forecast.city,
                iconCode = current.iconCode,
                temperature = "${current.temperature}°",
                feelsLike = current.feelsLike,
                todayMin = today?.tempMin ?: current.temperature,
                todayMax = today?.tempMax ?: current.temperature,
                windSpeed = current.windSpeed,
                updatedAt = forecast.cachedAt.formatTime(),
                entries = if (showHourly && forecast.hourly.isNotEmpty()) {
                    HourlyForecastWindow.nextHours(forecast.hourly, effectiveNow, limit = 5).map { hour ->
                        WeatherWidgetEntry(
                            label = hour.time.formatHourLabel(),
                            iconCode = hour.iconCode,
                            temperature = "${hour.temperature}°"
                        )
                    }
                } else {
                    forecast.daily.take(5).map { day ->
                        WeatherWidgetEntry(
                            label = day.date.formatDateLabel(),
                            iconCode = day.iconCode,
                            temperature = "${day.tempMin}…${day.tempMax}°"
                        )
                    }
                }
            )
        }

        private val HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))

        private fun String.formatHourLabel(): String =
            try {
                LocalDateTime.parse(this).format(HOUR_FORMATTER)
            } catch (_: DateTimeParseException) {
                substringAfter("T", this).take(5)
            }

        private fun String.formatDateLabel(): String =
            try {
                LocalDate.parse(this).format(DATE_FORMATTER)
            } catch (_: DateTimeParseException) {
                this
            }

        private fun Long.formatTime(): String =
            HOUR_FORMATTER.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
    }
}
