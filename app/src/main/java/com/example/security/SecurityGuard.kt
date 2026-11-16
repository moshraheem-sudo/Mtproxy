package com.example.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.BuildConfig
import com.example.data.network.AppUpdateAndStatsFetcher
import java.io.File
import java.security.MessageDigest

data class SecurityStatus(
    val httpsEnforced: Boolean = true,
    val signatureDigestShort: String = "",
    val packageIntegrityOk: Boolean = true
)

sealed interface ApkVerificationResult {
    data object Valid : ApkVerificationResult
    data class Invalid(val reasonKey: String) : ApkVerificationResult
}

object SecurityGuard {

    private val allowedHttpsHosts = setOf(
        "github.com",
        "api.github.com",
        "raw.githubusercontent.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "codeload.github.com",
        "cdn.jsdelivr.net"
    )

    private val serverHostRegex = Regex("^[A-Za-z0-9.-]{3,253}$")
    private val hexSecretRegex = Regex("^[0-9a-fA-F]{32,512}$")
    private val base64SecretRegex = Regex("^[A-Za-z0-9+/_-]{16,512}={0,2}$")

    fun isTrustedHttpsUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase()?.trim().orEmpty()
        if (host.isEmpty()) return false
        return allowedHttpsHosts.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }

    fun isValidMtprotoServer(server: String): Boolean {
        val clean = server.trim()
        return clean.isNotEmpty() &&
            !clean.contains("..") &&
            !clean.startsWith(".") &&
            !clean.endsWith(".") &&
            serverHostRegex.matches(clean)
    }

    fun isValidMtprotoPort(port: Int?): Boolean = port != null && port in 1..65535

    fun isValidMtprotoSecret(secret: String): Boolean {
        val clean = secret.trim()
        return hexSecretRegex.matches(clean) || base64SecretRegex.matches(clean)
    }

    fun sanitizeProxyFields(server: String, port: Int, secret: String): Boolean {
        return isValidMtprotoServer(server) &&
            isValidMtprotoPort(port) &&
            isValidMtprotoSecret(secret)
    }

    fun inspectAppIntegrity(context: Context): SecurityStatus {
        val installedDigests = getPackageSigningDigests(context, context.packageName)
        val integrityOk = context.packageName == BuildConfig.APPLICATION_ID && installedDigests.isNotEmpty()
        val shortDigest = installedDigests.firstOrNull()?.take(12)?.uppercase().orEmpty()
        return SecurityStatus(
            httpsEnforced = true,
            signatureDigestShort = shortDigest,
            packageIntegrityOk = integrityOk
        )
    }

    fun verifyDownloadedUpdateApk(context: Context, apkFile: File): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() < 100_000L) {
            return ApkVerificationResult.Invalid("apk_corrupt")
        }

        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo: PackageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            }
        } catch (_: Throwable) {
            null
        } ?: return ApkVerificationResult.Invalid("apk_corrupt")

        if (archiveInfo.packageName != context.packageName) {
            return ApkVerificationResult.Invalid("apk_package_mismatch")
        }

        val archiveVersion = archiveInfo.versionName.orEmpty()
        if (archiveVersion.isNotBlank() &&
            AppUpdateAndStatsFetcher.compareSemVer(archiveVersion, BuildConfig.VERSION_NAME) <= 0
        ) {
            return ApkVerificationResult.Invalid("apk_downgrade_blocked")
        }

        val installedDigests = getPackageSigningDigests(context, context.packageName)
        val archiveDigests = extractSigningDigests(archiveInfo)
        if (installedDigests.isNotEmpty() && archiveDigests.isNotEmpty()) {
            val matchesSigner = archiveDigests.any { it in installedDigests }
            if (!matchesSigner) {
                return ApkVerificationResult.Invalid("apk_signature_mismatch")
            }
        }

        return ApkVerificationResult.Valid
    }

    private fun getPackageSigningDigests(context: Context, packageName: String): Set<String> {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
            extractSigningDigests(info)
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private fun extractSigningDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                else signingInfo.signingCertificateHistory
            } else {
                emptyArray()
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }

        return signatures.mapNotNull { sig ->
            runCatching {
                val md = MessageDigest.getInstance("SHA-256")
                md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
            }.getOrNull()
        }.toSet()
    }
}
