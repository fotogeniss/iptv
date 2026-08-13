plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase belongs to the publisher, so a checkout must still configure and
// build without somebody else's project credentials. Dropping the publisher's
// app/google-services.json in place activates the two build plugins and mapping
// upload support; until then the in-app diagnostics screen stays in setup mode.
val crashReportingConfigured = file("google-services.json").isFile
if (crashReportingConfigured) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

android {
    namespace = "com.prelude.iptv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.prelude.iptv"
        minSdk = 26
        targetSdk = 35
        versionCode = 137
        versionName = "1.65.0"
        resourceConfigurations += listOf("en", "el")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The in-app picker remains QA-only until every release surface has
        // matching English/Greek resources. Flip only after the parity gate.
        buildConfigField("boolean", "LOCALIZATION_PARITY_COMPLETE", "false")
        buildConfigField(
            "boolean",
            "FIREBASE_CRASH_REPORTING_CONFIGURED",
            crashReportingConfigured.toString()
        )
    }

    // ---------------------------------------------------------------------
    // Ένα APK ανά αρχιτεκτονική.
    // Το libVLC κουβαλάει native βιβλιοθήκες για 4 αρχιτεκτονικές· μαζεμένες σε
    // ένα APK βγαίνουν ~150-250MB και πολλά μποξάκια δεν έχουν χώρο να το
    // εγκαταστήσουν («Η εφαρμογή δεν εγκαταστάθηκε»). Έτσι κάθε APK κρατάει ΜΟΝΟ
    // τη δική του (~40-60MB).
    // ---------------------------------------------------------------------
    splits {
        abi {
            isEnable = true
            reset()
            // arm64-v8a  -> νεότερες τηλεοράσεις/κινητά
            // armeabi-v7a-> παλιά μποξάκια (32-bit) — τα περισσότερα φθηνά boxes
            // x86_64     -> emulator
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true   // + ένα «όλα σε ένα» ως εφεδρικό
        }
    }

    buildTypes {
        debug {
            // Local developer builds may exercise every premium flow without
            // depending on a Play Store test purchase.
            buildConfigField("boolean", "PREMIUM_QA_OVERRIDE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ---------------------------------------------------------
            // ΥΠΟΓΡΑΦΗ RELEASE ΜΕ ΤΟ ΚΛΕΙΔΙ DEBUG
            // ---------------------------------------------------------
            // Χωρίς signingConfig, το «Build → Generate APKs» με επιλεγμένη τη
            // variant release βγάζει ΑΝΥΠΟΓΡΑΦΟ APK, που η τηλεόραση αρνείται να
            // εγκαταστήσει. Το αποτέλεσμα ήταν να μένει κανείς στο debug — και να
            // κρίνει την ταχύτητα της εφαρμογής από το build που είναι σχεδιασμένο
            // να είναι αργό.
            //
            // Το κλειδί debug αρκεί για εγκατάσταση στη δική σου συσκευή. ΔΕΝ
            // αρκεί για δημοσίευση: για Play Store χρειάζεται δικό σου keystore
            // μέσω «Generate Signed App Bundle or APK…».
            signingConfig = signingConfigs.getByName("debug")
            // Public builds must derive access exclusively from a verified
            // Google Play entitlement.
            buildConfigField("boolean", "PREMIUM_QA_OVERRIDE", "false")
        }
        create("qa") {
            // Keep release shrinking/optimisation so owner testing catches
            // release-only R8 and resource-shrinking problems.
            initWith(getByName("release"))
            // QA must coexist with release and keep isolated test data. This
            // prevents an owner test build from replacing production data.
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            signingConfig = signingConfigs.getByName("debug")
            // QA must exercise the same R8/resource-shrinking behavior as
            // release. A minified debuggable variant is contradictory and AGP
            // warns about it, so diagnostics are collected through the device
            // QA runners/logcat rather than android:debuggable.
            isDebuggable = false
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "PREMIUM_QA_OVERRIDE", "true")
        }
    }

    // English is exercised only by owner builds while translation is partial.
    // The public release keeps the existing Greek baseline until the parity gate.
    sourceSets {
        getByName("debug").res.srcDir("src/localizationQa/res")
        getByName("qa").res.srcDir("src/localizationQa/res")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        animationsDisabled = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ---- Tests (JVM, χωρίς emulator: τρέχουν σε δευτερόλεπτα) ----
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ---- Instrumentation / Android TV focus smoke tests ----
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // Google Play Billing. The product price and ownership state always come
    // from Play; no price or paid flag is hard-coded in the application.
    implementation("com.android.billingclient:billing:9.1.0")

    // Opt-in stability reporting only. Google Analytics is deliberately absent.
    // Firebase is initialized by DiagnosticsManager after explicit consent,
    // never by the default startup provider.
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")

    // FFmpeg audio decoders (AC3/EAC3/DTS/TrueHD κ.λπ.) — ήχος σε ξένες ταινίες
    // (version = {media3}-{nextlib}, ταιριάζει με το media3 παραπάνω)
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.7.1-0.9.0")

    // libVLC — Εσωτερικός Player 2 (παίζει ό,τι δεν παίζει ο ExoPlayer)
    implementation("org.videolan.android:libvlc-all:3.6.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Εικόνες (logos καναλιών)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Ελαφρύς HTTP server για το relay (MAC → M3U)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
