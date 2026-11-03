package sevynidd.bloomseason.ui

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import sevynidd.bloomseason.data.AppSettings
import sevynidd.bloomseason.data.backup.BackupException
import sevynidd.bloomseason.data.backup.BackupRepository
import sevynidd.bloomseason.data.backup.ExportFormat

private const val MIN_PASSWORD = 8

private sealed interface Dialog {
    data object CreatePassword : Dialog
    data object AutoPassword : Dialog
    data class RestorePassword(val file: Uri) : Dialog
    data object ExportChoice : Dialog
}

/** Die Zeilen der Gruppe „Daten“: Ordner, Sicherung erstellen und einspielen, Export und Import. */
@Composable
fun BackupRows(
    settings: AppSettings,
    backup: BackupRepository,
    onMessage: (String) -> Unit,
    /** true, solange eine Systemauswahl offen ist, damit die App-Sperre das nicht als Hintergrund zählt. */
    onExternal: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run(success: suspend () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            val message = try {
                success()
            } catch (e: BackupException) {
                e.message ?: "Fehlgeschlagen."
            } catch (e: java.io.IOException) {
                e.message ?: "Fehlgeschlagen."
            } catch (e: SecurityException) {
                "Kein Zugriff auf die Datei oder den Ordner."
            }
            busy = false
            onMessage(message)
        }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        onExternal(false)
        if (uri != null) run { backup.setFolder(uri); "Backup-Ordner gespeichert." }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onExternal(false)
        if (uri != null) dialog = Dialog.RestorePassword(uri)
    }
    val pickImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onExternal(false)
        if (uri != null) run { "${backup.import(uri)} Tage importiert." }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.JSON.mime)) { uri ->
        onExternal(false)
        if (uri != null) run { backup.export(uri, ExportFormat.JSON); "JSON exportiert." }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.CSV.mime)) { uri ->
        onExternal(false)
        if (uri != null) run { backup.export(uri, ExportFormat.CSV); "CSV exportiert." }
    }

    val folder = settings.backupFolder.takeIf { it.isNotEmpty() }?.let(Uri::parse)
    SettingRow(
        Icons.Outlined.FolderOpen,
        "Backup-Ordner",
        folder?.let(::folderName) ?: "noch nicht festgelegt",
        { onExternal(true); pickFolder.launch(folder) },
    ) { Chevron() }
    SettingRow(
        Icons.Outlined.Download,
        "Backup erstellen",
        lastBackupText(settings.lastBackup) ?: "verschlüsselt mit Passwort",
        { if (folder == null) onMessage("Lege zuerst einen Backup-Ordner fest.") else dialog = Dialog.CreatePassword },
    ) { Chevron() }
    SettingRow(Icons.Outlined.Autorenew, "Automatisch sichern", "wöchentlich, die letzten 8 bleiben") {
        Switch(settings.autoBackup, { on ->
            when {
                !on -> run { backup.disableAutomatic(); "Automatische Sicherung aus." }
                folder == null -> onMessage("Lege zuerst einen Backup-Ordner fest.")
                else -> dialog = Dialog.AutoPassword
            }
        })
    }
    SettingRow(
        Icons.Outlined.Upload,
        "Backup einspielen",
        "ersetzt alle Einträge",
        { onExternal(true); pickBackup.launch(arrayOf("*/*")) },
    ) { Chevron() }
    SettingRow(
        Icons.Outlined.Description,
        "Exportieren / Importieren",
        "JSON oder CSV, unverschlüsselt",
        { dialog = Dialog.ExportChoice },
    ) { Chevron() }

    when (val current = dialog) {
        Dialog.CreatePassword -> PasswordDialog(
            title = "Backup erstellen",
            text = "Das Passwort schützt die Datei. Ohne das Passwort lässt sie sich nicht wiederherstellen, und es wird nirgends gespeichert.",
            confirmRepeat = true,
            confirmLabel = "Erstellen",
            onDismiss = { dialog = null },
        ) { password ->
            dialog = null
            val target = folder ?: return@PasswordDialog
            run { "Gespeichert: ${backup.createBackup(target, password)}" }
        }
        Dialog.AutoPassword -> PasswordDialog(
            title = "Automatisch sichern",
            text = "Das Passwort schützt die Dateien. Damit die App im Hintergrund sichern kann, wird es verschlüsselt auf diesem Gerät gespeichert. Merke es dir trotzdem, du brauchst es zum Wiederherstellen.",
            confirmRepeat = true,
            confirmLabel = "Einschalten",
            onDismiss = { dialog = null },
        ) { password ->
            dialog = null
            val target = folder ?: return@PasswordDialog
            run { "Eingeschaltet, erste Sicherung: ${backup.enableAutomatic(target, password)}" }
        }
        is Dialog.RestorePassword -> PasswordDialog(
            title = "Backup einspielen",
            text = "Alle aktuellen Einträge werden durch den Inhalt der Sicherung ersetzt.",
            confirmRepeat = false,
            confirmLabel = "Einspielen",
            onDismiss = { dialog = null },
        ) { password ->
            dialog = null
            run { "${backup.restore(current.file, password)} Tage wiederhergestellt." }
        }
        Dialog.ExportChoice -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Exportieren / Importieren") },
            text = {
                Text(
                    "Exporte sind nicht verschlüsselt und enthalten deine Zyklusdaten im Klartext. Der Import ergänzt vorhandene Einträge; für importierte Tage gilt die Datei.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Column {
                    TextButton({ dialog = null; onExternal(true); exportJson.launch("bloomseason-export.json") }) { Text("Als JSON exportieren") }
                    TextButton({ dialog = null; onExternal(true); exportCsv.launch("bloomseason-export.csv") }) { Text("Als CSV exportieren") }
                    TextButton({ dialog = null; onExternal(true); pickImport.launch(arrayOf("*/*")) }) { Text("JSON oder CSV importieren") }
                }
            },
            dismissButton = { TextButton({ dialog = null }) { Text("Abbrechen") } },
        )
        null -> Unit
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    text: String,
    confirmRepeat: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    // Beim Einspielen zählt jedes Passwort; beim Erstellen muss es lang genug und bestätigt sein.
    val valid = if (confirmRepeat) password.length >= MIN_PASSWORD && password == repeat else password.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    password, { password = it },
                    label = { Text("Passwort") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (confirmRepeat) {
                    OutlinedTextField(
                        repeat, { repeat = it },
                        label = { Text("Wiederholen") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { Text("Mindestens $MIN_PASSWORD Zeichen") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(password.toCharArray()) }, enabled = valid) { Text(confirmLabel) } },
        dismissButton = { TextButton(onDismiss) { Text("Abbrechen") } },
    )
}

private fun folderName(folder: Uri): String =
    runCatching { DocumentsContract.getTreeDocumentId(folder).substringAfter(':').ifEmpty { "Speicher" } }.getOrDefault("gewählt")

private fun lastBackupText(value: String): String? = runCatching {
    "zuletzt " + LocalDateTime.parse(value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}.getOrNull()
