# SitePin - App Store & Google Play Publishing Guide

## Pre-requisites

### Apple App Store (iOS)
- Apple Developer Program membership ($99/year) - https://developer.apple.com/programs/
- Xcode 15+ installed
- Valid Apple ID enrolled in the program

### Google Play Store (Android)
- Google Play Console account ($25 one-time) - https://play.google.com/console/
- JDK 17+ installed
- Android Studio (optional, for signing key generation)

---

## STEP 1: Prepare App Icons & Metadata (Both Platforms)

### App Icons
Create app icons in all required sizes. Use a single 1024x1024 source image.

**iOS**: Add to `SitePinApp/Assets.xcassets/AppIcon.appiconset/`
- Xcode will generate all sizes from a 1024x1024 source

**Android**: Add to `androidApp/src/main/res/`
- Use Android Studio's Image Asset tool, or provide:
  - `mipmap-mdpi/ic_launcher.png` (48x48)
  - `mipmap-hdpi/ic_launcher.png` (72x72)
  - `mipmap-xhdpi/ic_launcher.png` (96x96)
  - `mipmap-xxhdpi/ic_launcher.png` (144x144)
  - `mipmap-xxxhdpi/ic_launcher.png` (192x192)

### Screenshots (both stores require these)
Prepare screenshots on these devices:
- **iOS**: iPhone 6.7" (iPhone 15 Pro Max), iPad 12.9"
- **Android**: Phone (1080x1920 minimum), Tablet (optional)

Minimum 2 screenshots per device, recommended 5-8 showing:
1. Project list view
2. Document list with pin summary dashboard
3. Plan annotation view with pins placed
4. Pin detail view with photos
5. CSV/PDF export in action

### Store Listing Text
- **App Name**: SitePin
- **Subtitle/Short description**: Construction site inspection & pin tracking
- **Description**: (write a compelling 200-400 word description)
- **Category**: Business / Productivity
- **Keywords** (iOS): construction, inspection, site, pin, annotation, defect, building
- **Privacy Policy URL**: Required by both stores (can use a simple hosted page)

---

## STEP 2: Code Signing & Build Configuration

### iOS - Code Signing

1. **Create certificates** in Apple Developer portal:
   - Go to https://developer.apple.com/account/resources/certificates/list
   - Create "Apple Distribution" certificate
   - Download and install in Keychain

2. **Create App ID**:
   - Go to Identifiers → Register new
   - Bundle ID: `com.sitepinapp.SitePinApp`
   - Enable capabilities: (none special needed)

3. **Create provisioning profile**:
   - Go to Profiles → Generate new
   - Type: "App Store Connect"
   - Select the App ID and certificate
   - Download and install

4. **Configure in Xcode**:
   - Open `SitePinApp.xcodeproj`
   - Select the SitePinApp target → Signing & Capabilities
   - Set Team to your developer account
   - Or set `DEVELOPMENT_TEAM` in build settings

### Android - Signing Key

1. **Generate a keystore** (do this ONCE, keep it safe forever):
```bash
keytool -genkey -v -keystore sitepin-release.keystore \
  -alias sitepin -keyalg RSA -keysize 2048 -validity 10000
```

2. **Configure signing in `androidApp/build.gradle.kts`**:
Add a `signingConfigs` block:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../sitepin-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "sitepin"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

3. **Set environment variables** before building:
```bash
export KEYSTORE_PASSWORD="your-password"
export KEY_PASSWORD="your-key-password"
```

---

## STEP 3: Create Store Entries

### iOS - App Store Connect

1. Go to https://appstoreconnect.apple.com
2. Click "My Apps" → "+" → "New App"
3. Fill in:
   - Platform: iOS
   - Name: SitePin
   - Primary Language: English
   - Bundle ID: `com.sitepinapp.SitePinApp`
   - SKU: `sitepin-ios-1`
4. Fill in the "App Information" tab:
   - Category: Business
   - Content Rights: Does not contain third-party content
   - Age Rating: 4+
5. Fill in "Pricing and Availability":
   - Price: Free (or your chosen price)
   - Availability: All territories

### Android - Google Play Console

1. Go to https://play.google.com/console
2. Click "Create app"
3. Fill in:
   - App name: SitePin
   - Default language: English
   - App or game: App
   - Free or paid: Free
