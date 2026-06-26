package masha.pogoda.domain.model

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

object HourlyForecastWindow {
    fun nextHours(
        hourly: List<HourlyWeather>,
        now: LocalDateTime = LocalDateTime.now(),
        limit: Int
    ): List<HourlyWeather> {
        val nextHour = now.withMinute(0).withSecond(0).withNano(0).plusHours(1)
        return hourly
            .filter { item -> item.time.toLocalDateTimeOrNull()?.let { it >= nextHour } == true }
            .take(limit)
    }

    /**
     * Текущее локальное время в зоне локации. Время прогноза Open-Meteo приходит без смещения
     * и привязано к зоне точки (timezone=auto), поэтому сравнивать его нужно с «сейчас» в той же зоне,
     * а не с временем устройства. При неизвестной/некорректной зоне — фолбэк на зону устройства.
     */
    fun nowInZone(timezone: String?): LocalDateTime =
        timezone
            ?.let { runCatching { LocalDateTime.now(ZoneId.of(it)) }.getOrNull() }
            ?: LocalDateTime.now()

    private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
        try {
            LocalDateTime.parse(this)
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(this).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                null
            }
        }
}
