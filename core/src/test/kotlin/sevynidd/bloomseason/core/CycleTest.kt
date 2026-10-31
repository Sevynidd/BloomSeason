package sevynidd.bloomseason.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleTest {
    private val first = LocalDate.of(2026, 1, 5)

    private fun period(start: LocalDate, length: Int = 5) = Period(start, start.plusDays(length - 1L))

    private fun steadyPeriods(count: Int, cycle: Int = 28): List<Period> =
        (0 until count).map { period(first.plusDays(it * cycle.toLong())) }

    @Test
    fun detectPeriodsMergesSmallGapsAndSplitsLargeOnes() {
        val d = { n: Int -> LocalDate.of(2026, 3, n) }
        val one = detectPeriods(listOf(d(1), d(2), d(3), d(5)))
        assertEquals(listOf(Period(d(1), d(5))), one)
        val two = detectPeriods(listOf(d(1), d(2), d(6), d(7)))
        assertEquals(listOf(Period(d(1), d(2)), Period(d(6), d(7))), two)
        assertTrue(detectPeriods(emptyList()).isEmpty())
    }

    @Test
    fun spottingIsNotAPeriodDay() {
        assertTrue(!Bleeding.SPOTTING.isPeriodDay)
        assertTrue(Bleeding.LIGHT.isPeriodDay)
    }

    @Test
    fun phaseLayoutForTwentyEightDayCycle() {
        val layout = phaseLayout(28, 5)
        assertEquals(
            listOf(
                PhaseSpan(Season.WINTER, 1, 5),
                PhaseSpan(Season.SPRING, 6, 13),
                PhaseSpan(Season.SUMMER, 14, 16),
                PhaseSpan(Season.AUTUMN, 17, 28),
            ),
            layout,
        )
    }

    @Test
    fun phaseLayoutOfShortCycleStaysConsistent() {
        val layout = phaseLayout(20, 5)
        assertEquals(1, layout.first().firstDay)
        assertEquals(20, layout.last().lastDay)
        layout.zipWithNext { a, b -> assertEquals(a.lastDay + 1, b.firstDay) }
    }

    @Test
    fun steadyCyclesGiveNarrowWindowAndHighConfidence() {
        val periods = steadyPeriods(7)
        val today = periods.last().start.plusDays(8)
        val model = CycleModel(periods, today)
        val prediction = model.prediction!!
        assertEquals(28, prediction.cycleLength)
        assertEquals(periods.last().start.plusDays(28), prediction.expectedStart)
        assertEquals(prediction.expectedStart.minusDays(1), prediction.earliest)
        assertEquals(prediction.expectedStart.plusDays(1), prediction.latest)
        assertEquals(Confidence.HIGH, prediction.confidence)
        assertEquals(6, model.stats.cycles.size)
    }

    @Test
    fun irregularCyclesWidenWindowAndLowerConfidence() {
        val starts = listOf(0, 26, 57, 83, 114, 140, 171)
        val periods = starts.map { period(first.plusDays(it.toLong())) }
        val prediction = CycleModel(periods, periods.last().start.plusDays(3)).prediction!!
        assertTrue(prediction.latest.toEpochDay() - prediction.earliest.toEpochDay() >= 4)
        assertTrue(prediction.confidence != Confidence.HIGH)
    }

    @Test
    fun recentCyclesWeighMoreThanOldOnes() {
        val starts = listOf(0, 30, 60, 90, 117, 144)
        val periods = starts.map { period(first.plusDays(it.toLong())) }
        val prediction = CycleModel(periods, periods.last().start.plusDays(3)).prediction!!
        assertTrue(prediction.cycleLength in 28..29)
    }

    @Test
    fun noPeriodsMeansNoPrediction() {
        val model = CycleModel(emptyList(), first)
        assertNull(model.prediction)
        assertNull(model.phaseOn(first))
    }

    @Test
    fun singlePeriodUsesDefaultsWithLowConfidence() {
        val periods = steadyPeriods(1)
        val prediction = CycleModel(periods, first.plusDays(2)).prediction!!
        assertEquals(DEFAULT_CYCLE_LENGTH, prediction.cycleLength)
        assertEquals(Confidence.LOW, prediction.confidence)
        assertTrue(prediction.usesDefaults)
    }

    @Test
    fun fewCyclesUseTheirMeanButStayLow() {
        val periods = listOf(period(first), period(first.plusDays(33)), period(first.plusDays(66)))
        val prediction = CycleModel(periods, first.plusDays(70)).prediction!!
        assertEquals(33, prediction.cycleLength)
        assertEquals(Confidence.LOW, prediction.confidence)
    }

    @Test
    fun implausibleCycleLengthsAreIgnored() {
        val periods = listOf(period(first), period(first.plusDays(90)), period(first.plusDays(118)))
        val stats = computeStats(periods, first.plusDays(125))
        assertEquals(listOf(28), stats.cycles.map { it.days })
    }

    @Test
    fun unfinishedLatestPeriodIsNotCountedForPeriodLength() {
        val periods = steadyPeriods(3, 28).dropLast(1) + period(first.plusDays(56), length = 2)
        val stats = computeStats(periods, first.plusDays(57))
        assertEquals(5.0, stats.meanPeriod!!, 0.0001)
    }

    @Test
    fun phaseOnFollowsTheCycle() {
        val periods = steadyPeriods(7)
        val last = periods.last().start
        val model = CycleModel(periods, today = last.plusDays(8))
        assertEquals(DayPhase(Season.WINTER, 1, 28, false), model.phaseOn(last))
        assertEquals(Season.SPRING, model.phaseOn(last.plusDays(8))!!.season)
        assertEquals(Season.SUMMER, model.phaseOn(last.plusDays(14))!!.season)
        val projected = model.phaseOn(last.plusDays(28))!!
        assertEquals(Season.WINTER, projected.season)
        assertTrue(projected.isProjected)
        assertNull(model.phaseOn(first.minusDays(1)))
    }

    @Test
    fun overdueDaysStayInAutumn() {
        val periods = steadyPeriods(4)
        val last = periods.last().start
        val phase = CycleModel(periods, today = last.plusDays(33)).phaseOn(last.plusDays(32))!!
        assertEquals(Season.AUTUMN, phase.season)
        assertTrue(phase.cycleDay > 28)
    }

    @Test
    fun painIsAveragedPerSeason() {
        val periods = steadyPeriods(4)
        val model = CycleModel(periods, periods.last().start.plusDays(8))
        val samples = listOf(
            PainSample(periods[1].start, 8),
            PainSample(periods[1].start.plusDays(1), 4),
            PainSample(periods[1].start.plusDays(20), 2),
        )
        val bySeason = model.painBySeason(samples)
        assertEquals(6.0, bySeason.getValue(Season.WINTER)!!, 0.0001)
        assertEquals(2.0, bySeason.getValue(Season.AUTUMN)!!, 0.0001)
        assertNull(bySeason.getValue(Season.SPRING))
    }
}
