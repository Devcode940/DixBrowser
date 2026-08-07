# Dix-Browser Core Architecture

## Core WebView Component

The core of the Dix-Browser application is built around the Android `WebView` component, seamlessly integrated into Jetpack Compose via the `AndroidView` composable. This provides a robust rendering engine while maintaining the declarative UI benefits of Compose.

### Key Features of the WebView Implementation:

1. **Jetpack Compose Integration**:
   The `WebView` is wrapped in an `AndroidView`, which allows it to be part of the Compose UI tree. State variables (like the current URL, loading progress, and navigation history) are synchronized between the `WebView` callbacks and the Compose state using a `BrowserTab` data class and the `BrowserViewModel`.

2. **Navigation & State Management**:
   - `WebViewClient`: Handles page rendering events (`onPageStarted`, `onPageFinished`). It updates the Compose state indicating whether a page is loading, tracks the current URL, and updates back/forward capabilities.
   - `WebChromeClient`: Tracks the loading progress (0-100%) and extracts the website's title and favicon, updating the UI in real-time.
   - **Multi-Tab Support**: The browser supports multiple tabs by instantiating separate `WebView` instances or saving their state when switching, managed by a list of `BrowserTab` states.

3. **Security & Privacy Controls**:
   - **JavaScript Control**: JavaScript execution can be toggled on or off per tab.
   - **Cookie Management**: Integrated with `CookieManager` to enforce first-party and third-party cookie preferences on a per-domain basis, which are stored persistently in the local Room database.
   - **Incognito Mode**: Supports incognito browsing by disabling DOM storage, setting the cache mode to `LOAD_NO_CACHE`, and clearing cookies and history dynamically.

4. **Credential Auto-Fill**:
   - Injects a secure JavaScript interface (`PasswordAutoFillBridge`) into the loaded web pages.
   - Detects password fields and securely prompts the user to auto-fill credentials stored in the encrypted Room database.

---

## Deployment & Build Guide

### Prerequisites
- Android Studio (latest version recommended)
- JDK 17 or higher
- Android SDK (API 34)

### Local Development and Testing
1. Clone the repository and open it in Android Studio.
2. Let Gradle sync the project dependencies.
3. Select an emulator or a physical device connected via USB/Wi-Fi debugging.
4. Click the **Run** button (or press `Shift + F10`) to build and deploy the debug version to your device.

### Building for Release (APK / AAB)
To generate a production-ready application for distribution:

1. **Generate a Signed Bundle or APK**:
   - In Android Studio, go to `Build` > `Generate Signed Bundle / APK...`.
   - Choose **Android App Bundle (AAB)** (recommended for Google Play) or **APK** (for direct distribution).
   - Create a new Keystore or select an existing one. Enter your Key alias, passwords, and certificate details.
   - Select the `release` build variant and click **Finish**.

2. **Using Gradle CLI**:
   Alternatively, you can build the project from the terminal:
   ```bash
   # Build a debug APK (located in app/build/outputs/apk/debug/)
   ./gradlew assembleDebug

   # Build a release APK
   ./gradlew assembleRelease

   # Build a release App Bundle (AAB) for Google Play Store
   ./gradlew bundleRelease
   ```

3. **Distribution**:
   - **Google Play Console**: Upload the generated `.aab` file from `app/build/outputs/bundle/release/` to your Play Console dashboard.
   - **Direct Download / Sideloading**: Distribute the `.apk` file from `app/build/outputs/apk/release/` to users. Ensure they have "Install unknown apps" enabled on their devices.
