# ============================
# DraftIt ProGuard / R8 Rules
# ============================

# Keep source file names and line numbers for meaningful stack traces (Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================
# Kotlin & Coroutines
# ============================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# ============================
# Google ML Kit
# ============================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ============================
# Room Database
# ============================
# Keep Room-generated classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
# Keep the NoteEntity specifically
-keep class com.flash.draft.NoteEntity { *; }

# ============================
# App Models (serialized/reflected)
# ============================
-keep class com.flash.draft.model.** { *; }

# ============================
# Jetpack Compose
# ============================
# Compose uses reflection for @Composable detection — keep annotations
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ============================
# Compose Navigation
# ============================
-keep class androidx.navigation.** { *; }
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# ============================
# Coil (Image Loading)
# ============================
-dontwarn coil.**
-keep class coil.** { *; }

# ============================
# AndroidX Ink (alpha library)
# ============================
-keep class androidx.ink.** { *; }
-dontwarn androidx.ink.**

# ============================
# Liquid Library (io.github.fletchmckee.liquid)
# ============================
-keep class io.github.fletchmckee.liquid.** { *; }
-dontwarn io.github.fletchmckee.liquid.**

# ============================
# Composables Icons (Lucide)
# ============================
-keep class com.composables.icons.** { *; }
-dontwarn com.composables.icons.**

# ============================
# Google Play Services
# ============================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================
# Google Fonts Provider
# ============================
-keep class androidx.compose.ui.text.googlefonts.** { *; }

# ============================
# AndroidX Splash Screen
# ============================
-keep class androidx.core.splashscreen.** { *; }

# ============================
# Firebase (for when you enable it)
# ============================
# -keep class com.google.firebase.** { *; }
# -dontwarn com.google.firebase.**