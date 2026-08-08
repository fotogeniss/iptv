# =====================================================================
# ΚΑΝΟΝΕΣ R8 — ΤΙ ΔΕΝ ΕΠΙΤΡΕΠΕΤΑΙ ΝΑ ΠΕΙΡΑΞΕΙ
# =====================================================================
#
# Το R8 αφαιρεί ό,τι δεν βλέπει να καλείται. Ό,τι φορτώνεται με reflection ή
# μέσω JNI ΔΕΝ φαίνεται — και σβήνεται σιωπηλά. Το αποτέλεσμα δεν είναι σφάλμα
# μεταγλώττισης αλλά κατάρρευση στη συσκευή, μόνο στο release, μόνο όταν φτάσεις
# στη συγκεκριμένη λειτουργία. Γι' αυτό κάθε κανόνας εδώ έχει λόγο γραμμένο.

# ---- OkHttp / Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- libVLC ----
# Ολόκληρη η επιφάνειά του καλείται από native κώδικα μέσω JNI: τα ονόματα
# κλάσεων και μεθόδων είναι συμβόλαιο με τη C βιβλιοθήκη.
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.libvlc.util.** { *; }
-dontwarn org.videolan.libvlc.**

# ---- Media3 / ExoPlayer ----
# Οι renderers και οι decoders επιλέγονται δυναμικά κατά την αναπαραγωγή, με
# βάση τον κωδικοποιητή της ροής — το R8 δεν μπορεί να το προβλέψει στατικά.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---- FFmpeg decoders (nextlib) ----
# ΚΡΙΣΙΜΟ και έλειπε: φορτώνονται μέσω JNI. Χωρίς αυτό, το release έχανε τον ήχο
# σε AC3/EAC3/DTS — δηλαδή στις περισσότερες ξένες ταινίες. Το debug έπαιζε μια
# χαρά, οπότε το πρόβλημα εμφανιζόταν μόνο στους χρήστες.
-keep class io.github.anilbeesetti.nextlib.** { *; }
-dontwarn io.github.anilbeesetti.nextlib.**

# ---- NanoHTTPD ----
# Ο relay server· οι handlers εντοπίζονται με reflection.
-keep class org.nanohttpd.** { *; }
-dontwarn org.nanohttpd.**

# ---- Coil ----
-dontwarn coil.**

# ---- Firebase Crashlytics optional profiling (newer Android API) ----
# Crashlytics checks for these platform classes only on Android versions that
# provide them. With compileSdk 35 they are absent from android.jar, so R8
# reports them as missing even though no supported device path loads them.
# Keep this suppression deliberately narrow; do not replace it with android.os.**.
-dontwarn android.os.ProfilingTrigger$Builder
-dontwarn android.os.ProfilingTrigger

# ---- Μοντέλα δεδομένων ----
# Σειριοποιούνται σε/από JSON με ονόματα πεδίων. Μετονομασία = σιωπηλά κενά
# αντικείμενα, όχι σφάλμα.
-keep class com.prelude.iptv.data.** { *; }

# ---- Kotlin ----
-dontwarn kotlinx.coroutines.**
-keepclassmembers class ** {
    @kotlin.jvm.JvmStatic *;
}
