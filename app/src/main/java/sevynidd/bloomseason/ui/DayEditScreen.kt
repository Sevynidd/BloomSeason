package sevynidd.bloomseason.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt
import sevynidd.bloomseason.AppState
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.ui.theme.Tabular
import sevynidd.bloomseason.ui.theme.seasonColors

private val BaseLocations = listOf("Bauch", "Kopf", "Rücken")
private const val DEFAULT_INTENSITY = 4

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayEditScreen(
    date: LocalDate,
    state: AppState,
    onSave: (Bleeding, Map<String, Int>) -> Unit,
    onClose: () -> Unit,
) {
    var bleeding by remember(date) { mutableStateOf(state.bleeding[date] ?: Bleeding.NONE) }
    val pain = remember(date) {
        mutableStateMapOf<String, Int>().apply { state.pain[date].orEmpty().forEach { put(it.location, it.intensity) } }
    }
    var active by remember(date) { mutableStateOf(pain.keys.firstOrNull()) }
    val addedLocations = remember(date) { mutableStateListOf<String>() }
    var showAddDialog by remember { mutableStateOf(false) }
    val locations = (BaseLocations + state.knownLocations + addedLocations + pain.keys).distinct()
    val phase = state.model.phaseOn(date)

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose) { Icon(Icons.Outlined.Close, contentDescription = "Schließen") }
                Text(date.shortLabel(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
            }
            if (phase != null) {
                val palette = seasonColors.of(phase.season)
                Surface(color = palette.container, contentColor = palette.onContainer, shape = RoundedCornerShape(14.dp)) {
                    Row(
                        Modifier.height(28.dp).padding(start = 8.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(phase.season.glyph, contentDescription = null, Modifier.size(16.dp))
                        Text("${phase.season.label} · Zyklustag ${phase.cycleDay}", style = MaterialTheme.typography.labelMedium.merge(Tabular))
                    }
                }
            }

            SectionLabel("Blutung")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Bleeding.entries.forEach { level ->
                    BleedingChoice(level, level == bleeding, Modifier.weight(1f)) { bleeding = level }
                }
            }

            SectionLabel("Schmerzen", Modifier.padding(top = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                locations.forEach { location ->
                    val selected = location in pain
                    FilterChip(
                        selected = selected,
                        onClick = {
                            when {
                                !selected -> { pain[location] = DEFAULT_INTENSITY; active = location }
                                active != location -> active = location
                                else -> { pain.remove(location); active = pain.keys.firstOrNull() }
                            }
                        },
                        label = { Text(location) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    )
                }
                AssistChip(
                    onClick = { showAddDialog = true },
                    label = { Text("Ort") },
                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(18.dp)) },
                )
            }

            val current = active
            if (current != null && current in pain) {
                BloomCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("Stärke · $current", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${pain[current]}", style = MaterialTheme.typography.displaySmall.merge(Tabular))
                            Text(" / 10", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                    Slider(
                        value = (pain[current] ?: DEFAULT_INTENSITY).toFloat(),
                        onValueChange = { pain[current] = it.roundToInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveTickColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0 · keine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10 · sehr stark", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Jeder gewählte Ort hat eine eigene Stärke. Tippe auf einen gewählten Ort, um ihn zu wechseln, und nochmals, um ihn zu entfernen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        Button(
            onClick = { onSave(bleeding, pain.toMap()) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Speichern", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Neuer Ort") },
            text = {
                OutlinedTextField(name, { name = it.take(24) }, singleLine = true, label = { Text("z. B. Brust") })
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = name.trim()
                        if (clean.isNotEmpty()) {
                            addedLocations += clean
                            pain[clean] = DEFAULT_INTENSITY
                            active = clean
                        }
                        showAddDialog = false
                    },
                ) { Text("Hinzufügen") }
            },
            dismissButton = { TextButton({ showAddDialog = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun BleedingChoice(level: Bleeding, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    Surface(
        color = if (selected) scheme.primaryContainer else scheme.surfaceContainer,
        contentColor = content,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) scheme.primary else Color0),
        modifier = modifier.height(84.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
            BleedingDrop(level, content)
            Text(level.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private val Color0 = androidx.compose.ui.graphics.Color.Transparent
