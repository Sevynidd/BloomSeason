package sevynidd.bloomseason

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate
import kotlinx.coroutines.launch
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.ui.CalendarScreen
import sevynidd.bloomseason.ui.DayEditScreen
import sevynidd.bloomseason.ui.SettingsScreen
import sevynidd.bloomseason.ui.StatsScreen
import sevynidd.bloomseason.ui.TodayScreen
import sevynidd.bloomseason.ui.theme.BloomSeasonTheme
import sevynidd.bloomseason.ui.theme.neutral

private data class Destination(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val Destinations = listOf(
    Destination("Heute", Icons.Outlined.Home),
    Destination("Kalender", Icons.Outlined.CalendarMonth),
    Destination("Statistik", Icons.Outlined.BarChart),
    Destination("Einstellungen", Icons.Outlined.Tune),
)

@Composable
fun BloomApp(state: AppState, viewModel: AppViewModel) {
    // Die aktuelle Jahreszeit färbt die ganze Oberfläche; ohne Daten gilt der Frühling.
    val season = state.model.phaseOn(state.today)?.season ?: Season.SPRING
    BloomSeasonTheme(season) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var editingEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val editing = editingEpoch?.let(LocalDate::ofEpochDay)

        BackHandler(enabled = editing != null) { editingEpoch = null }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = neutral.nav) {
                    Destinations.forEachIndexed { index, destination ->
                        // Nur Icons; der Name steht für Screenreader in der contentDescription.
                        NavigationBarItem(
                            selected = index == tab || (editing != null && index == 1),
                            onClick = { editingEpoch = null; tab = index },
                            icon = { Icon(destination.icon, contentDescription = destination.name) },
                            label = null,
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (editing != null) {
                    DayEditScreen(
                        date = editing,
                        state = state,
                        onSave = { bleeding, pain ->
                            viewModel.saveDay(editing, bleeding, pain)
                            editingEpoch = null
                        },
                        onClose = { editingEpoch = null },
                    )
                } else {
                    when (tab) {
                        0 -> TodayScreen(
                            state = state,
                            onAddToday = { editingEpoch = state.today.toEpochDay() },
                            onStartPeriod = viewModel::startPeriodToday,
                        )
                        1 -> CalendarScreen(state) { editingEpoch = it.toEpochDay() }
                        2 -> StatsScreen(state)
                        else -> SettingsScreen(
                            settings = state.settings,
                            onChange = viewModel::updateSettings,
                            onMessage = { scope.launch { snackbar.showSnackbar(it) } },
                            onLoadSample = viewModel::loadSampleData,
                            onClearAll = viewModel::clearAllData,
                        )
                    }
                }
            }
        }
    }
}
