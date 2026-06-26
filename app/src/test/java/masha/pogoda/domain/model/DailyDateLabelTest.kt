package masha.pogoda.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyDateLabelTest {
    private val today = LocalDate.parse("2026-06-26") // пятница

    @Test
    fun todayAndTomorrow() {
        assertEquals("Сегодня", DailyDateLabel.format("2026-06-26", today))
        assertEquals("Завтра", DailyDateLabel.format("2026-06-27", today))
    }

    @Test
    fun furtherDaysUseWeekdayName() {
        assertEquals("Воскресенье", DailyDateLabel.format("2026-06-28", today))
        assertEquals("Понедельник", DailyDateLabel.format("2026-06-29", today))
    }

    @Test
    fun invalidDateFallsBackToRaw() {
        assertEquals("not-a-date", DailyDateLabel.format("not-a-date", today))
    }
}
