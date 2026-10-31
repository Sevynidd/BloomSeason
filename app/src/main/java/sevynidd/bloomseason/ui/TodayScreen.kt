package sevynidd.bloomseason.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import sevynidd.bloomseason.AppState
import sevynidd.bloomseason.core.Confidence
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.core.daysBetween
import sevynidd.bloomseason.core.phaseLayout
import sevynidd.bloomseason.ui.theme.Tabular

private val DayMonth = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)
private val DayMonthShort = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)

@Composable
fun TodayScreen(
    state: AppState,
    onAddToday: () -> Unit,
    onStartPeriod: () -> Unit,
) {
    val today = state.today
    val model = state.model
    val prediction = model.prediction
    val phase = model.phaseOn(today)
    val inPeriod = phase?.season == Season.WINTER

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppBar(today.fullLabel(), subtitle = true)

        if (prediction == null || phase == null) {
            Hero(
                season = Season.SPRING,
                dayLine = "Noch keine Daten",
                name = "Willkommen",
                words = "Trage den Beginn deiner letzten Periode ein",
                phaseName = "Los geht's",
            )
            BloomCard {
                Text("Nächste Periode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("noch keine Vorhersage", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Sobald ein Periodenbeginn erfasst ist, erscheint hier das Zeitfenster.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val overdue = phase.cycleDay > phase.cycleLength
            Hero(
                season = phase.season,
                dayLine = if (overdue) "Zyklustag ${phase.cycleDay} · überfällig" else "Zyklustag ${phase.cycleDay} von ${phase.cycleLength}",
                name = phase.season.label,
                words = phase.season.words,
                phaseName = phase.season.phaseName,
            )
            BloomCard {
                val headline = nextHeadline(today, prediction.earliest, prediction.latest, inPeriod, phase.cycleDay, prediction.periodLength)
                Text(headline.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(headline.big, style = MaterialTheme.typography.headlineMedium.merge(Tabular), modifier = Modifier.padding(top = 2.dp))
                Text(headline.sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                YearRun(
                    spans = phaseLayout(phase.cycleLength, maxOf(prediction.periodLength, 1)),
                    cycleLength = phase.cycleLength,
                    windowFrom = daysBetween(prediction.lastStart, prediction.earliest) + 1,
                    windowTo = daysBetween(prediction.lastStart, prediction.latest) + 1,
                    currentDay = phase.cycleDay,
                    modifier = Modifier.padding(top = 6.dp),
                )
                HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfidenceMeter(prediction.confidence.bars)
                    Text(
                        confidenceText(prediction.confidence, prediction.basedOnCycles, prediction.sd, prediction.usesDefaults),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(onClick = onAddToday, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Heute eintragen", style = MaterialTheme.typography.titleMedium)
        }
        if (!inPeriod) {
            OutlinedButton(
                onClick = onStartPeriod,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Periode hat begonnen", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Hero(season: Season, dayLine: String, name: String, words: String, phaseName: String) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = container,
        contentColor = onContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 32.dp, bottomStart = 12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 222.dp),
    ) {
        Box {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-28).dp).size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    listOf(40f, 62f, 84f).forEach { radius ->
                        drawCircle(onContainer.copy(alpha = 0.18f), radius = radius.dp.toPx() * size.width / 190.dp.toPx(), style = Stroke(1.dp.toPx()))
                    }
                }
                Icon(season.glyph, contentDescription = null, tint = accent, modifier = Modifier.size(80.dp))
            }
            Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 18.dp)) {
                Text(dayLine, style = MaterialTheme.typography.labelLarge.merge(Tabular))
                Text(name, style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 52.dp, bottom = 2.dp))
                Text(words, style = MaterialTheme.typography.bodyLarge)
                Surface(
                    color = container,
                    contentColor = onContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, onContainer),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Text(
                        phaseName,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.heightIn(min = 28.dp).padding(horizontal = 12.dp).wrapVerticalCenter(),
                    )
                }
            }
        }
    }
}

private fun Modifier.wrapVerticalCenter(): Modifier = this.padding(vertical = 6.dp)

private data class NextHeadline(val label: String, val big: String, val sub: String)

private fun nextHeadline(
    today: LocalDate,
    earliest: LocalDate,
    latest: LocalDate,
    inPeriod: Boolean,
    cycleDay: Int,
    periodLength: Int,
): NextHeadline {
    if (inPeriod) {
        val remaining = (periodLength - cycleDay).coerceAtLeast(0)
        val sub = if (remaining == 0) "Ende bald erwartet" else "voraussichtliches Ende in ${maxOf(remaining - 1, 0)}–${remaining + 1} Tagen"
        return NextHeadline("Periode läuft", "Tag $cycleDay", sub)
    }
    val from = daysBetween(today, earliest)
    val to = daysBetween(today, latest)
    val window = windowText(earliest, latest)
    return when {
        to < 0 -> NextHeadline("Nächste Periode", "seit ${-to} ${days(-to)} überfällig", "erwartet war $window")
        from <= 0 -> NextHeadline("Nächste Periode", "jetzt erwartet", "Zeitfenster bis ${latest.format(DayMonth)}")
        from == to -> NextHeadline("Nächste Periode", "in $from ${days(from)}", "voraussichtlich ${earliest.format(DayMonth)}")
        else -> NextHeadline("Nächste Periode", "in $from–$to Tagen", "voraussichtlich $window")
    }
}

private fun days(count: Int) = if (count == 1) "Tag" else "Tagen"

private fun windowText(earliest: LocalDate, latest: LocalDate): String =
    if (earliest.month == latest.month) {
        "${earliest.dayOfMonth}.–${latest.dayOfMonth}. ${latest.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMAN))}"
    } else {
        "${earliest.format(DayMonthShort)}–${latest.format(DayMonthShort)}"
    }

private fun confidenceText(confidence: Confidence, cycles: Int, sd: Double, usesDefaults: Boolean): String {
    if (usesDefaults) return "noch unsicher · $cycles von 3 Zyklen, Standardwerte als Grundlage"
    val label = when (confidence) {
        Confidence.HIGH -> "sehr sicher"
        Confidence.MEDIUM -> "recht sicher"
        Confidence.LOW -> "noch unsicher"
    }
    return "$label · aus $cycles Zyklen, ± ${decimal(sd)} Tage"
}
