# SitePin - TestFlight Deployment Guide

## Prerequisites

- Mac with Xcode installed (you already have this)
- Apple Developer account ($99/year) — enroll at https://developer.apple.com/programs/
- Your Apple ID signed into Xcode

---

## Step 1: Apple Developer Account

If you don't have one yet:
1. Go to https://developer.apple.com/programs/
2. Click "Enroll" and follow the steps
3. Pay the $99/year fee
4. Wait for approval (usually instant for individuals, 1-2 days for organizations)

---

## Step 2: Configure Signing in Xcode

1. Open `/Users/sebastiaanbellinkx/SitePinApp/SitePinApp.xcodeproj` in Xcode
2. Click the **SitePinApp** project in the left sidebar (blue icon, top of file list)
3. Select the **SitePinApp** target (under TARGETS)
4. Go to **Signing & Capabilities** tab
5. Check **"Automatically manage signing"** (should already be checked)
6. Set **Team** to your Apple Developer account (select from dropdown)
7. The **Bundle Identifier** is `com.sitepinapp.SitePinApp`
   - If this is already taken, change it to something unique like `com.sebastiaanbellinkx.SitePin`
   - If you change it, change it in BOTH Debug and Release configurations

---

## Step 3: Create the App in App Store Connect

1. Go to https://appstoreconnect.apple.com
2. Click **"My Apps"** → **"+"** → **"New App"**
3. Fill in:
   - **Platform**: iOS
   - **Name**: SitePin
   - **Primary Language**: English (or Dutch)
   - **Bundle ID**: Select `com.sitepinapp.SitePinApp` (matches Xcode)
   - **SKU**: `sitepin-1` (any unique string)
   - **User Access**: Full Access
4. Click **Create**

---

## Step 4: Archive and Upload

### Option A: From Xcode (Recommended)

1. In Xcode, set the destination to **"Any iOS Device (arm64)"** (top toolbar, next to the scheme)
2. Go to **Product** → **Archive**
3. Wait for the archive to complete (1-2 minutes)
4. The **Organizer** window opens automatically
5. Select the archive and click **"Distribute App"**
6. Choose **"App Store Connect"** → **Next**
7. Choose **"Upload"** → **Next**
8. Leave all options checked (bitcode, symbol upload) → **Next**
9. Select your signing certificate and provisioning profile → **Upload**
10. Wait for upload to complete

### Option B: From Command Line

```bash
# 1. Archive
xcodebuild -project SitePinApp.xcodeproj \
  -scheme SitePinApp \
  -sdk iphoneos \
  -configuration Release \
  -archivePath ~/SitePinApp.xcarchive \
  archive

# 2. Create ExportOptions.plist (one-time)
cat > ~/ExportOptions.plist << 'EOF'
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
EOF

# 3. Export and upload
xcodebuild -exportArchive \
  -archivePath ~/SitePinApp.xcarchive \
  -exportOptionsPlist ~/ExportOptions.plist \
  -exportPath ~/SitePinExport
```

---

## Step 5: TestFlight Setup

1. After upload, go to https://appstoreconnect.apple.com → **My Apps** → **SitePin**
2. Click the **TestFlight** tab
3. Wait for **Processing** to complete (5-30 minutes)
4. Apple may ask you **Export Compliance** questions:
   - "Does your app use encryption?" → **No** (the app doesn't use custom encryption)

### Internal Testing (just you, up to 100 Apple Developer team members):
5. Under **Internal Testing**, your build should appear automatically
6. Click the build → **"Test"** or add yourself as an internal tester
7. Open **TestFlight** app on your iPhone → install the app

### External Testing (invite others like Glenn):
8. Click **"External Testing"** → **"+"** → Create a new group (e.g., "Beta Testers")
9. Add testers by email
10. Select the build to test
11. Fill in:
    - **What to Test**: "Test pin placement on floor plans, PDF export, and project sharing"
    - **App Description**: "SitePin - Construction site inspection tool for annotating floor plans"
12. Click **Submit for Review** (Apple reviews external TestFlight builds, takes ~24 hours first time)
13. Testers receive an email invitation → they download TestFlight → install SitePin

---

## Step 6: Install on Your Physical iPhone (Quick Method)

If you just want to test on your own device without TestFlight:

1. Connect your iPhone via USB cable
2. In Xcode, select your iPhone from the device dropdown (top toolbar)
3. Press **Cmd+R** (Run)
4. On first run, your iPhone may say "Untrusted Developer":
   - Go to **Settings** → **General** → **VPN & Device Management**
   - Tap your developer certificate → **Trust**
5. Run again from Xcode

---

## Version Management

For each new TestFlight build, increment the build number:
- **Marketing Version** (1.0, 1.1, 2.0) — visible to users
- **Build Number** (1, 2, 3...) — must be unique per upload

In Xcode: Target → General → Identity section
Or in the project file, update `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION`.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "No signing certificate" | Xcode → Settings → Accounts → your Apple ID → Manage Certificates → + Apple Distribution |
| "Bundle ID already taken" | Change `PRODUCT_BUNDLE_IDENTIFIER` to something unique |
| "Processing" stuck in App Store Connect | Wait up to 1 hour; if still stuck, delete and re-upload |
| Build rejected for missing privacy | Already included: PrivacyInfo.xcprivacy is in the project |
| "Untrusted developer" on iPhone | Settings → General → VPN & Device Management → Trust |
| Archive option is greyed out | Make sure destination is "Any iOS Device", not a simulator |

---

## What's Already Prepared

The following are already configured in the project:

- [x] App icon (1024x1024 PNG — blue pin with "SP")
- [x] Privacy manifest (PrivacyInfo.xcprivacy — UserDefaults + file timestamps declared)
- [x] Info.plist with camera/photo library usage descriptions
- [x] Bundle display name: "SitePin"
- [x] Supports iPhone + iPad
- [x] iOS 17.0+ deployment target
- [x] Debug test code (#if DEBUG) excluded from Release builds
- [x] Release build verified ✓

## What You Need To Do

1. [ ] Enroll in Apple Developer Program ($99/year) if not already
2. [ ] Set your Team in Xcode → Signing & Capabilities
3. [ ] Create the app in App Store Connect
4. [ ] Archive → Upload from Xcode
5. [ ] Set up TestFlight testers
