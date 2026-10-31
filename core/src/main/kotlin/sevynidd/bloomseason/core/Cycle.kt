package sevynidd.bloomseason.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val DEFAULT_CYCLE_LENGTH = 28
const val DEFAULT_PERIOD_LENGTH = 5
const val DEFAULT_SD = 3.0
const val RECENT_CYCLES = 6
const val MIN_CYCLES_FOR_ESTIMATE = 3
val VALID_CYCLE_LENGTHS = 15..60

enum class Season { WINTER, SPRING, SUMMER, AUTUMN }

enum class Bleeding(val level: Int) {
    NONE(0), SPOTTING(1), LIGHT(2), MEDIUM(3), HEAVY(4);

    /** Schmierblutungen zählen nicht als Periodentag. */
    val isPeriodDay: Boolean get() = level >= LIGHT.level

    companion object {
        fun fromLevel(level: Int): Bleeding = entries.firstOrNull { it.level == level } ?: NONE
    }
}

data class Period(val start: LocalDate, val end: LocalDate) {
    val length: Int get() = daysBetween(start, end) + 1
}

data class CycleLength(val start: LocalDate, val days: Int)

data class PhaseSpan(val season: Season, val firstDay: Int, val lastDay: Int)

data class DayPhase(
    val season: Season,
    val cycleDay: Int,
    val cycleLength: Int,
    /** true, wenn der Tag in einem noch nicht begonnenen, vorhergesagten Zyklus liegt. */
    val isProjected: Boolean,
)

enum class Confidence(val bars: Int) { LOW(1), MEDIUM(2), HIGH(3) }

data class CycleStats(
    /** Die letzten (höchstens sechs) gültigen Zykluslängen, älteste zuerst. */
    val cycles: List<CycleLength>,
    val meanCycle: Double?,
    val sdCycle: Double?,
    val meanPeriod: Double?,
    val sdPeriod: Double?,
)

data class Prediction(
    val lastStart: LocalDate,
    val expectedStart: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
    val cycleLength: Int,
    val periodLength: Int,
    val sd: Double,
    val basedOnCycles: Int,
    val confidence: Confidence,
) {
    val usesDefaults: Boolean get() = basedOnCycles < MIN_CYCLES_FOR_ESTIMATE
}

fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

/**
 * Fasst Tage mit Blutung zu Perioden zusammen. Eine Lücke von einem nicht erfassten Tag
 * wird toleriert, ab zwei freien Tagen beginnt eine neue Periode.
 */
fun detectPeriods(bleedingDays: Collection<LocalDate>, maxGapDays: Int = 2): List<Period> {
    val days = bleedingDays.distinct().sorted()
    if (days.isEmpty()) return emptyList()
    val result = mutableListOf<Period>()
    var start = days.first()
    var previous = start
    for (day in days.drop(1)) {
        if (daysBetween(previous, day) > maxGapDays) {
            result += Period(start, previous)
            start = day
        }
        previous = day
    }
    result += Period(start, previous)
    return result
}

fun ovulationDay(cycleLength: Int, periodLength: Int): Int = max(cycleLength - 13, periodLength + 2)

/** Aufteilung eines Zyklus in die vier Jahreszeiten. Eisprung rund 14 Tage vor der nächsten Periode. */
fun phaseLayout(cycleLength: Int, periodLength: Int): List<PhaseSpan> {
    val ovulation = ovulationDay(cycleLength, periodLength)
    return listOf(
        PhaseSpan(Season.WINTER, 1, periodLength),
        PhaseSpan(Season.SPRING, periodLength + 1, ovulation - 2),
        PhaseSpan(Season.SUMMER, ovulation - 1, ovulation + 1),
        PhaseSpan(Season.AUTUMN, ovulation + 2, cycleLength),
    ).filter { it.firstDay <= it.lastDay }
}

fun seasonOfDay(cycleDay: Int, cycleLength: Int, periodLength: Int): Season =
    phaseLayout(cycleLength, periodLength).firstOrNull { cycleDay in it.firstDay..it.lastDay }?.season
        ?: Season.AUTUMN

