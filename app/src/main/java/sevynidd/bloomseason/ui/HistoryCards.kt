package sevynidd.bloomseason.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import sevynidd.bloomseason.AppState
import sevynidd.bloomseason.core.PredictionCheck
import sevynidd.bloomseason.core.PredictionSummary
import sevynidd.bloomseason.core.cycleHistory
import sevynidd.bloomseason.core.daysBetween
import sevynidd.bloomseason.core.predictionChecks
import sevynidd.bloomseason.ui.theme.Tabular

private const val MAX_POINTS = 24
private const val MAX_CHECKS = 8
private val MonthYear = DateTimeFormatter.ofPattern("MMM yy", Locale.GERMAN)
private val DayMonth = DateTimeFormatter.ofPattern("d.M.", Locale.GERMAN)

private enum class Metric(val label: String, val unit: String) {
    CYCLE("Zyklus", "Tage"),
    PERIOD("Periode", "Tage"),
    PAIN("Schmerz", "von 10"),
}

/** Zykluslänge, Periodendauer und Schmerzstärke aller erfassten Zyklen als Linie über die Zeit. */
@Composable
fun HistoryCard(state: AppState) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val metric = Metric.entries[selected]
    val history = remember(state.model, state.pain) { cycleHistory(state.model.periods, state.painSamples).takeLast(MAX_POINTS) }
    val points = history.mapNotNull { point ->
        val value = when (metric) {
            Metric.CYCLE -> point.cycleLength?.toDouble()
            Metric.PERIOD -> point.periodLength.toDouble()
            Metric.PAIN -> point.painAverage
        }
        value?.let { point.start to it }
    }
    BloomCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Verlauf über die Zeit", style = MaterialTheme.typography.titleMedium)
            Text("${points.size} Zyklen · ${metric.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric.entries.forEachIndexed { index, item ->
                FilterChip(selected = index == selected, onClick = { selected = index }, label = { Text(item.label) })
            }
        }
        if (points.size < 2) {
            Text(
                "Sobald mindestens zwei Zyklen mit diesem Wert erfasst sind, erscheint hier der Verlauf.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LineChart(points, if (metric == Metric.PAIN) 0.0..10.0 else null, metric)
        }
    }
}

