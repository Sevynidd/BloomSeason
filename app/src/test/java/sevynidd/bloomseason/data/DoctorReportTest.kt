package sevynidd.bloomseason.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.data.report.buildReport

class DoctorReportTest {
    private val first = LocalDate.of(2026, 1, 5)

    /** Drei Zyklen zu 28 Tagen mit je fünf Tagen Blutung, der erste mit starker Blutung am zweiten Tag. */
    private val days = (0 until 3).flatMap { cycle ->
        (0 until 5).map { day ->
            val level = if (cycle == 0 && day == 1) Bleeding.HEAVY.level else Bleeding.MEDIUM.level
            DayEntryEntity(first.plusDays(cycle * 28L + day).toEpochDay(), level)
        }
    }
    private val pain = listOf(
        PainEntryEntity(first.plusDays(1).toEpochDay(), "Bauch", 6),
        PainEntryEntity(first.plusDays(2).toEpochDay(), "Bauch", 4),
        PainEntryEntity(first.plusDays(2).toEpochDay(), "Rücken", 2),
    )
    private val today = first.plusDays(70)

    @Test
    fun summarisesAllCyclesWithoutALimit() {
        val report = buildReport(days, pain, today, months = null)
        assertEquals(3, report.cycles.size)
        assertEquals(28.0, report.summary.meanCycle!!, 0.0001)
        assertEquals(28, report.summary.shortestCycle)
        assertEquals(5.0, report.summary.meanPeriod!!, 0.0001)
        assertEquals(1, report.summary.cyclesWithPain)
    }

    @Test
    fun firstCycleCarriesBleedingAndPain() {
        val cycle = buildReport(days, pain, today, months = null).cycles.first()
        assertEquals(Bleeding.HEAVY, cycle.strongestBleeding)
        assertEquals(28, cycle.cycleLength)
        // Stärkster Wert je Tag: 6 und 4, Mittel 5.
        assertEquals(5.0, cycle.painAverage!!, 0.0001)
        assertEquals(6, cycle.painMax)
        assertEquals(listOf("Bauch", "Rücken"), cycle.painLocations)
    }

    @Test
    fun theRunningCycleHasNoLength() {
        val last = buildReport(days, pain, today, months = null).cycles.last()
        assertNull(last.cycleLength)
        assertNull(last.painAverage)
    }

    @Test
    fun theMonthLimitDropsOlderCycles() {
        val report = buildReport(days, pain, today, months = 1)
        assertEquals(listOf(first.plusDays(56)), report.cycles.map { it.start })
    }

    @Test
    fun withoutDataTheReportIsEmpty() {
        val report = buildReport(emptyList(), emptyList(), today, months = 12)
        assertEquals(0, report.cycles.size)
        assertNull(report.summary.meanCycle)
    }
}
