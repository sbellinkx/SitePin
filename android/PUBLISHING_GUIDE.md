# SitePin - Store Publishing Guide

## Android (Google Play Store)

### Prerequisites
- Google Play Developer account ($25 one-time fee)
- Signed release keystore

### Build Release AAB
```bash
./gradlew :androidApp:bundleRelease
```
Output: `androidApp/build/outputs/bundle/release/androidApp-release.aab`

### Signing
1. Create keystore: `keytool -genkey -v -keystore sitepin-release.jks -keyalg RSA -keysize 2048 -validity 10000`
2. Add to `androidApp/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("sitepin-release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "sitepin"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

### Google Play Console
1. Create app in Google Play Console
2. Set up store listing (title, description, screenshots)
3. Upload AAB to internal/closed/open testing track
4. Complete content rating questionnaire
5. Set pricing & distribution
6. Submit for review

## iOS (App Store)

### Prerequisites
- Apple Developer Program ($99/year)
- Xcode with signing certificates
- Mac for building

### Build
```bash
# Build the shared framework
./gradlew :shared:linkReleaseFrameworkIosArm64

# Archive in Xcode
cd iosApp
xcodebuild archive -scheme iosApp -archivePath build/iosApp.xcarchive -destination 'generic/platform=iOS'

# Export IPA
xcodebuild -exportArchive -archivePath build/iosApp.xcarchive -exportPath build/ -exportOptionsPlist exportOptions.plist
```

### App Store Connect
1. Create app in App Store Connect
2. Upload via Xcode Organizer or `xcrun altool`
3. Add screenshots, description, keywords
4. Submit to TestFlight for beta testing
5. Submit for App Store review

## CI/CD (GitHub Actions)
Include a basic workflow for both platforms.

## Shared .sitepin File Format
The .sitepin file format is cross-platform compatible. Files exported on Android can be imported on iOS and vice versa.
