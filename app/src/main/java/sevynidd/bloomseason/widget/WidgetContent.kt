package sevynidd.bloomseason.widget

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.PhaseSpan
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.core.daysBetween
import sevynidd.bloomseason.core.phaseLayout
import sevynidd.bloomseason.ui.label

/** Die vier Jahreszeiten des laufenden Zyklus und der heutige Tag darin, für den Jahreslauf im großen Widget. */
data class YearRun(val spans: List<PhaseSpan>, val day: Int, val length: Int)

/**
 * Texte für alle Widget-Größen. `title`, `big` und `sub` füllen das mittlere, `name`, `number` und `unit` das
 * kompakte Widget; das große zeigt zusätzlich `window` und den Jahreslauf. `season` bestimmt die Farben, null
 * heißt noch keine Daten.
 */
data class WidgetContent(
    val season: Season?,
    val name: String,
    val title: String,
    val big: String,
    val sub: String,
    val number: String,
    val unit: String,
    val window: String,
    val year: YearRun?,
)

private val DayMonthShort = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)
private val MonthName = DateTimeFormatter.ofPattern("MMMM", Locale.GERMAN)

/** Mit neutralem Text steht kein Wort zur Periode auf dem Startbildschirm. */
fun widgetContent(model: CycleModel, today: LocalDate, neutral: Boolean): WidgetContent {
    val prediction = model.prediction
    val phase = model.phaseOn(today)
    if (prediction == null || phase == null) {
        return WidgetContent(null, "BloomSeason", "BloomSeason", "Noch keine Daten", "", "–", "keine Daten", "", null)
    }
    val name = phase.season.label
    val year = YearRun(
        phaseLayout(phase.cycleLength, maxOf(prediction.periodLength, 1)),
        phase.cycleDay,
        phase.cycleLength,
    )
    if (neutral) return WidgetContent(phase.season, name, name, "Tag ${phase.cycleDay}", "", "Tag ${phase.cycleDay}", "", "", year)
    if (phase.season == Season.WINTER) {
        val remaining = (prediction.periodLength - phase.cycleDay).coerceAtLeast(0)
        val end = if (remaining == 0) "Ende bald erwartet" else "voraussichtliches Ende in ${maxOf(remaining - 1, 0)}–${remaining + 1} Tagen"
        return WidgetContent(phase.season, name, name, "Tag ${phase.cycleDay}", "Periode läuft", "Tag ${phase.cycleDay}", "Periode läuft", end, year)
    }
    val from = daysBetween(today, prediction.earliest)
    val to = daysBetween(today, prediction.latest)
    val big = when {
        to < 0 -> "seit ${-to} ${days(-to)} überfällig"
        from <= 0 -> "jetzt erwartet"
        from == to -> "in $from ${days(from)}"
        else -> "in $from–$to Tagen"
    }
    val (number, unit) = when {
        to < 0 -> "+${-to}" to "überfällig"
        from <= 0 -> "jetzt" to "erwartet"
        from == to -> "$from" to "${if (from == 1) "Tag" else "Tage"} bis zur Periode"
        else -> "$from–$to" to "Tage bis zur Periode"
    }
    return WidgetContent(
        season = phase.season,
        name = name,
        title = "$name · Tag ${phase.cycleDay}",
        big = big,
        sub = "nächste Periode",
        number = number,
        unit = unit,
        window = "voraussichtlich ${windowText(prediction.earliest, prediction.latest)}",
        year = year,
    )
}

private fun days(count: Int) = if (count == 1) "Tag" else "Tagen"

private fun windowText(earliest: LocalDate, latest: LocalDate): String =
    if (earliest.month == latest.month) {
        "${earliest.dayOfMonth}.–${latest.dayOfMonth}. ${latest.format(MonthName)}"
    } else {
        "${earliest.format(DayMonthShort)}–${latest.format(DayMonthShort)}"
    }
