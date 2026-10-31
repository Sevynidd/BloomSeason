package sevynidd.bloomseason.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.core.PhaseSpan
import sevynidd.bloomseason.ui.theme.neutral
import sevynidd.bloomseason.ui.theme.seasonColors

private val FullDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

fun LocalDate.fullLabel(): String = format(FullDate)

/** Kurzform ohne Punkt hinter dem Wochentag: "Mi, 14. Oktober". */
fun LocalDate.shortLabel(): String {
    val weekday = dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.GERMAN).removeSuffix(".")
    return "$weekday, $dayOfMonth. ${month.getDisplayName(JavaTextStyle.FULL, Locale.GERMAN)}"
}

fun decimal(value: Double): String = String.format(Locale.GERMAN, "%.1f", value)

val Bleeding.label: String
    get() = when (this) {
        Bleeding.NONE -> "keine"
        Bleeding.SPOTTING -> "Schmier"
        Bleeding.LIGHT -> "leicht"
        Bleeding.MEDIUM -> "mittel"
        Bleeding.HEAVY -> "stark"
    }

@Composable
fun BloomCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = if (subtitle) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
            color = if (subtitle) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
        actions()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
fun ConfidenceMeter(bars: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        listOf(6, 10, 14).forEachIndexed { index, height ->
            Box(
                Modifier.width(4.dp).height(height.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (index < bars) MaterialTheme.colorScheme.primary else neutral.bar),
            )
        }
    }
}

/** Tropfen mit Füllstand für die Blutungsstärke. */
@Composable
fun BleedingDrop(level: Bleeding, tint: Color, modifier: Modifier = Modifier) {
    val drop = remember { PathParser().parsePathString(DROP_PATH).toPath() }
    Canvas(modifier.size(28.dp)) {
        scale(size.width / 24f, pivot = Offset.Zero) {
            val fillFrom = when (level) {
                Bleeding.LIGHT -> 15.5f
                Bleeding.MEDIUM -> 11f
                Bleeding.HEAVY -> 0f
                else -> null
            }
            if (fillFrom != null) clipRect(top = fillFrom) { drawPath(drop, tint) }
            drawPath(drop, tint, style = Stroke(1.6f, join = StrokeJoin.Round))
            when (level) {
                Bleeding.SPOTTING -> drawCircle(tint, 1.6f, Offset(12f, 16f))
                Bleeding.NONE -> drawLine(tint, Offset(7.5f, 17.5f), Offset(16.5f, 8.5f), 1.6f, StrokeCap.Round)
                else -> Unit
            }
        }
    }
}

/**
 * Jahreslauf des aktuellen Zyklus: vier Jahreszeiten-Segmente, ein Marker für heute
 * und das Zeitfenster, in dem die nächste Periode erwartet wird. Tage zählen ab 1.
 */
@Composable
fun YearRun(
    spans: List<PhaseSpan>,
    cycleLength: Int,
    windowFrom: Int,
    windowTo: Int,
    currentDay: Int,
    modifier: Modifier = Modifier,
) {
    val colors = seasonColors
    val axis = maxOf(cycleLength + 3, windowTo, currentDay).toFloat()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val ink = MaterialTheme.colorScheme.onSurface
    val halo = MaterialTheme.colorScheme.surfaceContainer
    val description = "Jahreslauf: Zyklustag $currentDay von $cycleLength. " +
        "Nächste Periode erwartet um Tag $windowFrom bis $windowTo."
    BoxWithConstraints(modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = description }) {
        val width = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            fun x(day: Float) = day / axis * size.width
            val gap = 2.dp.toPx()
            spans.forEach { span ->
                val left = x(span.firstDay - 1f) + if (span.firstDay > 1) gap / 2 else 0f
                val right = x(span.lastDay.toFloat()) - if (span.lastDay < cycleLength) gap / 2 else 0f
                drawRoundRect(
                    color = colors.of(span.season).accent,
                    topLeft = Offset(left, 26.dp.toPx()),
                    size = Size(right - left, 10.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            }
            drawLine(
                color = outline,
                start = Offset(x(cycleLength.toFloat()) + 3.dp.toPx(), 31.dp.toPx()),
                end = Offset(size.width, 31.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx())),
            )
            val windowLeft = x(windowFrom - 1f)
            val windowRight = x(windowTo.toFloat())
            clipRect(top = 0f, bottom = 20.dp.toPx()) {
                drawRoundRect(
                    color = colors.winter.accent,
                    topLeft = Offset(windowLeft, 6.dp.toPx()),
                    size = Size(windowRight - windowLeft, 30.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            val markerX = x(currentDay - 0.5f)
            drawRoundRect(halo, Offset(markerX - 6.dp.toPx(), 15.dp.toPx()), Size(12.dp.toPx(), 32.dp.toPx()), CornerRadius(6.dp.toPx()))
            drawRoundRect(ink, Offset(markerX - 3.dp.toPx(), 18.dp.toPx()), Size(6.dp.toPx(), 26.dp.toPx()), CornerRadius(3.dp.toPx()))
        }
        val small = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
        Text(
            "Periode erwartet",
            style = small,
            color = muted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(150.dp).offset(x = width * ((windowFrom - 1f) / axis) - 156.dp, y = 2.dp),
        )
        Text("Tag 1", style = small, color = muted, modifier = Modifier.offset(y = 42.dp))
        Text(
            "$cycleLength",
            style = small,
            color = muted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp).offset(x = width * (cycleLength / axis) - 40.dp, y = 42.dp),
        )
    }
}
