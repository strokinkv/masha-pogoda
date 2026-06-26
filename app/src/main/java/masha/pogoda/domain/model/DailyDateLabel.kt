package masha.pogoda.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Метка дня для раздела «7 дней»: сегодня/завтра, дальше — день недели.
 * Дата приходит в ISO-формате (yyyy-MM-dd) в зоне локации.
 */
object DailyDateLabel {
    private val RU = Locale("ru")
    private val WEEKDAY = DateTimeFormatter.ofPattern("EEEE", RU)

    fun format(date: String, today: LocalDate = LocalDate.now()): String {
        val parsed = try {
            LocalDate.parse(date)
        } catch (_: DateTimeParseException) {
            return date
        }
        return when (parsed) {
            today -> "Сегодня"
            today.plusDays(1) -> "Завтра"
            else -> parsed.format(WEEKDAY).replaceFirstChar { it.titlecase(RU) }
        }
    }
}
