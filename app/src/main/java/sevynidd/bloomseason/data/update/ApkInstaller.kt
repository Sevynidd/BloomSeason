package sevynidd.bloomseason.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

private const val POLL_INTERVAL_MILLIS = 300L

sealed interface ApkDownloadStatus {
    /** [total] ist -1, wenn der Server keine Größe meldet. */
    data class InProgress(val downloaded: Long, val total: Long) : ApkDownloadStatus
    data class Succeeded(val uri: Uri) : ApkDownloadStatus
    data object Failed : ApkDownloadStatus
}

/** Lädt eine Release-APK über den DownloadManager und startet den System-Installer. */
class ApkInstaller(private val context: Context) {

    fun startDownload(release: GitHubRelease): Long {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), release.apkFileName).delete()
        val request = DownloadManager.Request(release.apkUrl.toUri())
            .setTitle(release.apkFileName)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, release.apkFileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        return downloadManager().enqueue(request)
    }

    fun observe(downloadId: Long): Flow<ApkDownloadStatus> = flow {
        while (true) {
            val status = downloadManager()
                .query(DownloadManager.Query().setFilterById(downloadId))
                .use { it.toStatus() }
            emit(status)
            if (status !is ApkDownloadStatus.InProgress) break
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private fun Cursor.toStatus(): ApkDownloadStatus {
        if (!moveToFirst()) return ApkDownloadStatus.Failed
        return when (getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                val path = getString(getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)).toUri().path
                val file = File(requireNotNull(path))
                ApkDownloadStatus.Succeeded(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
            DownloadManager.STATUS_FAILED -> ApkDownloadStatus.Failed
            else -> ApkDownloadStatus.InProgress(
                getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            )
        }
    }

    fun install(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun downloadManager() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
}