fun computeStats(periods: List<Period>, today: LocalDate): CycleStats {
    val cycles = periods.zipWithNext { a, b -> CycleLength(a.start, daysBetween(a.start, b.start)) }
        .filter { it.days in VALID_CYCLE_LENGTHS }
        .takeLast(RECENT_CYCLES)
    // Eine Periode, die gerade erst endet, ist eventuell noch nicht abgeschlossen.
    val finished = periods.filter { daysBetween(it.end, today) >= 2 }.takeLast(RECENT_CYCLES)
    val cycleValues = cycles.map { it.days.toDouble() }
    val periodValues = finished.map { it.length.toDouble() }
    return CycleStats(
        cycles = cycles,
        meanCycle = cycleValues.takeIf { it.isNotEmpty() }?.average(),
        sdCycle = cycleValues.takeIf { it.isNotEmpty() }?.let(::sampleSd),
        meanPeriod = periodValues.takeIf { it.isNotEmpty() }?.average(),
        sdPeriod = periodValues.takeIf { it.isNotEmpty() }?.let(::sampleSd),
    )
}

private fun sampleSd(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    return sqrt(values.sumOf { (it - mean) * (it - mean) } / (values.size - 1))
}

/** Gewichteter Mittelwert, neuere Zyklen zählen stärker (Gewichte 1, 2, 3 …). */
private fun weightedMean(values: List<Double>): Double {
    val weights = values.indices.map { it + 1.0 }
    return values.zip(weights).sumOf { (v, w) -> v * w } / weights.sum()
}

fun predict(periods: List<Period>, stats: CycleStats): Prediction? {
    val last = periods.lastOrNull() ?: return null
    val n = stats.cycles.size
    val cycleLength = when {
        n == 0 -> DEFAULT_CYCLE_LENGTH
        n < MIN_CYCLES_FOR_ESTIMATE -> stats.meanCycle!!.roundToInt()
        else -> weightedMean(stats.cycles.map { it.days.toDouble() }).roundToInt()
    }
    val sd = if (n >= MIN_CYCLES_FOR_ESTIMATE) stats.sdCycle ?: DEFAULT_SD else DEFAULT_SD
    val periodLength = (stats.meanPeriod?.roundToInt() ?: DEFAULT_PERIOD_LENGTH).coerceIn(2, 10)
    val half = max(1, ceil(1.25 * sd).toInt())
    val expected = last.start.plusDays(cycleLength.toLong())
    val confidence = when {
        n >= RECENT_CYCLES && sd <= 2.0 -> Confidence.HIGH
        n >= MIN_CYCLES_FOR_ESTIMATE && sd <= 4.0 -> Confidence.MEDIUM
        else -> Confidence.LOW
    }
    return Prediction(
        lastStart = last.start,
        expectedStart = expected,
        earliest = expected.minusDays(half.toLong()),
        latest = expected.plusDays(half.toLong()),
        cycleLength = cycleLength,
        periodLength = periodLength,
        sd = sd,
        basedOnCycles = n,
        confidence = confidence,
    )
}

class CycleModel(val periods: List<Period>, val today: LocalDate) {
    val stats: CycleStats = computeStats(periods, today)
    val prediction: Prediction? = predict(periods, stats)

    /** Jahreszeit und Zyklustag für ein Datum; null, wenn davor keine Periode erfasst ist. */
    fun phaseOn(date: LocalDate): DayPhase? {
        val prediction = prediction ?: return null
        val index = periods.indexOfLast { !it.start.isAfter(date) }
        if (index < 0) return null
        val period = periods[index]
        val day = daysBetween(period.start, date) + 1
        if (index < periods.lastIndex) {
            val length = daysBetween(period.start, periods[index + 1].start)
            return DayPhase(seasonOfDay(day, length, period.length), day, length, isProjected = false)
        }
        val length = prediction.cycleLength
        if (day <= length) {
            val periodLength = max(prediction.periodLength, period.length)
            return DayPhase(seasonOfDay(day, length, periodLength), day, length, date.isAfter(today))
        }
        if (!date.isAfter(today)) return DayPhase(Season.AUTUMN, day, length, isProjected = false)
        val nextDay = daysBetween(prediction.expectedStart, date) + 1
        return if (nextDay in 1..length) {
            DayPhase(seasonOfDay(nextDay, length, prediction.periodLength), nextDay, length, isProjected = true)
        } else {
            null
        }
    }

    /** Durchschnittliche Schmerzstärke je Jahreszeit; null, wenn dort nichts erfasst ist. */
    fun painBySeason(samples: List<PainSample>): Map<Season, Double?> {
        val bySeason = samples.mapNotNull { sample ->
            phaseOn(sample.date)?.takeIf { !it.isProjected }?.let { it.season to sample.intensity }
        }.groupBy({ it.first }, { it.second })
        return Season.entries.associateWith { season -> bySeason[season]?.average() }
    }
}

data class PainSample(val date: LocalDate, val intensity: Int)
