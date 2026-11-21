<div align="center">

# TeleProxy — تلي بروكسي

**Telegram MTProto Proxy Manager for Android**  
Discover, test, organize, and open Telegram proxy links from one simple Android app.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue)](https://developer.android.com/tools/releases/platforms)

[English](#english) · [العربية](#العربية)

</div>

---

## English

### Overview

**TeleProxy** is an Android application for discovering and managing publicly listed Telegram **MTProto proxy** endpoints. It retrieves proxy links from an external community-maintained source, checks endpoint reachability, measures approximate connection latency, and helps the user choose and open a proxy link in an installed Telegram-compatible client.

> **Important:** TeleProxy is not a VPN and does not route all device traffic. It manages Telegram MTProto proxy links and hands the selected link over to a Telegram client installed on the device.

### Features

- **Automatic proxy discovery** — retrieves public proxy entries from the configured source, with alternative source URLs when the primary endpoint is unavailable.
- **MTProto link parsing** — recognizes `tg://proxy` and `https://t.me/proxy` links and removes duplicate entries.
- **Connectivity and latency checks** — tests proxy server ports, measures approximate TCP connection latency, and displays the results.
- **Automatic cleanup** — removes endpoints that fail the connectivity check during synchronization or batch testing.
- **Smart proxy selection** — sorts available endpoints by reachability and latency, and can automatically switch to the fastest verified alternative when auto-failover is enabled.
- **Connection filters** — browse all entries, reachable entries only, or favorites.
- **Favorites** — mark frequently used proxy entries for quick access.
- **One-tap Telegram handoff** — opens the selected proxy link using an available Telegram-compatible Android client.
- **QR code tools** — generate a QR code for a proxy link, save the QR image, share the image, or copy the connection link.
- **Background synchronization** — schedules periodic proxy refreshes through WorkManager when automatic sync is enabled.
- **Configurable sync interval** — choose 15 minutes, 30 minutes, 1 hour, 6 hours, or 12 hours. The default interval is 30 minutes.
- **Optional stability monitor** — a foreground service checks the selected endpoint periodically and provides notification actions for refresh, stop, and opening Telegram.
- **In-app update flow** — checks GitHub Releases for newer versions and can download and launch an APK installer when a suitable release asset is available.
- **Update validation** — checks the downloaded APK's package identity and version, and compares its signing certificate when signing data is available.
- **Four interface languages** — Arabic, English, Kurdish, and Persian, with right-to-left layout support where applicable.
- **Appearance options** — System, Light, and Dark themes, plus responsive layouts for different screen sizes.
- **Local persistence** — stores proxy entries locally with Room and app preferences with DataStore.

### How it works

1. TeleProxy requests the public proxy list from the configured external source.
2. It extracts and validates MTProto proxy links and avoids duplicate entries.
3. It checks endpoint reachability and records an approximate TCP connection time.
4. The list is organized by availability and latency. Unreachable entries are removed during the cleanup process.
5. The user chooses a proxy and opens its link in an installed Telegram-compatible application, or shares it using a link or QR code.

### Supported Telegram clients

The application checks for several known Android clients, including:

- Telegram
- Telegram Direct
- Telegram X
- Plus Messenger
- Nekogram
- Nagram
- AKA Messenger

The available clients depend on what is installed on the device and on Android's application-link handling.

### Download

- **APK included in this repository:** [Download TeleProxy.apk](./TeleProxy.apk)
- **GitHub Releases:** [View releases](https://github.com/moshraheem-sudo/Mtproxy/releases)

For the in-app update feature, publish a GitHub Release with a version tag greater than the installed app version and attach the corresponding APK file to that release.

### Requirements

| Item | Current project configuration |
|---|---|
| Platform | Android |
| Minimum Android version | Android 7.0 (API 24) |
| Compile SDK | Android API 36.1 |
| Target SDK | Android API 36 |
| Application version | 1.0.0 |
| Application ID | `com.aistudio.teleproxy.kxqrmv` |
| Android Gradle Plugin | 9.1.1 |
| Gradle Wrapper | 9.3.1 |
| Kotlin | 2.2.10 |
| JDK for Gradle | 17 |
| Java source/target compatibility | 11 |

### Build from source

1. Install Android Studio and the Android SDK required by this project. Make sure JDK 17 is selected for Gradle.
2. Clone the repository:

```bash
git clone https://github.com/moshraheem-sudo/Mtproxy.git
cd Mtproxy
```

3. **Create a local debug keystore if `debug.keystore` is missing.** The current Gradle configuration refers to this root-level file, and it is intentionally excluded from Git:

```bash
keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

4. Build the debug APK:

```bash
./gradlew assembleDebug
```

On Windows, use:

```bat
gradlew.bat assembleDebug
```

The generated APK is normally located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

**Release builds:** the release signing configuration expects a keystore and the `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables. Keep your release keystore and passwords private; never commit them to the repository.

### Main technologies

- Kotlin
- Android Jetpack Compose and Material 3
- AndroidX Navigation Compose
- Kotlin Coroutines and Flow
- Room for local proxy storage
- DataStore Preferences for app settings
- WorkManager for scheduled background synchronization
- OkHttp for network requests
- ZXing for QR-code support
- Android foreground services and notifications

### Proxy data source

The current implementation retrieves public proxy links from the community-maintained **SoliSpirit/mtproto** project, using its GitHub raw-content and jsDelivr endpoints as configured in the application:

- [SoliSpirit/mtproto](https://github.com/SoliSpirit/mtproto)
- [Proxy list — all_proxies.txt](https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt)

This is an external source and is not operated by the TeleProxy developer. Its contents and endpoint availability can change at any time.

### Technical notes and limitations

- The endpoint test measures whether a TCP connection to the proxy server and port can be opened. It is a useful reachability and latency indicator, **not a guarantee that the complete MTProto handshake or a Telegram session will succeed**.
- Actual Telegram connectivity is handled by the Telegram-compatible client after TeleProxy opens the selected proxy link.
- Public proxy servers are operated by third parties. Their speed, availability, reliability, and privacy practices are outside the application's control. Use proxy endpoints you trust.
- TeleProxy does not create, host, or administer MTProto proxy servers.

### Contributing and support

Bug reports, suggestions, and improvements are welcome through [GitHub Issues](https://github.com/moshraheem-sudo/Mtproxy/issues). When reporting a problem, include the Android version, app version, and steps to reproduce it. Do not post private proxy secrets or other sensitive connection details in public issues.

### Developer

**Eng. Mohammed Shaheed Al-Ajaibi**  
Repository: [moshraheem-sudo/Mtproxy](https://github.com/moshraheem-sudo/Mtproxy)

### License

A license file is not currently present in the repository. Until a `LICENSE` file is added, the reuse, modification, and redistribution terms have not been explicitly granted. Add the license that you intend to use before describing the project as MIT-, Apache-, or GPL-licensed.

---

## العربية

### نبذة عن التطبيق

**تلي بروكسي (TeleProxy)** هو تطبيق أندرويد لإدارة روابط بروكسي **MTProto** الخاصة بتلغرام. يجلب روابط البروكسي المنشورة في مصدر خارجي، ويفحص إمكانية الوصول إلى الخوادم ويقيس زمن استجابة اتصال TCP بصورة تقريبية، ثم يساعد المستخدم على اختيار الرابط وفتحه مباشرةً داخل أحد تطبيقات تلغرام المتوافقة والمثبتة على الجهاز.

> **تنبيه مهم:** التطبيق ليس VPN ولا يمرر جميع اتصالات الهاتف عبره؛ وظيفته إدارة روابط MTProto الخاصة بتلغرام وفتح الرابط المحدد داخل تطبيق تلغرام.

### المميزات الرئيسية

- **جلب تلقائي للبروكسيات:** تحديث قائمة روابط MTProto من مصدر خارجي مع استخدام روابط بديلة للمصدر عند تعذر الوصول إلى الرابط الأساسي.
- **قراءة روابط تلغرام:** التعرف على صيغ `tg://proxy` و`https://t.me/proxy` واستخراجها مع إزالة التكرار.
- **فحص الاتصال والسرعة:** اختبار إمكانية الوصول إلى منفذ الخادم وقياس زمن اتصال TCP بصورة تقريبية.
- **تنظيف تلقائي:** حذف نقاط الاتصال التي تفشل في اختبار الوصول أثناء التحديث أو الفحص الجماعي.
- **اختيار ذكي:** ترتيب نقاط الاتصال بحسب إمكانية الوصول وزمن الاستجابة، مع إمكانية التحويل التلقائي إلى أسرع بديل متاح عند تفعيل خيار التحويل الذكي.
- **فلاتر للقائمة:** عرض جميع البروكسيات، أو المتاحة فقط، أو المفضلة.
- **إدارة المفضلة:** وضع علامة على نقاط الاتصال المستخدمة باستمرار للوصول إليها بسرعة.
- **فتح تلغرام مباشرةً:** إرسال رابط البروكسي المختار إلى تطبيق تلغرام متوافق ومثبت على الهاتف.
- **رمز QR:** إنشاء رمز QR للرابط، وتنزيل صورته، ومشاركة الصورة، أو نسخ رابط الاتصال.
- **تحديث في الخلفية:** جدولة مزامنة قائمة البروكسيات باستخدام WorkManager عند تفعيل التحديث التلقائي.
- **تحديد وقت التحديث:** 15 دقيقة، أو 30 دقيقة، أو ساعة، أو 6 ساعات، أو 12 ساعة؛ والإعداد الافتراضي 30 دقيقة.
- **مراقبة اختيارية للاستقرار:** خدمة أمامية تراقب نقطة الاتصال المحددة دورياً، وتوفر إشعاراً يتضمن إجراءات التحديث والإيقاف وفتح تلغرام.
- **تحديث التطبيق من داخله:** فحص إصدارات GitHub Releases وتنزيل ملف APK وتشغيل أداة تثبيته عند توفر إصدار أحدث وملف مناسب.
- **التحقق من التحديث:** فحص هوية الحزمة ورقم الإصدار، ومقارنة شهادة التوقيع عند توفر بيانات التوقيع، قبل تثبيت ملف التحديث.
- **أربع لغات:** العربية والإنجليزية والكردية والفارسية، مع دعم اتجاه الكتابة من اليمين إلى اليسار عند الحاجة.
- **ثيمات متعددة:** تلقائي حسب النظام، فاتح، وداكن، مع واجهة متجاوبة مع أحجام الشاشات المختلفة.
- **حفظ محلي:** تخزين بيانات البروكسي باستخدام Room وحفظ إعدادات التطبيق باستخدام DataStore.

### آلية العمل

1. يجلب التطبيق قائمة البروكسيات العامة من المصدر الخارجي المحدد.
2. يستخرج روابط MTProto ويتحقق من صيغتها ويزيل الروابط المكررة.
3. يفحص إمكانية الوصول إلى الخوادم ويسجل زمن اتصال TCP التقريبي.
4. يرتب القائمة حسب حالة الوصول وزمن الاستجابة، ويحذف العناصر التي تفشل في الفحص ضمن عملية التنظيف.
5. يختار المستخدم نقطة الاتصال ويفتحها داخل تطبيق تلغرام، أو يشاركها كرابط أو رمز QR.

### تطبيقات تلغرام التي يتعرف عليها

يتحقق التطبيق من وجود عدد من تطبيقات أندرويد المعروفة، ومنها:

- Telegram الرسمي
- Telegram Direct
- Telegram X
- Plus Messenger
- Nekogram
- Nagram
- AKA Messenger

يعتمد توفر هذه التطبيقات على ما هو مثبت في الهاتف وعلى آلية تعامل أندرويد مع روابط التطبيقات.

### التحميل

- **ملف APK الموجود في المستودع:** [تحميل TeleProxy.apk](./TeleProxy.apk)
- **إصدارات GitHub:** [عرض الإصدارات](https://github.com/moshraheem-sudo/Mtproxy/releases)

لتفعيل آلية التحديث من داخل التطبيق، انشر إصداراً جديداً في GitHub Releases باستخدام رقم إصدار أعلى من الإصدار المثبت، وأرفق ملف APK الخاص بذلك الإصدار.

### متطلبات التشغيل والتطوير

| العنصر | إعداد المشروع الحالي |
|---|---|
| النظام | Android |
| أقل إصدار أندرويد | Android 7.0 (API 24) |
| Compile SDK | Android API 36.1 |
| Target SDK | Android API 36 |
| إصدار التطبيق | 1.0.0 |
| معرّف التطبيق | `com.aistudio.teleproxy.kxqrmv` |
| Android Gradle Plugin | 9.1.1 |
| Gradle Wrapper | 9.3.1 |
| Kotlin | 2.2.10 |
| JDK المستخدم لتشغيل Gradle | 17 |
| توافق مصدر Java | 11 |

### بناء التطبيق من المصدر

استنسخ المستودع:

```bash
git clone https://github.com/moshraheem-sudo/Mtproxy.git
cd Mtproxy
```

إذا لم يكن ملف `debug.keystore` موجوداً، أنشئ نسخة محلية منه؛ لأن إعداد Gradle الحالي يشير إلى هذا الملف في المجلد الرئيسي، وهو مستثنى من الرفع إلى Git:

```bash
keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

ابنِ نسخة الاختبار:

```bash
./gradlew assembleDebug
```

في Windows:

```bat
gradlew.bat assembleDebug
```

سيظهر ملف APK الناتج عادةً في:

```text
app/build/outputs/apk/debug/app-debug.apk
```

**نسخة Release:** يتطلب إعداد توقيع الإصدار ملف keystore خاصاً ومتغيرات البيئة `KEYSTORE_PATH` و`STORE_PASSWORD` و`KEY_PASSWORD`. لا ترفع ملف التوقيع أو كلمات المرور إلى المستودع.

### التقنيات المستخدمة

- Kotlin
- Android Jetpack Compose وMaterial 3
- AndroidX Navigation Compose
- Kotlin Coroutines وFlow
- Room لتخزين بيانات البروكسي محلياً
- DataStore Preferences لإعدادات التطبيق
- WorkManager للمزامنة المجدولة في الخلفية
- OkHttp لطلبات الشبكة
- ZXing لإنشاء رموز QR
- خدمات Android الأمامية والإشعارات

### مصدر بيانات البروكسي

يعتمد التطبيق حالياً على مشروع **SoliSpirit/mtproto** المجتمعي لجلب روابط البروكسي العامة، من خلال روابط GitHub Raw وjsDelivr المحددة في الشيفرة:

- [مستودع SoliSpirit/mtproto](https://github.com/SoliSpirit/mtproto)
- [قائمة البروكسيات all_proxies.txt](https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt)

هذا مصدر خارجي لا يديره مطور TeleProxy، وقد تتغير محتوياته أو تتوقف بعض نقاط الاتصال في أي وقت.

### ملاحظات وحدود الاستخدام

- يقيس الفحص إمكانية فتح اتصال TCP إلى خادم البروكسي ومنفذه. وهو مؤشر على إمكانية الوصول وزمن الاستجابة، **ولا يضمن نجاح مصافحة MTProto كاملة أو نجاح جلسة تلغرام**.
- يتولى تطبيق تلغرام المتوافق تنفيذ الاتصال الفعلي بعد فتح رابط البروكسي من TeleProxy.
- خوادم البروكسي العامة تابعة لأطراف خارجية؛ لذلك لا يتحكم التطبيق في سرعتها أو توفرها أو موثوقيتها أو ممارساتها المتعلقة بالخصوصية. استخدم نقاط اتصال تثق بمصدرها.
- التطبيق لا ينشئ خوادم MTProto ولا يستضيفها ولا يديرها.

### المساهمة والدعم

يمكن إرسال تقارير الأخطاء والاقتراحات والتحسينات عبر [GitHub Issues](https://github.com/moshraheem-sudo/Mtproxy/issues). يرجى ذكر إصدار أندرويد وإصدار التطبيق وخطوات إعادة ظهور المشكلة. لا تنشر أسرار البروكسي الخاصة أو بيانات الاتصال الحساسة في البلاغات العامة.

### المطور

**المهندس محمد شهيد الأعاجيبي**  
المستودع: [moshraheem-sudo/Mtproxy](https://github.com/moshraheem-sudo/Mtproxy)

### الترخيص

لا يحتوي المستودع حالياً على ملف `LICENSE`. لذلك لم تُحدد صراحةً شروط إعادة استخدام الشيفرة أو تعديلها أو إعادة توزيعها. أضف ملف الترخيص الذي تختاره قبل وصف المشروع بأنه مرخص تحت MIT أو Apache أو GPL.
