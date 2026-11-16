package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.ProxyEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object QrCodeGenerator {

    fun generateQrBitmap(content: String, sizePx: Int = 720): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) Color.parseColor("#0B1D3A") else Color.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    fun createShareablePosterBitmap(proxy: ProxyEntity): Bitmap {
        val width = 1080
        val height = 1360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep Navy gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.parseColor("#081D38"), Color.parseColor("#0F2D52"), Color.parseColor("#091628")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("تلي بروكسي • MTProto Proxy", width / 2f, 115f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7DD3FC")
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("امسح الباركود بكاميرا الهاتف أو تطبيق تلغرام للاتصال المباشر", width / 2f, 175f, subtitlePaint)

        // White rounded card for crisp scanning
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val cardRect = RectF(130f, 225f, width - 130f, 1045f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // QR bitmap inside card
        val qrSize = 720
        val qrBitmap = generateQrBitmap(proxy.tmeLink, qrSize)
        canvas.drawBitmap(qrBitmap, (width - qrSize) / 2f, 275f, null)

        // Server info inside bottom of white card
        val serverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 30f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${proxy.server}:${proxy.port}", width / 2f, 1018f, serverPaint)

        // Latency & Type pill
        val infoLine = buildString {
            append(proxy.secretType)
            if (proxy.isReachable && proxy.latencyMs in 1 until ProxyEntity.LATENCY_FAILED) {
                append("  •  ${proxy.latencyMs} ms")
            }
        }
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34D399")
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(infoLine, width / 2f, 1135f, infoPaint)

        // Developer credit footer
        val devPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("المطور: محمد شهيد الاعاجيبي", width / 2f, 1230f, devPaint)

        val repoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("github.com/SoliSpirit/mtproto", width / 2f, 1285f, repoPaint)

        return bitmap
    }

    suspend fun saveQrToMediaStore(context: Context, bitmap: Bitmap, fileName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/TeleProxy"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext Result.failure(IllegalStateException("تعذر إنشاء ملف الصورة"))

                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: return@withContext Result.failure(IllegalStateException("تعذر فتح مسار الحفظ"))

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Result.success(uri)
                } else {
                    Result.failure(UnsupportedOperationException("USE_SAF_DOCUMENT_CREATOR"))
                }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun writeBitmapToUri(context: Context, uri: Uri, bitmap: Bitmap): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val out: OutputStream = context.contentResolver.openOutputStream(uri)
                    ?: return@withContext Result.failure(IllegalStateException("تعذر فتح الوجهة"))
                out.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun shareQrImage(context: Context, bitmap: Bitmap, fileName: String) {
        val shareUri = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "qr_codes").apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_TEXT, "باركود بروكسي تلغرام السريع • تطوير: محمد شهيد الاعاجيبي")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "مشاركة صورة الباركود").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
