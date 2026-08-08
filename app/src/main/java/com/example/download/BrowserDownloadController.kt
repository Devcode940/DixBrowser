package com.example.download

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import java.util.UUID

/**
 * Lifecycle-safe downloads backed by Android's system DownloadManager.
 * Download state survives activity recreation and process death.
 */
class BrowserDownloadController(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)
    private val prefs = appContext.getSharedPreferences("browser_downloads", Context.MODE_PRIVATE)
    private val idsKey = "download_ids"

    fun enqueue(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null
    ): Long {
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Only HTTP(S) downloads are supported"
        }

        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            .ifBlank { "download-${UUID.randomUUID()}" }

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription(url)
            setMimeType(mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            userAgent?.takeIf { it.isNotBlank() }?.let { addRequestHeader("User-Agent", it) }
        }

        val id = manager.enqueue(request)
        persistId(id)
        return id
    }

    fun query(id: Long): DownloadState? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            return DownloadState(
                id = id,
                status = it.int(DownloadManager.COLUMN_STATUS),
                reason = it.int(DownloadManager.COLUMN_REASON),
                bytesDownloaded = it.long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                totalBytes = it.long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                localUri = it.string(DownloadManager.COLUMN_LOCAL_URI),
                fileName = it.string(DownloadManager.COLUMN_TITLE)
            )
        }
    }

    fun activeDownloads(): List<DownloadState> = storedIds().mapNotNull(::query)

    fun remove(id: Long) {
        manager.remove(id)
        removeId(id)
    }

    fun clearFinished() {
        storedIds().forEach { id ->
            val state = query(id) ?: return@forEach
            if (state.status == DownloadManager.STATUS_SUCCESSFUL ||
                state.status == DownloadManager.STATUS_FAILED
            ) {
                removeId(id)
            }
        }
    }

    private fun storedIds(): List<Long> = prefs.getStringSet(idsKey, emptySet())
        .orEmpty()
        .mapNotNull { it.toLongOrNull() }

    private fun persistId(id: Long) {
        val ids = storedIds().toMutableSet().apply { add(id.toString()) }
        prefs.edit().putStringSet(idsKey, ids).apply()
    }

    private fun removeId(id: Long) {
        val ids = storedIds().toMutableSet().apply { remove(id.toString()) }
        prefs.edit().putStringSet(idsKey, ids).apply()
    }

    private fun Cursor.int(column: String): Int = getColumnIndexOrThrow(column).let(::getInt)
    private fun Cursor.long(column: String): Long = getColumnIndexOrThrow(column).let(::getLong)
    private fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let(::getString)
}

data class DownloadState(
    val id: Long,
    val status: Int,
    val reason: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localUri: String?,
    val fileName: String?
) {
    val progress: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
}
