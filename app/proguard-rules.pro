# ── ScanFlow ProGuard Rules ──────────────────────────────────────────────

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Domain models ────────────────────────────────────────────────────────
-keep class com.example.scanflow.domain.model.** { *; }

# ── ViewModels ───────────────────────────────────────────────────────────
-keep class com.example.scanflow.ui.screens.files.FilesViewModel { *; }
-keep class com.example.scanflow.ui.screens.preview.PreviewViewModel { *; }
-keep class com.example.scanflow.ui.screens.detail.DocumentDetailViewModel { *; }

# ── AndroidX / Jetpack ───────────────────────────────────────────────────
# Keep ViewModel Factory creation via reflection
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory { *; }

# ── Kotlin ───────────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── CameraX ──────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Coil (image loading) ────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }

# ── Compose Navigation ──────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ── JSON metadata parsing (org.json) ────────────────────────────────────
-keep class org.json.** { *; }

# ── Android components ──────────────────────────────────────────────────
-keep class * extends android.app.Activity
-keep class * extends android.content.ContentProvider
-keep class * extends android.content.BroadcastReceiver

# ── FileProvider (sharing PDFs) ──────────────────────────────────────────
-keep class androidx.core.content.FileProvider { *; }

# ── Prevent stripping of Compose runtime ─────────────────────────────────
-dontwarn androidx.compose.**

# ── Remove logging in release ────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
