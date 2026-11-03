package sevynidd.bloomseason.core

import java.time.LocalDate
import kotlin.math.abs

/** Eine Vorhersage, so wie sie vor dem tatsächlichen Periodenbeginn aus den früheren Zyklen berechnet wurde. */
data class PredictionCheck(
    val actualStart: LocalDate,
    val expectedStart: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
    val usesDefaults: Boolean,
) {
    /** Positiv: Die Periode kam später als erwartet. */
    val deviation: Int get() = daysBetween(expectedStart, actualStart)
    val hit: Boolean get() = !actualStart.isBefore(earliest) && !actualStart.isAfter(latest)
}

data class PredictionSummary(val checks: List<PredictionCheck>) {
    val hits: Int get() = checks.count { it.hit }
    val meanAbsoluteDeviation: Double? get() = checks.takeIf { it.isNotEmpty() }?.map { abs(it.deviation) }?.average()
}

/**
 * Vergleicht für jede erfasste Periode die Vorhersage, die kurz davor aus den früheren Perioden entstanden wäre,
 * mit dem tatsächlichen Beginn. Zyklen außerhalb der gültigen Länge (zum Beispiel nach einer Erfassungslücke)
 * bleiben außen vor, älteste zuerst.
 */
fun predictionChecks(periods: List<Period>): List<PredictionCheck> =
    (1 until periods.size).mapNotNull { index ->
        val prior = periods.subList(0, index)
        val actual = periods[index].start
        if (daysBetween(prior.last().start, actual) !in VALID_CYCLE_LENGTHS) return@mapNotNull null
        val prediction = predict(prior, computeStats(prior, actual)) ?: return@mapNotNull null
        PredictionCheck(actual, prediction.expectedStart, prediction.earliest, prediction.latest, prediction.usesDefaults)
    }

/** Ein Zyklus mit den Werten, die im Verlauf über die Zeit gezeigt werden. */
data class CyclePoint(
    val start: LocalDate,
    /** null für den laufenden Zyklus und für Zyklen außerhalb der gültigen Länge. */
    val cycleLength: Int?,
    val periodLength: Int,
    /** Mittel der täglich stärksten Schmerzen im Zyklus; null, wenn nichts erfasst ist. */
    val painAverage: Double?,
)

fun cycleHistory(periods: List<Period>, pain: List<PainSample>): List<CyclePoint> {
    val strongestPerDay = pain.groupBy { it.date }.mapValues { (_, samples) -> samples.maxOf { it.intensity } }
    return periods.mapIndexed { index, period ->
        val next = periods.getOrNull(index + 1)?.start
        val length = next?.let { daysBetween(period.start, it) }?.takeIf { it in VALID_CYCLE_LENGTHS }
        val inCycle = strongestPerDay.filterKeys { date -> !date.isBefore(period.start) && (next == null || date.isBefore(next)) }
        CyclePoint(period.start, length, period.length, inCycle.values.takeIf { it.isNotEmpty() }?.average())
    }
}
