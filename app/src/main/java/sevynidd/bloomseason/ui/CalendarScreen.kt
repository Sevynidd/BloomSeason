package sevynidd.bloomseason.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import sevynidd.bloomseason.core.Bleeding
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import sevynidd.bloomseason.AppState
import sevynidd.bloomseason.core.Season
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import sevynidd.bloomseason.ui.theme.SeasonColors
import sevynidd.bloomseason.ui.theme.SeasonPalette
import sevynidd.bloomseason.ui.theme.Tabular
import sevynidd.bloomseason.ui.theme.seasonColors

private enum class Band { NONE, PERIOD, SUMMER }

private data class CellInfo(
    val date: LocalDate,
    val inMonth: Boolean,
    val band: Band,
    val bleeding: Bleeding?,
    val predicted: Boolean,
    val hasPain: Boolean,
    val season: Season?,
    val future: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    state: AppState,
    monthIndex: Int,
    onMonthChange: (Int) -> Unit,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onEdit: (LocalDate) -> Unit,
) {
    val today = state.today
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val colors = seasonColors
    val currentIndex by rememberUpdatedState(monthIndex)

    Column(
        Modifier.fillMaxSize()
            // Nach links wischen: nächster Monat, nach rechts: vorheriger.
            .pointerInput(Unit) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag < -80f) onMonthChange(currentIndex + 1) else if (drag > 80f) onMonthChange(currentIndex - 1)
                    },
                    onHorizontalDrag = { _, amount -> drag += amount },
                )
            }
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppBar("${month.month.getDisplayName(JavaTextStyle.FULL, Locale.GERMAN)} ${month.year}") {
            Row {
                IconButton({ onMonthChange(monthIndex - 1) }) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Vorheriger Monat") }
                IconButton({ onMonthChange(monthIndex + 1) }) { Icon(Icons.Outlined.ChevronRight, contentDescription = "Nächster Monat") }
            }
        }

        val first = month.atDay(1)
        val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
        val weeks = (first.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7
        val cells = (0 until weeks * 7).map { cellInfo(state, gridStart.plusDays(it.toLong()), month) }

        Row(Modifier.fillMaxWidth()) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach {
                Text(
                    it,
                    Modifier.weight(1f).padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val index = week * 7 + column
                    val cell = cells[index]
                    val previous = cells.getOrNull(index - 1)?.takeIf { column > 0 }
                    val next = cells.getOrNull(index + 1)?.takeIf { column < 6 }
                    Box(Modifier.weight(1f)) {
                        DayCell(
                            cell = cell,
                            colors = colors,
                            isToday = cell.date == today,
                            isSelected = cell.date == selected,
                            bandStart = previous?.band != cell.band,
                            bandEnd = next?.band != cell.band,
                            lineStart = previous?.season != cell.season,
                            lineEnd = next?.season != cell.season,
                            onClick = {
                                onSelect(cell.date)
                                if (!cell.inMonth) onMonthChange(cell.date.year * 12 + cell.date.monthValue - 1)
                            },
                        )
                    }
                }
            }
        }

        FlowRow(
            Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Bleeding.entries.filter { it.isPeriodDay }.forEach { level ->
                LegendItem("Periode ${level.label}") { Box(Modifier.size(14.dp).clip(CircleShape).background(periodColor(level, colors.winter))) }
            }
            LegendItem("erwartet") { Box(Modifier.size(14.dp).dashedCircle(colors.winter.accent)) }
            LegendItem("Sommer · Eisprung geschätzt") {
                Box(Modifier.size(14.dp).clip(CircleShape).background(colors.summer.container).border(1.dp, colors.summer.accent, CircleShape))
            }
            LegendItem("Jahreszeit") { SeasonLine(colors) }
            LegendItem("Schmerz notiert") { Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface)) }
        }

        Spacer(Modifier.height(4.dp))
        DaySheet(state, selected, onEdit)
        Spacer(Modifier.height(8.dp))
    }
}

private fun cellInfo(state: AppState, date: LocalDate, month: YearMonth): CellInfo {
    val model = state.model
    val phase = model.phaseOn(date)
    val prediction = model.prediction
    val period = state.bleeding[date]?.isPeriodDay == true
    val band = when {
        period -> Band.PERIOD
        phase?.season == Season.SUMMER -> Band.SUMMER
        else -> Band.NONE
    }
    val predicted = !period && prediction != null && date.isAfter(state.today.minusDays(1)) &&
        !date.isBefore(prediction.earliest) && !date.isAfter(prediction.latest)
    return CellInfo(
        date = date,
        inMonth = YearMonth.from(date) == month,
        band = band,
        bleeding = state.bleeding[date],
        predicted = predicted,
        hasPain = state.pain[date].orEmpty().isNotEmpty(),
        season = phase?.season,
        future = date.isAfter(state.today),
    )
}

