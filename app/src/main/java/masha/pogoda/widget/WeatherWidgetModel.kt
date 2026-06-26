package masha.pogoda.widget

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import masha.pogoda.domain.model.WeatherForecast

data class WeatherWidgetDay(
    val dateLabel: String,
    val iconCode: String,
    val temperatureRange: String
)

data class WeatherWidgetModel(
    val city: String,
    val iconCode: String,
    val temperature: String,
    val details: String,
    val updatedAt: String,
    val days: List<WeatherWidgetDay>
) {
    companion object {
        fun from(forecast: WeatherForecast): WeatherWidgetModel {
            val current = forecast.current
            return WeatherWidgetModel(
                city = forecast.city,
                iconCode = current.iconCode,
                temperature = "${current.temperature}°",
                details = "Ощущ. ${current.feelsLike}°  Вл. ${current.humidity}%  Ветер ${current.windSpeed} м/с",
                updatedAt = forecast.cachedAt.formatTime(),
                days = forecast.daily.take(3).map { day ->
                    WeatherWidgetDay(
                        dateLabel = day.date.formatDateLabel(),
                        iconCode = day.iconCode,
                        temperatureRange = "${day.tempMin}…${day.tempMax}°"
                    )
                }
            )
        }

        private fun String.formatDateLabel(): String =
            try {
                LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd.MM", Locale("ru")))
            } catch (_: DateTimeParseException) {
                this
            }

        private fun Long.formatTime(): String =
            java.text.SimpleDateFormat("HH:mm", Locale("ru")).format(java.util.Date(this))
    }
}
