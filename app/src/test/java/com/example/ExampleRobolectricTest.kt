package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppLanguage
import com.example.data.local.SyncStateCode
import com.example.data.network.AppUpdateAndStatsFetcher
import com.example.data.network.MtprotoFetcher
import com.example.security.SecurityGuard
import com.example.ui.i18n.stringsFor
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ScreenWidthClass
import com.example.ui.theme.computeAdaptiveUiSpecs
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `multi language strings and sync state formatting work without cross language leaks`() {
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertTrue(AppLanguage.KURDISH.isRtl)
        assertTrue(AppLanguage.PERSIAN.isRtl)

        val en = stringsFor(AppLanguage.ENGLISH)
        val arabicCharRegex = Regex("[\u0600-\u06FF]")
        assertFalse(arabicCharRegex.containsMatchIn(en.officialTelegramName))
        assertFalse(arabicCharRegex.containsMatchIn(en.formatSyncState(SyncStateCode.WORKING, 90, 288, "")))
        assertFalse(arabicCharRegex.containsMatchIn(en.formatSyncState(SyncStateCode.IDLE, 0, 0, "")))
        assertFalse(arabicCharRegex.containsMatchIn(en.securityShieldDetails))

        assertEquals("قائمة الاتصال", stringsFor(AppLanguage.ARABIC).navConnections)
        assertEquals("Connections", en.navConnections)
        assertEquals("لیستی پەیوەندی", stringsFor(AppLanguage.KURDISH).navConnections)
        assertEquals("فهرست اتصال", stringsFor(AppLanguage.PERSIAN).navConnections)

        // Ensure Home sync footer line has no repeated auto-sync phrases
        AppLanguage.entries.forEach { lang ->
            val s = stringsFor(lang)
            val timestampLine = s.lastUpdateCloudText("12:00")
            assertFalse(timestampLine.contains(s.autoSyncOnBadge.replace(" ✓", "")))
        }
    }

    @Test
    fun `adaptive ui specs adjust spacing typography scale and navigation mode for all screen sizes`() {
        val tinyPhone = computeAdaptiveUiSpecs(maxWidth = 320.dp, systemFontScale = 1.0f)
        assertEquals(ScreenWidthClass.ULTRA_COMPACT, tinyPhone.widthClass)
        assertTrue(tinyPhone.useStackedHeaders)
        assertFalse(tinyPhone.useNavigationRail)
        assertTrue(tinyPhone.fontScaleMultiplier < 1.0f)

        val standardPhone = computeAdaptiveUiSpecs(maxWidth = 390.dp, systemFontScale = 1.0f)
        assertEquals(ScreenWidthClass.COMPACT, standardPhone.widthClass)
        assertFalse(standardPhone.useStackedHeaders)
        assertFalse(standardPhone.useNavigationRail)
        assertEquals(1.0f, standardPhone.fontScaleMultiplier)

        val tablet = computeAdaptiveUiSpecs(maxWidth = 720.dp, systemFontScale = 1.0f)
        assertEquals(ScreenWidthClass.EXPANDED, tablet.widthClass)
        assertTrue(tablet.useNavigationRail)
        assertFalse(tablet.useStackedHeaders)
    }

    @Test
    fun `security guard blocks untrusted urls and malformed mtproto inputs`() {
        assertTrue(SecurityGuard.isTrustedHttpsUrl("https://github.com/moshraheem-sudo/Mtproxy"))
        assertTrue(SecurityGuard.isTrustedHttpsUrl("https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt"))
        assertFalse(SecurityGuard.isTrustedHttpsUrl("http://github.com/moshraheem-sudo/Mtproxy"))
        assertFalse(SecurityGuard.isTrustedHttpsUrl("https://evil-attacker.example/update.apk"))

        assertTrue(SecurityGuard.sanitizeProxyFields("ir.ataman.info", 8443, "ee11223344556677889900aabbccddeeff776655"))
        assertFalse(SecurityGuard.sanitizeProxyFields("bad host/path", 8443, "ee11223344556677889900aabbccddeeff776655"))
        assertFalse(SecurityGuard.sanitizeProxyFields("ir.ataman.info", 99999, "ee11223344556677889900aabbccddeeff776655"))
        assertFalse(SecurityGuard.sanitizeProxyFields("ir.ataman.info", 443, "short"))
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