@Composable
private fun DayCell(
    cell: CellInfo,
    colors: SeasonColors,
    isToday: Boolean,
    isSelected: Boolean,
    bandStart: Boolean,
    bandEnd: Boolean,
    lineStart: Boolean,
    lineEnd: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> scheme.background
        cell.band == Band.PERIOD -> readableOn(periodColor(cell.bleeding, colors.winter), colors.winter)
        cell.band == Band.SUMMER -> colors.summer.onContainer
        !cell.inMonth -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val bandColor = when (cell.band) {
        Band.PERIOD -> periodColor(cell.bleeding, colors.winter)
        Band.SUMMER -> colors.summer.container
        Band.NONE -> Color.Transparent
    }
    val bandShape: Shape = RoundedCornerShape(
        topStart = if (bandStart) 18.dp else 0.dp,
        bottomStart = if (bandStart) 18.dp else 0.dp,
        topEnd = if (bandEnd) 18.dp else 0.dp,
        bottomEnd = if (bandEnd) 18.dp else 0.dp,
    )
    val description = buildString {
        append(cell.date.fullLabel())
        if (cell.band == Band.PERIOD) append(", Periode, Blutung ${cell.bleeding?.label}")
        if (cell.predicted) append(", Periode erwartet")
        if (cell.band == Band.SUMMER) append(", Eisprung geschätzt")
        if (cell.hasPain) append(", Schmerz notiert")
    }
    Box(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).semantics { contentDescription = description },
    ) {
        if (cell.band != Band.NONE) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 4.dp, start = if (bandStart) 6.dp else 0.dp, end = if (bandEnd) 6.dp else 0.dp)
                    .fillMaxWidth().height(36.dp).clip(bandShape).background(bandColor),
            )
        }
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(36.dp)
                .then(if (isSelected) Modifier.clip(CircleShape).background(scheme.onSurface) else Modifier)
                .then(if (isToday && !isSelected) Modifier.border(2.dp, scheme.onSurface, CircleShape) else Modifier)
                .then(if (cell.predicted && !isSelected) Modifier.dashedCircle(colors.winter.accent) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${cell.date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium.merge(Tabular),
                fontWeight = if (cell.inMonth) FontWeight.Medium else FontWeight.Normal,
                color = textColor,
            )
        }
        if (cell.hasPain) {
            Box(Modifier.align(Alignment.TopCenter).offset(y = 27.dp).size(4.dp).clip(CircleShape).background(textColor))
        }
        cell.season?.let { season ->
            val alpha = if (cell.future) 0.45f else 1f
            val line = RoundedCornerShape(
                topStart = if (lineStart) 2.dp else 0.dp,
                bottomStart = if (lineStart) 2.dp else 0.dp,
                topEnd = if (lineEnd) 2.dp else 0.dp,
                bottomEnd = if (lineEnd) 2.dp else 0.dp,
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp, start = if (lineStart) 3.dp else 0.dp, end = if (lineEnd) 3.dp else 0.dp)
                    .fillMaxWidth().height(3.dp).clip(line).background(colors.of(season).accent.copy(alpha = alpha)),
            )
        }
    }
}

private fun Modifier.dashedCircle(color: Color): Modifier = drawBehind {
    val stroke = 1.5.dp.toPx()
    drawCircle(
        color = color,
        radius = size.minDimension / 2 - stroke / 2,
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

@Composable
private fun LegendItem(text: String, symbol: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        symbol()
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeasonLine(colors: SeasonColors) {
    Canvas(Modifier.width(18.dp).height(3.dp)) {
        val part = size.width / 4
        listOf(colors.winter, colors.spring, colors.summer, colors.autumn).forEachIndexed { i, palette ->
            drawLine(palette.accent, Offset(part * i, size.height / 2), Offset(part * (i + 1), size.height / 2), size.height)
        }
    }
}

@Composable
private fun DaySheet(state: AppState, date: LocalDate, onEdit: (LocalDate) -> Unit) {
    val phase = state.model.phaseOn(date)
    val bleeding = state.bleeding[date]
    val pain = state.pain[date].orEmpty()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).width(32.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(date.fullLabel(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (phase != null) {
                    val palette = seasonColors.of(phase.season)
                    Surface(color = palette.container, contentColor = palette.onContainer, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(start = 8.dp, end = 10.dp).height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(phase.season.glyph, contentDescription = null, Modifier.size(16.dp))
                            Text(
                                "${phase.season.label} · Tag ${phase.cycleDay}" + if (phase.isProjected) " (erwartet)" else "",
                                style = MaterialTheme.typography.labelMedium.merge(Tabular),
                            )
                        }
                    }
                }
            }
            SheetRow(Icons.Outlined.WaterDrop, "Blutung", bleeding?.takeIf { it != sevynidd.bloomseason.core.Bleeding.NONE }?.label ?: "keine")
            SheetRow(
                Icons.Outlined.Bolt,
                "Schmerzen",
                if (pain.isEmpty()) "–" else pain.joinToString(", ") { "${it.location} · ${it.intensity}/10" },
            )
            FilledTonalButton(
                onClick = { onEdit(date) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tag bearbeiten")
            }
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.merge(Tabular), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Je stärker die Blutung, desto kräftiger die Farbe: von der hellen Fläche bis zum vollen Akzent. */
private fun periodColor(bleeding: Bleeding?, palette: SeasonPalette): Color {
    val strength = when (bleeding) {
        Bleeding.LIGHT -> 0.3f
        Bleeding.HEAVY -> 1f
        else -> 0.65f
    }
    return lerp(palette.container, palette.accent, strength)
}

/** Wählt unter den beiden Textfarben der Palette die mit dem besseren Kontrast zum Untergrund. */
private fun readableOn(background: Color, palette: SeasonPalette): Color {
    fun contrast(text: Color): Float {
        val a = background.luminance() + 0.05f
        val b = text.luminance() + 0.05f
        return maxOf(a, b) / minOf(a, b)
    }
    return if (contrast(palette.onAccent) >= contrast(palette.onContainer)) palette.onAccent else palette.onContainer
}
