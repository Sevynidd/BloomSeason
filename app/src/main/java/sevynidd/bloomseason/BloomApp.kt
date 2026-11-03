package sevynidd.bloomseason

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kotlinx.coroutines.launch
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.security.AppLock
import sevynidd.bloomseason.ui.CalendarScreen
import sevynidd.bloomseason.ui.DayEditScreen
import sevynidd.bloomseason.ui.LockScreen
import sevynidd.bloomseason.ui.SettingsScreen
import sevynidd.bloomseason.ui.StatsScreen
import sevynidd.bloomseason.ui.TodayScreen
import sevynidd.bloomseason.ui.theme.BloomSeasonTheme
import sevynidd.bloomseason.ui.theme.neutral

private data class Destination(val name: String, val icon: ImageVector)

private val Destinations = listOf(
    Destination("Heute", Icons.Outlined.Home),
    Destination("Kalender", Icons.Outlined.CalendarMonth),
    Destination("Statistik", Icons.Outlined.BarChart),
    Destination("Einstellungen", Icons.Outlined.Tune),
)

private fun monthIndexOf(date: LocalDate) = date.year * 12 + date.monthValue - 1

@Composable
fun BloomApp(state: AppState, viewModel: AppViewModel) {
    // Die aktuelle Jahreszeit färbt die ganze Oberfläche; ohne Daten gilt der Frühling.
    val season = state.model.phaseOn(state.today)?.season ?: Season.SPRING
    BloomSeasonTheme(season) {
        val context = LocalContext.current
        val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var editingEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
        // Der Kalender-Zustand liegt hier, damit er das Öffnen des Eintrags-Bildschirms überlebt.
        var calendarMonth by rememberSaveable { mutableIntStateOf(monthIndexOf(state.today)) }
        var calendarSelected by rememberSaveable { mutableLongStateOf(state.today.toEpochDay()) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val editing = editingEpoch?.let(LocalDate::ofEpochDay)

        BackHandler(enabled = editing != null) { editingEpoch = null }

        fun changeAppLock(enable: Boolean) {
            when {
                !enable -> viewModel.updateSettings { it.copy(appLock = false) }
                !AppLock.isAvailable(context) -> scope.launch {
                    snackbar.showSnackbar("Richte zuerst eine Bildschirmsperre auf deinem Gerät ein.")
                }
                else -> {
                    viewModel.setAuthenticating(true)
                    AppLock.authenticate(context, "App-Sperre aktivieren") { success ->
                        viewModel.setAuthenticating(false)
                        if (success) viewModel.updateSettings { it.copy(appLock = true) }
                    }
                }
            }
        }

        when {
            !state.ready -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            state.settings.appLock && !unlocked -> LockScreen(
                onUnlocked = viewModel::unlock,
                onDisableLock = { viewModel.updateSettings { it.copy(appLock = false) } },
                onAuthenticating = viewModel::setAuthenticating,
            )
            else -> Scaffold(
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
                                calendarSelected = editing.toEpochDay()
                                calendarMonth = monthIndexOf(editing)
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
                            1 -> CalendarScreen(
                                state = state,
                                monthIndex = calendarMonth,
                                onMonthChange = { calendarMonth = it },
                                selected = LocalDate.ofEpochDay(calendarSelected),
                                onSelect = { calendarSelected = it.toEpochDay() },
                                onEdit = { editingEpoch = it.toEpochDay() },
                                onEndPeriod = viewModel::endPeriodOn,
                            )
                            2 -> StatsScreen(state)
                            else -> SettingsScreen(
                                settings = state.settings,
                                backup = viewModel.backup,
                                onExternal = viewModel::setAuthenticating,
                                onChange = viewModel::updateSettings,
                                onAppLockChange = ::changeAppLock,
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
}
