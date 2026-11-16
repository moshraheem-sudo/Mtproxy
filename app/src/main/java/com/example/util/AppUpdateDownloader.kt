package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.security.ApkVerificationResult
import com.example.security.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface AppDownloadState {
    data object Idle : AppDownloadState
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float
    ) : AppDownloadState {
        val downloadedMb: String
            get() = formatMb(downloadedBytes)
        val totalMb: String
            get() = if (totalBytes > 0L) formatMb(totalBytes) else "..."
    }

    data class ReadyToInstall(
        val apkFile: File,
        val totalBytes: Long
    ) : AppDownloadState {
        val totalMb: String
            get() = formatMb(totalBytes)
    }

    data class Error(val reasonKey: String, val detail: String = "") : AppDownloadState

    companion object {
        fun formatMb(bytes: Long): String {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.2f MB", mb)
        }
    }
}

object AppUpdateDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun downloadApk(context: Context, apkUrl: String, versionName: String): Flow<AppDownloadState> = flow {
        if (!SecurityGuard.isTrustedHttpsUrl(apkUrl)) {
            emit(AppDownloadState.Error("untrusted_source"))
            return@flow
        }

        emit(AppDownloadState.Downloading(0L, 0L, 0f))
        val updatesDir = File(context.filesDir, "updates").apply {
            mkdirs()
            listFiles()?.forEach { old ->
                if (old.isFile && (old.name.endsWith(".apk") || old.name.endsWith(".part"))) {
                    old.delete()
                }
            }
        }
        val safeVer = versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(updatesDir, "TeleProxy_v${safeVer}.apk")
        val tempFile = File(updatesDir, "TeleProxy_v${safeVer}.apk.part")

        val request = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", "TeleProxy-Android")
            .build()

        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            if (!SecurityGuard.isTrustedHttpsUrl(finalUrl)) {
                emit(AppDownloadState.Error("untrusted_source"))
                return@flow
            }
            if (!response.isSuccessful) {
                emit(AppDownloadState.Error("http_error", "${response.code}"))
                return@flow
            }
            val body = response.body ?: run {
                emit(AppDownloadState.Error("empty_file"))
                return@flow
            }
            val contentLength = body.contentLength()
            val buffer = ByteArray(16 * 1024)
            var downloaded = 0L
            var lastEmitTime = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 150L) {
                            lastEmitTime = now
                            val progress = if (contentLength > 0L) {
                                (downloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            emit(AppDownloadState.Downloading(downloaded, contentLength, progress))
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }

            when (val verification = SecurityGuard.verifyDownloadedUpdateApk(context, targetFile)) {
                is ApkVerificationResult.Valid -> {
                    emit(AppDownloadState.ReadyToInstall(targetFile, targetFile.length()))
                }
                is ApkVerificationResult.Invalid -> {
                    targetFile.delete()
                    emit(AppDownloadState.Error(verification.reasonKey))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        if (SecurityGuard.verifyDownloadedUpdateApk(context, apkFile) !is ApkVerificationResult.Valid) {
            apkFile.delete()
            return false
        }
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
