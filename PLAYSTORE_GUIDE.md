# DraftIt — Complete Play Store Publishing Guide & Session Log

> **App:** DraftIt (com.flash.draft)  
> **Developer:** Dhanush E Devadiga  
> **Date:** 2026-08-31  
> **Status:** Code changes DONE ✅ — Play Console steps PENDING  

---

## TABLE OF CONTENTS

1. [What This App Is](#1-what-this-app-is)
2. [Code Changes Already Made](#2-code-changes-already-made)
3. [How The Signing System Works](#3-how-the-signing-system-works)
4. [Step-by-Step: Create Keystore & Build AAB](#4-step-by-step-create-keystore--build-aab)
5. [Step-by-Step: Google Play Console Setup](#5-step-by-step-google-play-console-setup)
6. [Store Listing Content (Copy-Paste Ready)](#6-store-listing-content-copy-paste-ready)
7. [Graphics Requirements](#7-graphics-requirements)
8. [Content Rating Answers](#8-content-rating-answers)
9. [Data Safety Form Answers](#9-data-safety-form-answers)
10. [All Other Declarations](#10-all-other-declarations)
11. [Post-Launch: Firebase & In-App Review](#11-post-launch-firebase--in-app-review)
12. [Files Modified Reference](#12-files-modified-reference)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. What This App Is

DraftIt is an **infinite canvas app** built entirely in **Jetpack Compose** (Kotlin). It allows users to:

- Draw freehand with multiple brush types (Pen, Highlighter) using AndroidX Ink library
- Add sticky notes, images, PDFs, code blocks, audio notes, shapes, labels, frames, task lists
- Connect elements with connectors (arrows)
- Remove image backgrounds using Google ML Kit Subject Segmentation (on-device)
- Record voice memos (uses RECORD_AUDIO permission)
- Download Google Fonts dynamically (uses INTERNET permission)
- Manage multiple projects from a dashboard
- Customize settings: dark mode, framerate, liquid physics, haptic feedback, undo history

**Key technical details:**
- Package name: `com.flash.draft`
- Min SDK: 26 (Android 8.0)
- Target SDK: 36
- Single Activity architecture with Compose Navigation
- Room database for note storage
- No accounts, no cloud, no analytics (local-first)
- Privacy policy: https://fordhamgr.github.io/draftit-privacy/
- Support email: draftit.support@gmail.com

---

## 2. Code Changes Already Made

All these changes have been applied and **both debug and release builds pass successfully**.

### 2.1 — build.gradle.kts (app/build.gradle.kts)

**What changed:**
- Added `import java.io.FileInputStream` and `import java.util.Properties` at the top
- Added a `signingConfigs` block that reads credentials from `keystore.properties`
- Changed `isMinifyEnabled` from `false` to `true`
- Changed `isShrinkResources` from `false` to `true`
- Changed signing config to use release keystore when `keystore.properties` exists, falls back to debug otherwise

**Current state of the file:**
```kotlin
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // TODO: UNCOMMENT these two lines after adding your google-services.json to the app/ folder!
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.flash.draft"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flash.draft"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    // ... rest unchanged
}
```

**IMPORTANT NOTE:** The `kotlin-android` plugin is NOT added because `kotlin-compose` already includes it internally. Adding both causes a "duplicate extension" build error.

### 2.2 — proguard-rules.pro (app/proguard-rules.pro)

**Completely rewritten** with keep rules for all libraries used:
- Kotlin Coroutines
- Google ML Kit
- Room Database (Entity, Dao, NoteEntity)
- App models (`com.flash.draft.model.**`)
- Jetpack Compose runtime
- Compose Navigation
- Coil image loading
- AndroidX Ink (alpha library)
- Liquid library (io.github.fletchmckee.liquid)
- Composables Icons Lucide
- Google Play Services
- Google Fonts provider
- Splash Screen
- Firebase (commented out, ready for when enabled)

### 2.3 — .gitignore

**Added entries:**
```
# Release signing — NEVER commit these
keystore.properties
*.jks
*.keystore
/app/release/
```

### 2.4 — keystore.properties.example (NEW FILE)

Created at project root. Template file showing the format:
```properties
storeFile=../draftit-release.jks
storePassword=YOUR_KEYSTORE_PASSWORD_HERE
keyAlias=draftit
keyPassword=YOUR_KEY_PASSWORD_HERE
```

### 2.5 — Build Verification Results

| Build Command | Result | Duration |
|---|---|---|
| `.\gradlew assembleDebug` | ✅ BUILD SUCCESSFUL | 1m 27s |
| `.\gradlew assembleRelease` | ✅ BUILD SUCCESSFUL | 1m 40s |

---

## 3. How The Signing System Works

The build is set up so that:

1. If `keystore.properties` exists in the project root → uses your **release keystore** for release builds
2. If `keystore.properties` does NOT exist → falls back to **debug keystore** (so other developers can still build locally)

Files involved:
- `keystore.properties` — your real credentials (NEVER committed to Git)
- `keystore.properties.example` — template (committed to Git)
- `draftit-release.jks` — your actual keystore file (NEVER committed to Git)
- `.gitignore` — blocks all of the above from being committed

---

## 4. Step-by-Step: Create Keystore & Build AAB

### Step 4.1 — Create the Keystore

Open a terminal and run:

```bash
keytool -genkeypair -v -keystore draftit-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias draftit
```

It will ask you:

| Prompt | What to enter |
|---|---|
| Keystore password | Choose a strong password, WRITE IT DOWN |
| Re-enter password | Same password |
| First and Last Name | Dhanush E Devadiga |
| Organizational Unit | Independent (or leave blank) |
| Organization | Flash (or leave blank) |
| City | Your city |
| State | Your state |
| Country Code | IN (for India) |
| Is this correct? [no] | yes |
| Key password | Same as keystore password (or different) |

This creates `draftit-release.jks` in whatever directory you ran the command in.

### Step 4.2 — Move the Keystore

Move `draftit-release.jks` to the **project root** (same folder as `build.gradle.kts`, `settings.gradle.kts`, etc.):

```
draftit/
├── draftit-release.jks    ← HERE
├── keystore.properties    ← CREATE THIS
├── keystore.properties.example
├── build.gradle.kts
├── settings.gradle.kts
├── app/
│   └── ...
```

### Step 4.3 — Create keystore.properties

Copy `keystore.properties.example` to `keystore.properties` and fill in real values:

```properties
storeFile=../draftit-release.jks
storePassword=your_actual_password
keyAlias=draftit
keyPassword=your_actual_key_password
```

### Step 4.4 — Build the Release AAB

Google Play requires an **AAB** (Android App Bundle), not an APK:

```bash
.\gradlew bundleRelease
```

Output will be at: `app/build/outputs/bundle/release/app-release.aab`

This is the file you upload to the Play Console.

### Step 4.5 — BACK UP YOUR KEYSTORE

**CRITICAL:** Back up both `draftit-release.jks` and `keystore.properties` to:
- A USB drive
- Google Drive / OneDrive
- A password manager

If you lose your keystore, you can NEVER update your app on Google Play again (unless you use Play App Signing, which is recommended — see Step 5.7).

---

## 5. Step-by-Step: Google Play Console Setup

Go to: https://play.google.com/console

### Step 5.1 — Create the App

1. Click **"Create app"** (blue button on home page)
2. App name: **DraftIt**
3. Default language: **English (United States)**
4. App or Game: **App**
5. Free or Paid: **Free**
6. Read and check all the declaration checkboxes
7. Click **Create app**

### Step 5.2 — Store Listing

Navigate to: **Grow > Store presence > Main store listing**

Fill in the fields using the content from [Section 6](#6-store-listing-content-copy-paste-ready) below.

### Step 5.3 — Upload Graphics

See [Section 7](#7-graphics-requirements) for exact specs.

Upload:
- App icon (512×512) — you already have `ic_launcher-playstore.png` in `app/src/main/`
- Feature graphic (1024×500) — you need to create this
- Phone screenshots (min 2) — take these from your device or emulator

### Step 5.4 — Content Rating

Navigate to: **Policy > App content > Content rating**

See [Section 8](#8-content-rating-answers) for exact answers.

### Step 5.5 — Privacy Policy

Navigate to: **Policy > App content > Privacy policy**

Paste: `https://fordhamgr.github.io/draftit-privacy/`

### Step 5.6 — Data Safety

Navigate to: **Policy > App content > Data safety**

See [Section 9](#9-data-safety-form-answers) for exact answers.

### Step 5.7 — Create Release & Upload AAB

1. Navigate to: **Release > Production**
2. Click **Create new release**
3. Google will ask about **Play App Signing** → **OPT IN (YES)**
   - This means Google holds a copy of your upload key
   - If you ever lose your keystore, Google can help you recover
   - This is strongly recommended
4. Upload your `app-release.aab` file
5. Release name: `1.0.0`
6. Release notes: 
   ```
   Initial release of DraftIt — an infinite canvas for brainstorming, mind mapping, and doodling.
   ```

### Step 5.8 — Review & Publish

1. Go to **Publishing overview** in the left sidebar
2. All sections should show green checkmarks ✅
3. If any show warnings/errors, click them to fix
4. Click **Send for review**

**First-time review typically takes 3–7 days** (can take up to 14 for new accounts).

---

## 6. Store Listing Content (Copy-Paste Ready)

### App Name
```
DraftIt
```

### Short Description (80 characters max)
```
An infinite canvas for brainstorming, mind mapping, and doodling your ideas.
```

### Full Description (4000 characters max)
```
DraftIt is a blazingly fast infinite canvas built entirely in Jetpack Compose. Designed for brainstorming, mind mapping, designing systems, or just doodling your ideas out.

✨ KEY FEATURES

• Infinite Canvas — Pan, zoom, and draw without boundaries
• Multiple Brush Types — Pen, marker, and more with customizable colors and sizes
• Background Removal — ML-powered subject segmentation to cut out backgrounds from images
• Voice Memos — Record audio notes directly on your canvas
• Dark & Light Mode — Beautiful themes that adapt to your preference
• Performance Controls — Adjustable framerate, liquid physics, and haptic feedback
• Project Management — Create, rename, and organize multiple canvas projects
• Export — Save your work as images

🎨 DESIGNED FOR CREATORS

Whether you're a student sketching notes, a designer wireframing interfaces, or just someone who thinks better with a pen — DraftIt gives you a boundless space to think freely.

🔒 PRIVACY FIRST

DraftIt is a local-first app. Your data stays on your device. No accounts, no cloud, no tracking. Period.

📱 BUILT WITH MODERN ANDROID

Powered by Jetpack Compose and Material Design 3, DraftIt is optimized for smooth performance on phones and tablets.
```

---

## 7. Graphics Requirements

| Asset | Size | Format | Status |
|---|---|---|---|
| App icon | 512 × 512 px | PNG, 32-bit, no alpha channel | ✅ Already have `ic_launcher-playstore.png` |
| Feature graphic | 1024 × 500 px | PNG or JPEG | ❌ Need to create |
| Phone screenshots | Min 320px wide, max 3840px, 16:9 or 9:16 ratio | PNG or JPEG | ❌ Need to take (minimum 2, maximum 8) |
| 7-inch tablet screenshots | Optional | PNG or JPEG | Optional |
| 10-inch tablet screenshots | Optional | PNG or JPEG | Optional |

### How to take screenshots:
1. Run your app on a Pixel emulator (Pixel 7 or 8 recommended)
2. Screenshots to take:
   - Dashboard / Home screen (with a few projects)
   - Canvas with some drawings on it
   - Canvas toolbar / tools visible
   - Settings screen (dark mode)
3. Use Android Studio → Logcat window → Camera icon → Save

### Feature graphic:
A 1024×500 promotional banner displayed at the top of your Play Store listing. Should show:
- App name "DraftIt"
- A visual preview of the canvas
- Clean, professional design

---

## 8. Content Rating Answers

Navigate to: **Policy > App content > Content rating > Start questionnaire**

| Question | Answer |
|---|---|
| Email address | draftit.support@gmail.com |
| Category | **Utility, Productivity, Communication, or Other** |
| Violence | No |
| Sexual content | No |
| Language (profanity) | No |
| Controlled substances | No |
| User-generated content | **Yes** (users can draw/write anything) |
| Users can share content with others | No (local only) |
| Location sharing | No |
| Users can communicate | No |
| Users can purchase | No |
| Advertisement | No |

**Expected rating: PEGI 3 / Everyone**

---

## 9. Data Safety Form Answers

Navigate to: **Policy > App content > Data safety**

### Overview questions:

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all collected data encrypted in transit? | **N/A** (no data leaves device) |
| Do you provide a way for users to request that their data is deleted? | **Yes** (delete projects in-app, or uninstall) |

### Data types:

| Data type | Collected? | Shared? | Purpose |
|---|---|---|---|
| Audio files | **Yes** (voice memos) | **No** | App functionality — recorded and stored locally only |
| Photos/Videos | **No** (imported via Photo Picker, not stored by app separately) | **No** | — |
| Files and docs | **No** (PDFs accessed via Document Picker) | **No** | — |
| Personal info | **No** | **No** | — |
| Location | **No** | **No** | — |
| App activity | **No** | **No** | — |
| Web browsing | **No** | **No** | — |
| Device or other IDs | **No** | **No** | — |
| Financial info | **No** | **No** | — |
| Contacts | **No** | **No** | — |
| Messages | **No** | **No** | — |

**NOTE:** If you later enable Firebase Crashlytics/Analytics, you must update this form to declare crash logs, device info, and app diagnostics.

---

## 10. All Other Declarations

These are quick yes/no forms in **Policy > App content**:

| Declaration | Your Answer |
|---|---|
| **Ads** | No, my app does not contain ads |
| **Target audience** | 18 and above (safest for freeform drawing with UGC) |
| **App access** | All functionality is available without special access (no login) |
| **Government apps** | No |
| **Financial features** | No |
| **Health apps** | No |

---

## 11. Post-Launch: Firebase & In-App Review

These are **optional but recommended** for after your first release is live.

### Firebase Crashlytics (crash reporting)

Your build.gradle.kts already has the Firebase dependencies commented out. To enable:

1. Go to https://console.firebase.google.com
2. Create a project, add your Android app (`com.flash.draft`)
3. Download `google-services.json` → put in `app/` folder
4. Uncomment in `app/build.gradle.kts`:
   ```kotlin
   alias(libs.plugins.google.services)
   alias(libs.plugins.firebase.crashlytics)
   ```
   And in dependencies:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
   implementation("com.google.firebase:firebase-analytics")
   implementation("com.google.firebase:firebase-crashlytics")
   ```
5. Update the Data Safety form in Play Console to declare crash logs

### In-App Review (play-review)

Your version catalog already has `play-review` defined. To enable:

1. Uncomment in dependencies:
   ```kotlin
   implementation(libs.play.review)
   ```
2. Add review prompt logic in your ViewModel/Activity after the user has used the app a few times

---

## 12. Files Modified Reference

| File | Path | What |
|---|---|---|
| build.gradle.kts | `app/build.gradle.kts` | R8, signing, imports |
| proguard-rules.pro | `app/proguard-rules.pro` | Full R8 keep rules |
| .gitignore | `.gitignore` (root) | Signing files blocked |
| keystore.properties.example | Root | Signing template |

**Files NOT modified** (no changes needed):
- AndroidManifest.xml — already correct
- strings.xml — already has app_name
- themes.xml — splash screen already configured
- All Kotlin source files — no changes needed for publishing

---

## 13. Troubleshooting

### "Cannot find keystore" error
→ Make sure `storeFile` path in `keystore.properties` is correct. It's relative to the `app/` directory. If your keystore is at the project root, use `../draftit-release.jks`.

### Release build crashes but debug works
→ R8 is stripping a class. Check `proguard-rules.pro` and add a `-keep` rule for the crashing class. Run with `--stacktrace` to see what's missing.

### "App signing certificate and the certificate used to sign the APK do not match"
→ You're uploading with a different keystore than what Play Console expects. If this is your first upload, make sure you opted into Play App Signing.

### "Your APK is not zip aligned"
→ Use `bundleRelease` (AAB) instead of `assembleRelease` (APK). Google Play requires AAB.

### "Version code 1 has already been used"
→ Increment `versionCode` in `build.gradle.kts` for each new upload. Change from `1` to `2`, etc.

### Build takes too long
→ First R8 build is slow (~2min). Subsequent builds are cached and faster. If it's consistently slow, increase Gradle heap in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

---

## Quick Command Reference

```bash
# Create keystore
keytool -genkeypair -v -keystore draftit-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias draftit

# Build debug APK (testing)
.\gradlew assembleDebug

# Build release APK (testing R8)
.\gradlew assembleRelease

# Build release AAB (for Play Store upload)
.\gradlew bundleRelease

# Clean build (if things are weird)
.\gradlew clean

# Build with stacktrace (debugging crashes)
.\gradlew assembleRelease --stacktrace
```

---

*End of document. Everything above is self-contained and can be handed to any AI model or used as a reference.*