@Composable
private fun LineChart(points: List<Pair<LocalDate, Double>>, fixedRange: ClosedFloatingPointRange<Double>?, metric: Metric) {
    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val values = points.map { it.second }
    val mean = values.average()
    val description = "Verlauf ${metric.label}: " + points.joinToString(", ") { (date, value) ->
        "${date.format(MonthYear)} ${decimal(value)}"
    } + ". Durchschnitt ${decimal(mean)}."
    Canvas(Modifier.fillMaxWidth().height(170.dp).padding(top = 12.dp).semantics { contentDescription = description }) {
        val low = fixedRange?.start?.toFloat() ?: (floor(values.min()) - 1).toFloat()
        val high = fixedRange?.endInclusive?.toFloat() ?: (ceil(values.max()) + 1).toFloat()
        val small = TextStyle(color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        val left = 28.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        fun x(index: Int) = left + (size.width - left - 8.dp.toPx()) * index / (points.size - 1)
        fun y(value: Float) = bottom - (value - low) / (high - low) * (bottom - top)

        listOf(low, high).forEach { edge ->
            val label = measurer.measure(decimal(edge.toDouble()), small)
            drawText(label, topLeft = Offset(0f, y(edge) - label.size.height / 2f))
            drawLine(scheme.outlineVariant, Offset(left, y(edge)), Offset(size.width, y(edge)), 1.dp.toPx())
        }
        drawLine(
            scheme.primary.copy(alpha = 0.6f),
            Offset(left, y(mean.toFloat())),
            Offset(size.width, y(mean.toFloat())),
            1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        val path = Path()
        values.forEachIndexed { index, value ->
            if (index == 0) path.moveTo(x(index), y(value.toFloat())) else path.lineTo(x(index), y(value.toFloat()))
        }
        drawPath(path, scheme.primary, style = Stroke(2.dp.toPx()))
        values.forEachIndexed { index, value ->
            drawCircle(scheme.primary, if (index == values.lastIndex) 5.dp.toPx() else 3.dp.toPx(), Offset(x(index), y(value.toFloat())))
        }
        // Höchstens etwa fünf Monatsbeschriftungen, damit sie nicht ineinanderlaufen.
        val step = max(1, ceil(points.size / 5.0).toInt())
        points.forEachIndexed { index, (date, _) ->
            if (index % step == 0 || (index == points.lastIndex && points.lastIndex % step >= 2)) {
                val label = measurer.measure(date.format(MonthYear), small)
                drawText(label, topLeft = Offset((x(index) - label.size.width / 2f).coerceIn(left, size.width - label.size.width), bottom + 6.dp.toPx()))
            }
        }
        val avg = measurer.measure("Ø ${decimal(mean)}", small)
        drawText(avg, topLeft = Offset(size.width - avg.size.width, y(mean.toFloat()) - avg.size.height - 1.dp.toPx()))
    }
}

/** Für die letzten Zyklen: Wann wurde die Periode erwartet, und wann kam sie tatsächlich. */
@Composable
fun PredictionCard(state: AppState) {
    val checks = remember(state.model) { predictionChecks(state.model.periods) }
    val summary = PredictionSummary(checks)
    BloomCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Vorhersage gegen Wirklichkeit", style = MaterialTheme.typography.titleMedium)
            if (checks.isNotEmpty()) {
                Text("${summary.hits} von ${checks.size} im Fenster", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (checks.isEmpty()) {
            Text(
                "Ab dem zweiten Periodenbeginn zeigt die App hier, wie gut die Vorhersage getroffen hat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            return@BloomCard
        }
        Text(
            "Ø Abweichung ${decimal(summary.meanAbsoluteDeviation ?: 0.0)} Tage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        val shown = checks.takeLast(MAX_CHECKS)
        val span = shown.maxOf { check -> max(abs(check.deviation), max(daysBetween(check.expectedStart, check.latest), daysBetween(check.earliest, check.expectedStart))) } + 1
        shown.forEach { PredictionRow(it, span) }
        if (shown.any { it.usesDefaults }) {
            Text(
                "* mit Standardwerten berechnet, zu wenig frühere Zyklen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            "Balken: erwartetes Fenster, Punkt: tatsächlicher Beginn",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun PredictionRow(check: PredictionCheck, span: Int) {
    val scheme = MaterialTheme.colorScheme
    val deviation = check.deviation
    val verdict = when {
        deviation == 0 -> "pünktlich"
        deviation > 0 -> "$deviation ${if (deviation == 1) "Tag" else "Tage"} später"
        else -> "${-deviation} ${if (-deviation == 1) "Tag" else "Tage"} früher"
    }
    Row(
        Modifier.fillMaxWidth().height(34.dp).semantics {
            contentDescription = "Beginn am ${check.actualStart.format(DayMonth)}, $verdict, ${if (check.hit) "im erwarteten Fenster" else "außerhalb des erwarteten Fensters"}"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(check.actualStart.format(DayMonth), Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium.merge(Tabular))
        Canvas(Modifier.weight(1f).height(20.dp)) {
            val unit = size.width / (2f * span)
            val center = size.width / 2f
            val windowLeft = center - daysBetween(check.earliest, check.expectedStart) * unit
            val windowRight = center + daysBetween(check.expectedStart, check.latest) * unit
            drawRoundRect(
                scheme.primaryContainer,
                topLeft = Offset(windowLeft, size.height / 2 - 5.dp.toPx()),
                size = Size(windowRight - windowLeft, 10.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx()),
            )
            drawLine(scheme.outline, Offset(center, 2.dp.toPx()), Offset(center, size.height - 2.dp.toPx()), 1.dp.toPx())
            drawCircle(if (check.hit) scheme.primary else scheme.error, 5.dp.toPx(), Offset(center + deviation * unit, size.height / 2))
        }
        Text(
            verdict + if (check.usesDefaults) "*" else "",
            Modifier.width(96.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall.merge(Tabular),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
