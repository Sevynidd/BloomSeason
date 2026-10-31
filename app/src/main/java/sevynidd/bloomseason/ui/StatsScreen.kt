package sevynidd.bloomseason.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import sevynidd.bloomseason.AppState
import sevynidd.bloomseason.core.CycleStats
import sevynidd.bloomseason.core.MIN_CYCLES_FOR_ESTIMATE
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.ui.theme.Tabular
import sevynidd.bloomseason.ui.theme.neutral
import sevynidd.bloomseason.ui.theme.seasonColors

@Composable
fun StatsScreen(state: AppState) {
    val stats = state.model.stats
    val painBySeason = remember(state.model, state.pain) { state.model.painBySeason(state.painSamples) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppBar("Statistik")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Ø Zyklus", stats.meanCycle, stats.sdCycle, Modifier.weight(1f))
            Kpi("Ø Periode", stats.meanPeriod, stats.sdPeriod, Modifier.weight(1f))
        }

        BloomCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Zykluslängen", style = MaterialTheme.typography.titleMedium)
                Text("letzte ${stats.cycles.size.coerceAtLeast(1)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (stats.cycles.isEmpty()) {
                Text(
                    "Sobald zwei Periodenbeginne erfasst sind, erscheint hier die erste Zykluslänge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                CycleChart(stats)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(width = 14.dp, height = 10.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primaryContainer))
                    Text("üblicher Bereich", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        BloomCard {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Schmerzen je Jahreszeit", style = MaterialTheme.typography.titleMedium)
                Text("Ø Stärke", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Season.entries.forEach { season -> PainRow(season, painBySeason[season]) }
        }

        val footer = if (stats.cycles.size >= MIN_CYCLES_FOR_ESTIMATE) {
            "Aus ${stats.cycles.size} Zyklen · ein Muster, keine Diagnose"
        } else {
            "Aussagekräftig ab etwa $MIN_CYCLES_FOR_ESTIMATE Zyklen · ein Muster, keine Diagnose"
        }
        Text(
            footer,
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Kpi(label: String, mean: Double?, sd: Double?, modifier: Modifier) {
    BloomCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(mean?.roundToInt()?.toString() ?: "–", style = MaterialTheme.typography.displaySmall.merge(Tabular))
            Text(" Tage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (sd != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Text("± ${decimal(sd)}", style = MaterialTheme.typography.labelMedium.merge(Tabular), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun CycleChart(stats: CycleStats) {
    val scheme = MaterialTheme.colorScheme
    val barColor = neutral.bar
    val measurer = rememberTextMeasurer()
    val mean = stats.meanCycle ?: 0.0
    val sd = stats.sdCycle ?: 0.0
    val description = "Zykluslängen: " + stats.cycles.joinToString(", ") {
        "${it.start.month.getDisplayName(JavaTextStyle.FULL, Locale.GERMAN)} ${it.days} Tage"
    } + ". Durchschnitt ${decimal(mean)}, üblicher Bereich ${decimal(mean - sd)} bis ${decimal(mean + sd)} Tage."
    Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
        val lengths = stats.cycles.map { it.days }
        val low = floor(minOf(lengths.min().toDouble(), mean - sd)).toFloat() - 6f
        val high = ceil(maxOf(lengths.max().toDouble(), mean + sd)).toFloat() + 1f
        val baseline = 124.dp.toPx()
        val top = 24.dp.toPx()
        fun y(days: Float) = baseline - (days - low) / (high - low) * (baseline - top)

        drawRoundRect(
            color = scheme.primaryContainer,
            topLeft = Offset(0f, y((mean + sd).toFloat())),
            size = Size(size.width, y((mean - sd).toFloat()) - y((mean + sd).toFloat())),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
        drawLine(
            scheme.primary,
            Offset(0f, y(mean.toFloat())),
            Offset(size.width, y(mean.toFloat())),
            1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        val small = TextStyle(color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        val label = measurer.measure("Ø ${decimal(mean)}", small)
        drawText(label, topLeft = Offset(size.width - label.size.width, 0f))

        val slot = size.width / 6
        val barWidth = minOf(30.dp.toPx(), slot * 0.7f)
        stats.cycles.forEachIndexed { index, cycle ->
            val centerX = slot * index + slot / 2
            val barTop = y(cycle.days.toFloat())
            drawRoundRect(
                color = if (index == stats.cycles.lastIndex) scheme.primary else barColor,
                topLeft = Offset(centerX - barWidth / 2, barTop),
                size = Size(barWidth, baseline - barTop),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            val value = measurer.measure("${cycle.days}", TextStyle(color = scheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            drawText(value, topLeft = Offset(centerX - value.size.width / 2f, barTop - value.size.height - 2.dp.toPx()))
            // Startdatum statt Monatsname, damit zwei Zyklen im selben Monat unterscheidbar bleiben.
            val name = measurer.measure("${cycle.start.dayOfMonth}.${cycle.start.monthValue}.", small)
            drawText(name, topLeft = Offset(centerX - name.size.width / 2f, baseline + 6.dp.toPx()))
        }
    }
}

@Composable
private fun PainRow(season: Season, value: Double?) {
    val palette = seasonColors.of(season)
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(season.glyph, contentDescription = null, Modifier.size(20.dp), tint = palette.accent)
        Text(season.label, Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            if (value != null) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((value / 10).toFloat().coerceIn(0.02f, 1f)).clip(RoundedCornerShape(5.dp)).background(palette.accent))
            }
        }
        Text(
            value?.let { decimal(it) } ?: "–",
            Modifier.width(30.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium.merge(Tabular),
            fontWeight = FontWeight.Medium,
        )
    }
}
