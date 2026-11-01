package sevynidd.bloomseason.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import sevynidd.bloomseason.core.Bleeding

private val Strengths = listOf(Bleeding.LIGHT, Bleeding.MEDIUM, Bleeding.HEAVY)

private fun LocalDate.toPickerMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toPickerDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Trägt für einen gewählten Zeitraum in der Vergangenheit an jedem Tag dieselbe Blutungsstärke ein. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodRangeDialog(
    today: LocalDate,
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate, bleeding: Bleeding) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial.takeIf { !it.isAfter(today) }?.toPickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !utcTimeMillis.toPickerDate().isAfter(today)
        },
    )
    var bleeding by remember { mutableStateOf(Bleeding.MEDIUM) }
    val start = state.selectedStartDateMillis?.toPickerDate()
    val end = state.selectedEndDateMillis?.toPickerDate() ?: start

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                DateRangePicker(
                    state,
                    modifier = Modifier.weight(1f),
                    title = { Text("Periode nachtragen", Modifier.padding(start = 24.dp, top = 16.dp), style = MaterialTheme.typography.titleLarge) },
                    headline = {
                        val text = when {
                            start == null -> "Wähle den ersten Tag"
                            end == start -> start.fullLabel()
                            else -> "${start.fullLabel()} bis ${end!!.fullLabel()}"
                        }
                        Text(text, Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.bodyLarge)
                    },
                )
                Text(
                    "Blutungsstärke für alle gewählten Tage",
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Strengths.forEach { level ->
                        FilterChip(selected = bleeding == level, onClick = { bleeding = level }, label = { Text(level.label) })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text("Abbrechen") }
                    TextButton(
                        onClick = { if (start != null && end != null) onSave(start, end, bleeding) },
                        enabled = start != null,
                    ) { Text("Eintragen") }
                }
            }
        }
    }
}
