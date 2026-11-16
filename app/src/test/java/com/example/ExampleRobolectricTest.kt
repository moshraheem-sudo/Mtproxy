package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.AppUpdateAndStatsFetcher
import com.example.data.network.MtprotoFetcher
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name version and developer attribution are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("تلي بروكسي", context.getString(R.string.app_name))
        assertEquals("محمد شهيد الاعاجيبي", context.getString(R.string.developer_name))
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
    }

    @Test
    fun `semver comparison only accepts versions higher than installed`() {
        assertTrue(AppUpdateAndStatsFetcher.compareSemVer("v1.0.1", "1.0.0") > 0)
        assertTrue(AppUpdateAndStatsFetcher.compareSemVer("1.1.0", "1.0.0") > 0)
        assertTrue(AppUpdateAndStatsFetcher.compareSemVer("2.0", "1.0.0") > 0)
        assertEquals(0, AppUpdateAndStatsFetcher.compareSemVer("v1.0.0", "1.0.0"))
        assertTrue(AppUpdateAndStatsFetcher.compareSemVer("0.9.9", "1.0.0") < 0)
    }

    @Test
    fun `mtproto fetcher parses tg and tme proxy links`() {
        val fetcher = MtprotoFetcher()
        val sample = """
            tg://proxy?server=185.10.10.1&port=443&secret=ee11223344556677889900aabbccddeeff776655
            https://t.me/proxy?server=proxy.example.com&port=8443&secret=dd00112233445566778899aabbccddeeff
            tg://proxy?server=185.10.10.1&port=443&secret=ee11223344556677889900aabbccddeeff776655
        """.trimIndent()

        val parsed = fetcher.parseProxies(sample)
        assertEquals(2, parsed.size)
        assertEquals("185.10.10.1", parsed[0].server)
        assertEquals(443, parsed[0].port)
        assertEquals("Fake-TLS", parsed[0].secretType)
        assertEquals("Secure DD", parsed[1].secretType)
    }

    @Test
    fun `qr code generator builds non empty bitmap`() {
        val bitmap = QrCodeGenerator.generateQrBitmap(
            "https://t.me/proxy?server=1.2.3.4&port=443&secret=0123456789abcdef0123456789abcdef",
            320
        )
        assertTrue(bitmap.width == 320 && bitmap.height == 320)
    }
}
