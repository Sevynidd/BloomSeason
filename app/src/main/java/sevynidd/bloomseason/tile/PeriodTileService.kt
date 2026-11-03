package sevynidd.bloomseason.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sevynidd.bloomseason.MainActivity
import sevynidd.bloomseason.R
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.data.BloomDatabase
import sevynidd.bloomseason.data.DayEntryEntity
import sevynidd.bloomseason.data.SettingsStore
import sevynidd.bloomseason.widget.BloomWidget

/**
 * Kachel in den Schnelleinstellungen: Ein Tipp trägt den Beginn der Periode für heute ein. Läuft die Periode
 * schon, öffnet die Kachel stattdessen die App.
 */
class PeriodTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        scope.launch { refresh() }
    }

    override fun onClick() {
        unlockAndRun {
            scope.launch {
                if (periodRunsToday()) {
                    val open = PendingIntent.getActivity(
                        this@PeriodTileService, 0, Intent(this@PeriodTileService, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    startActivityAndCollapse(open)
                } else {
                    BloomDatabase.get(this@PeriodTileService).dao()
                        .upsertDay(DayEntryEntity(LocalDate.now().toEpochDay(), Bleeding.MEDIUM.level))
                    BloomWidget.refresh(this@PeriodTileService)
                    refresh()
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun periodRunsToday(): Boolean {
        val today = LocalDate.now().toEpochDay()
        return BloomDatabase.get(this).dao().allDays().any { it.epochDay == today && Bleeding.fromLevel(it.bleeding).isPeriodDay }
    }

    private suspend fun refresh() {
        val running = periodRunsToday()
        val neutral = SettingsStore(this).settings.first().neutralText
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification)
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (neutral) "BloomSeason" else "Periode"
        tile.subtitle = when {
            neutral -> if (running) "eingetragen" else "Heute eintragen"
            running -> "läuft"
            else -> "Heute beginnen"
        }
        tile.updateTile()
    }
}
