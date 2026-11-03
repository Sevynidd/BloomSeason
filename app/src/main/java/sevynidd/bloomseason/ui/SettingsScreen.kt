package sevynidd.bloomseason.ui

import android.Manifest
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.remember
import android.app.StatusBarManager
import android.app.TimePickerDialog
import android.content.ComponentName
import android.graphics.drawable.Icon as AndroidIcon
import androidx.compose.material.icons.outlined.Widgets
import sevynidd.bloomseason.R
import sevynidd.bloomseason.tile.PeriodTileService
import android.content.pm.ApplicationInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import sevynidd.bloomseason.reminder.ReminderScheduler
import sevynidd.bloomseason.reminder.parseTime
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sevynidd.bloomseason.data.AppSettings
import sevynidd.bloomseason.data.backup.BackupRepository
import sevynidd.bloomseason.ui.theme.Tabular

@Composable
fun SettingsScreen(
    settings: AppSettings,
    backup: BackupRepository,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onExternal: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onLoadSample: () -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) onMessage("Ohne Benachrichtigungen kann die App nicht erinnern. Erlaube sie in den Systemeinstellungen.")
    }
    // Beim Einschalten einer Erinnerung wird einmal die Berechtigung für Benachrichtigungen erfragt.
    fun remind(enabled: Boolean, change: (AppSettings) -> AppSettings) {
        onChange(change)
        if (enabled && !ReminderScheduler.canNotify(context)) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun pickTime() {
        val time = parseTime(settings.dailyTime)
        TimePickerDialog(
            context,
            { _, hour, minute -> onChange { it.copy(dailyTime = "%02d:%02d".format(hour, minute)) } },
            time.hour, time.minute, true,
        ).show()
    }

    // Android fragt selbst nach, ob die Kachel in die Schnelleinstellungen soll.
    fun addTile() {
        val manager = context.getSystemService(StatusBarManager::class.java)
        manager.requestAddTileService(
            ComponentName(context, PeriodTileService::class.java),
            "Periode",
            AndroidIcon.createWithResource(context, R.drawable.ic_notification),
            context.mainExecutor,
        ) { result ->
            when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> onMessage("Kachel hinzugefügt.")
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> onMessage("Die Kachel ist schon in den Schnelleinstellungen.")
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> Unit
                else -> onMessage("Die Kachel konnte nicht hinzugefügt werden. Ziehe sie in den Schnelleinstellungen manuell dazu.")
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppBar("Einstellungen")

        SectionLabel("Erinnerungen")
        SettingsGroup {
            SettingRow(Icons.Outlined.Notifications, "Periode erwartet", "2 Tage vor dem Zeitfenster") {
                Switch(settings.remindExpected, { v -> remind(v) { it.copy(remindExpected = v) } })
            }
            SettingRow(Icons.Outlined.Schedule, "Täglich eintragen", "${settings.dailyTime} Uhr, tippen zum Ändern", ::pickTime) {
                Switch(settings.remindDaily, { v -> remind(v) { it.copy(remindDaily = v) } })
            }
            SettingRow(Icons.Outlined.VisibilityOff, "Neutraler Text", "„Zeit für deinen Eintrag“ statt „Periode“") {
                Switch(settings.neutralText, { v -> onChange { it.copy(neutralText = v) } })
            }
        }

        SectionLabel("Sicherheit", Modifier.padding(top = 6.dp))
        SettingsGroup {
            SettingRow(Icons.Outlined.Lock, "App-Sperre", "Biometrie oder PIN") {
                Switch(settings.appLock, onAppLockChange)
            }
        }
        Text(
            "Die Erinnerungen kommen zur eingestellten Uhrzeit als Benachrichtigung; die tägliche nur, wenn für den Tag noch nichts eingetragen ist. Die App-Sperre nutzt die Bildschirmsperre deines Geräts und greift beim Öffnen und nach 15 Sekunden im Hintergrund.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        SectionLabel("Daten", Modifier.padding(top = 6.dp))
        SettingsGroup {
            BackupRows(settings, backup, onMessage, onExternal)
        }

        SectionLabel("App", Modifier.padding(top = 6.dp))
        SettingsGroup {
            SettingRow(Icons.Outlined.Widgets, "Schnellkachel", "Periodenbeginn heute mit einem Tipp eintragen", ::addTile) { Chevron() }
            SettingRow(Icons.Outlined.Info, "Version", version) {}
        }

        if (debuggable) {
            SectionLabel("Debug", Modifier.padding(top = 6.dp))
            SettingsGroup {
                SettingRow(Icons.Outlined.Science, "Beispieldaten laden", "ersetzt alle Einträge durch erfundene Daten", onLoadSample) { Chevron() }
                SettingRow(Icons.Outlined.DeleteOutline, "Alle Daten löschen", "nur in Debug-Builds sichtbar", onClearAll) { Chevron() }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Alle Daten bleiben auf diesem Gerät. Die App hat keinen Internetzugriff.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    BloomCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) { content() }
}

@Composable
internal fun Chevron() {
    Icon(Icons.Outlined.ChevronRight, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall.merge(Tabular), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing()
        }
    }
}