4. Complete the "Store listing":
   - Short description (80 chars max)
   - Full description (4000 chars max)
   - Screenshots
   - Feature graphic (1024x500)
5. Complete "Content rating" questionnaire
6. Complete "Data safety" form:
   - No data collected or shared
   - No account required
   - Camera permission: For taking inspection photos
   - Storage: For saving project data locally

---

## STEP 4: Build Release Versions

### iOS - Archive & Upload

```bash
cd /Users/sebastiaanbellinkx/SitePinApp

# 1. Set version number
# Edit SitePinApp.xcodeproj → MARKETING_VERSION and CURRENT_PROJECT_VERSION

# 2. Archive
xcodebuild -project SitePinApp.xcodeproj \
  -scheme SitePinApp \
  -destination 'generic/platform=iOS' \
  -archivePath build/SitePinApp.xcarchive \
  archive

# 3. Export for App Store
xcodebuild -exportArchive \
  -archivePath build/SitePinApp.xcarchive \
  -exportPath build/AppStore \
  -exportOptionsPlist ExportOptions.plist
```

Create `ExportOptions.plist`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store-connect</string>
    <key>destination</key>
    <string>upload</string>
</dict>
</plist>
```

**Or use Xcode GUI:**
1. Product → Archive
2. Window → Organizer → Select archive → Distribute App
3. Choose "App Store Connect" → Upload

### Android - Build AAB

```bash
cd /Users/sebastiaanbellinkx/SitePinKMP

# Set signing environment variables
export KEYSTORE_PASSWORD="your-password"
export KEY_PASSWORD="your-key-password"

# Build release AAB (Android App Bundle)
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
  ./gradlew :androidApp:bundleRelease

# Output: androidApp/build/outputs/bundle/release/androidApp-release.aab
```

---

## STEP 5: TestFlight / Internal Testing

### iOS - TestFlight

1. After uploading to App Store Connect, go to TestFlight tab
2. Wait for Apple's automated review (usually 1-2 hours)
3. Add internal testers (up to 25 from your team)
4. Add external testers (up to 10,000) - requires brief review
5. Test on real devices for at least 1 week

### Android - Internal Testing

1. In Play Console → Testing → Internal testing
2. Create a new release
3. Upload the `.aab` file
4. Add testers by email
5. Share the opt-in link with testers
6. Test for at least 1 week

---

## STEP 6: Submit for Review

### iOS - App Store Review

1. In App Store Connect → App Store tab
2. Select version → fill in "What's New" text
3. Upload screenshots for all required devices
4. Click "Submit for Review"
5. **Review timeline**: Usually 24-48 hours
6. **Common rejection reasons**:
   - Missing privacy policy
   - Incomplete metadata
   - Crash on launch (test thoroughly!)
   - Missing camera/photo usage descriptions (already configured in Info.plist)

### Android - Production Release

1. After internal testing passes:
2. Go to Production → Create new release
3. Upload the same `.aab` from testing
4. Fill in release notes
5. Review and roll out
6. **Review timeline**: Usually 1-7 days for first submission
7. **Common rejection reasons**:
   - Missing data safety information
   - Policy violations
   - Crash on specific devices

---

## STEP 7: Post-Launch Checklist

- [ ] Monitor crash reports (Xcode Organizer / Play Console)
- [ ] Respond to user reviews
- [ ] Plan update cadence (bug fixes, new features)
- [ ] Set up analytics (optional: Firebase, or App Store Connect built-in)
- [ ] Keep signing keys/certificates backed up securely

---

## Version Numbering

Use the same version for both platforms:
- `MARKETING_VERSION` (iOS) = `versionName` (Android)
- Format: `1.0.0` (major.minor.patch)
- Increment patch for bug fixes, minor for features, major for breaking changes

## Current Build Status

| Platform | Build | Tests |
|----------|-------|-------|
| iOS | BUILD SUCCEEDED | 22/22 pass |
| Android | BUILD SUCCEEDED | 71/71 pass |

## Cross-Platform Compatibility

Both apps export/import the same `.sitepin` file format (v2) with:
- Identical field names and structure
- Matching category names and colors
- Matching status values
- Base64-encoded binary data (photos, documents)
- ISO8601 date strings
- Format version field for future migrations
