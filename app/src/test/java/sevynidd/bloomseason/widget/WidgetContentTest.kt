package sevynidd.bloomseason.widget

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.Period
import sevynidd.bloomseason.core.Season

class WidgetContentTest {
    private val first = LocalDate.of(2026, 1, 5)
    private val periods = (0 until 5).map { Period(first.plusDays(it * 28L), first.plusDays(it * 28L + 4)) }
    private fun content(today: LocalDate, neutral: Boolean = false) = widgetContent(CycleModel(periods, today), today, neutral)

    @Test
    fun withoutDataItAsksForAnEntry() {
        val today = LocalDate.of(2026, 3, 1)
        val empty = widgetContent(CycleModel(emptyList(), today), today, false)
        assertNull(empty.season)
        assertEquals("Noch keine Daten", empty.big)
    }

    @Test
    fun duringThePeriodItShowsTheDay() {
        val result = content(periods.last().start.plusDays(1))
        assertEquals(Season.WINTER, result.season)
        assertEquals("Tag 2", result.big)
        assertEquals("Periode läuft", result.sub)
        assertEquals("Tag 2", result.number)
    }

    @Test
    fun betweenPeriodsItCountsDown() {
        val result = content(periods.last().end.plusDays(10))
        assert(result.big.startsWith("in ")) { result.big }
        assertEquals("nächste Periode", result.sub)
        assert(result.number.contains("–") || result.number.all { it.isDigit() }) { result.number }
        assert(result.window.startsWith("voraussichtlich ")) { result.window }
        assertNotNull(result.year)
    }

    @Test
    fun neutralTextNamesNoPeriod() {
        val result = content(periods.last().end.plusDays(10), neutral = true)
        assertEquals("", result.sub)
        assert(result.big.startsWith("Tag ")) { result.big }
        assertEquals("", result.window)
    }
}
