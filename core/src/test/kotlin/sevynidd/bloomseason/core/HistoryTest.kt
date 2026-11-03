package sevynidd.bloomseason.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {
    private val first = LocalDate.of(2026, 1, 5)

    private fun period(start: LocalDate, length: Int = 5) = Period(start, start.plusDays(length - 1L))

    /** Perioden mit den angegebenen Abständen zwischen den Anfängen. */
    private fun periodsWithGaps(vararg gaps: Int): List<Period> {
        var start = first
        return listOf(period(start)) + gaps.map { gap -> period(start.plusDays(gap.toLong()).also { start = it }) }
    }

    @Test
    fun steadyCyclesAreAllHits() {
        val checks = predictionChecks(periodsWithGaps(28, 28, 28, 28))
        assertEquals(4, checks.size)
        assertTrue(checks.all { it.hit && it.deviation == 0 })
        assertEquals(0.0, PredictionSummary(checks).meanAbsoluteDeviation!!, 0.0001)
    }

    @Test
    fun theFirstPredictionUsesDefaults() {
        val checks = predictionChecks(periodsWithGaps(30))
        assertEquals(1, checks.size)
        assertTrue(checks[0].usesDefaults)
        // Standard sind 28 Tage, tatsächlich waren es 30: zwei Tage später, noch im Fenster.
        assertEquals(2, checks[0].deviation)
        assertTrue(checks[0].hit)
    }

    @Test
    fun aLateCycleIsAMiss() {
        val checks = predictionChecks(periodsWithGaps(28, 28, 28, 40))
        val last = checks.last()
        assertTrue(!last.hit)
        assertEquals(12, last.deviation)
        assertEquals(3, PredictionSummary(checks).hits)
    }

    @Test
    fun cyclesOutsideTheValidLengthAreSkipped() {
        assertTrue(predictionChecks(periodsWithGaps(90)).isEmpty())
        assertTrue(predictionChecks(periodsWithGaps()).isEmpty())
    }

    @Test
    fun historyKeepsLengthsAndPain() {
        val periods = periodsWithGaps(28, 27)
        val pain = listOf(
            PainSample(first.plusDays(1), 4),
            PainSample(first.plusDays(1), 6),
            PainSample(first.plusDays(2), 2),
            PainSample(first.plusDays(30), 8),
        )
        val history = cycleHistory(periods, pain)
        assertEquals(listOf(28, 27, null), history.map { it.cycleLength })
        assertEquals(listOf(5, 5, 5), history.map { it.periodLength })
        // Erster Zyklus: stärkster Wert je Tag, also 6 und 2, Mittel 4.
        assertEquals(4.0, history[0].painAverage!!, 0.0001)
        assertEquals(8.0, history[1].painAverage!!, 0.0001)
        assertNull(history[2].painAverage)
    }
}
