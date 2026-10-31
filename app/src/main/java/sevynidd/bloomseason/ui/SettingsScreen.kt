package sevynidd.bloomseason.ui

import android.content.pm.ApplicationInfo
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
import sevynidd.bloomseason.ui.theme.Tabular

private const val COMING_LATER = "Folgt in einer späteren Version."

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onMessage: (String) -> Unit,
    onLoadSample: () -> Unit,
    onClearAll: () -> Unit,
) {
    val debuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppBar("Einstellungen")

        SectionLabel("Erinnerungen")
        SettingsGroup {
            SettingRow(Icons.Outlined.Notifications, "Periode erwartet", "2 Tage vor dem Zeitfenster") {
                Switch(settings.remindExpected, { v -> onChange { it.copy(remindExpected = v) } })
            }
            SettingRow(Icons.Outlined.Schedule, "Täglich eintragen", "${settings.dailyTime} Uhr") {
                Switch(settings.remindDaily, { v -> onChange { it.copy(remindDaily = v) } })
            }
            SettingRow(Icons.Outlined.VisibilityOff, "Neutraler Text", "„Zeit für deinen Eintrag“ statt „Periode“") {
                Switch(settings.neutralText, { v -> onChange { it.copy(neutralText = v) } })
            }
        }

        SectionLabel("Sicherheit", Modifier.padding(top = 6.dp))
        SettingsGroup {
            SettingRow(Icons.Outlined.Lock, "App-Sperre", "Biometrie oder PIN") {
                Switch(settings.appLock, { v -> onChange { it.copy(appLock = v) } })
            }
        }
        Text(
            "Erinnerungen und App-Sperre werden gespeichert, aber noch nicht ausgelöst.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        SectionLabel("Daten", Modifier.padding(top = 6.dp))
        SettingsGroup {
            SettingRow(Icons.Outlined.FolderOpen, "Backup-Ordner", "noch nicht festgelegt", { onMessage(COMING_LATER) }) { Chevron() }
            SettingRow(Icons.Outlined.Download, "Backup erstellen", "verschlüsselt", { onMessage(COMING_LATER) }) { Chevron() }
            SettingRow(Icons.Outlined.Upload, "Backup einspielen", null, { onMessage(COMING_LATER) }) { Chevron() }
            SettingRow(Icons.Outlined.Description, "Exportieren / Importieren", "JSON oder CSV", { onMessage(COMING_LATER) }) { Chevron() }
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
                "Alle Daten bleiben auf diesem Gerät.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    BloomCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) { content() }
}

@Composable
private fun Chevron() {
    Icon(Icons.Outlined.ChevronRight, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingRow(
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
