package sevynidd.bloomseason.data.report

import java.time.LocalDate
import kotlin.math.sqrt
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.core.PainSample
import sevynidd.bloomseason.core.cycleHistory
import sevynidd.bloomseason.core.detectPeriods
import sevynidd.bloomseason.data.DayEntryEntity
import sevynidd.bloomseason.data.PainEntryEntity

/** Ein Zyklus der Auswertung; der laufende Zyklus hat noch keine Länge. */
data class ReportCycle(
    val start: LocalDate,
    val cycleLength: Int?,
    val periodLength: Int,
    val strongestBleeding: Bleeding,
    val painAverage: Double?,
    val painMax: Int?,
    /** Die häufigsten Schmerzorte des Zyklus, höchstens drei. */
    val painLocations: List<String>,
)

data class ReportSummary(
    val cycles: Int,
    val meanCycle: Double?,
    val sdCycle: Double?,
    val shortestCycle: Int?,
    val longestCycle: Int?,
    val meanPeriod: Double?,
    val cyclesWithPain: Int,
)

data class DoctorReport(val from: LocalDate, val to: LocalDate, val cycles: List<ReportCycle>, val summary: ReportSummary)

/** Stellt die Zyklen der letzten `months` Monate zusammen, ohne Angabe alle erfassten. Nur Zahlen, keine Bewertung. */
fun buildReport(days: List<DayEntryEntity>, pain: List<PainEntryEntity>, today: LocalDate, months: Int?): DoctorReport {
    val bleeding = days.associate { LocalDate.ofEpochDay(it.epochDay) to Bleeding.fromLevel(it.bleeding) }
    val periods = detectPeriods(bleeding.filterValues { it.isPeriodDay }.keys)
    val painByDate = pain.filter { it.intensity > 0 }.groupBy { LocalDate.ofEpochDay(it.epochDay) }
    val history = cycleHistory(periods, painByDate.flatMap { (date, items) -> items.map { PainSample(date, it.intensity) } })
    val from = months?.let { today.minusMonths(it.toLong()) } ?: periods.firstOrNull()?.start ?: today

    val cycles = periods.indices.filter { !periods[it].start.isBefore(from) }.map { index ->
        val period = periods[index]
        val point = history[index]
        val end = periods.getOrNull(index + 1)?.start?.minusDays(1) ?: today
        val inCycle = painByDate.filterKeys { !it.isBefore(period.start) && !it.isAfter(end) }.values.flatten()
        ReportCycle(
            start = period.start,
            cycleLength = point.cycleLength,
            periodLength = period.length,
            strongestBleeding = bleeding.filterKeys { !it.isBefore(period.start) && !it.isAfter(period.end) }.values
                .maxByOrNull { it.level } ?: Bleeding.NONE,
            painAverage = point.painAverage,
            painMax = inCycle.maxOfOrNull { it.intensity },
            painLocations = inCycle.groupingBy { it.location }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(3).map { it.key },
        )
    }
    val lengths = cycles.mapNotNull { it.cycleLength }
    return DoctorReport(
        from = from,
        to = today,
        cycles = cycles,
        summary = ReportSummary(
            cycles = cycles.size,
            meanCycle = lengths.takeIf { it.isNotEmpty() }?.average(),
            sdCycle = lengths.takeIf { it.size >= 2 }?.let(::sampleSd),
            shortestCycle = lengths.minOrNull(),
            longestCycle = lengths.maxOrNull(),
            meanPeriod = cycles.takeIf { it.isNotEmpty() }?.map { it.periodLength }?.average(),
            cyclesWithPain = cycles.count { it.painAverage != null },
        ),
    )
}

private fun sampleSd(values: List<Int>): Double {
    val mean = values.average()
    return sqrt(values.sumOf { (it - mean) * (it - mean) } / (values.size - 1))
}
