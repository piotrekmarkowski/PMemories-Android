import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// 15.09.2026, Etap 7 — Google Maps needs its own API key, a SEPARATE
// blocker from Firebase's `google-services.json` (same class of problem:
// requires the user to create it in a browser — Google Cloud Console,
// "Maps SDK for Android" enabled — then paste it here). Read from
// `local.properties` (already gitignored, never committed) rather than
// hardcoding, same spirit as `sdk.dir` in that file. Empty string when
// absent — the app still builds and runs, the map screens just show blank
// map tiles until a real key is dropped in.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.piotrmarkowski.pmemories"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.piotrmarkowski.pmemories"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Etap 4 — Ranking + logowanie. BoM ustala spójne wersje dla
    // firebase-auth/firebase-firestore, ten sam wzorzec co Compose BoM
    // wyżej — nie trzeba ręcznie synchronizować wersji poszczególnych
    // modułów Firebase.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    // Credential Manager — nowoczesny, zalecany sposób logowania Google na
    // Androidzie (zastąpił starsze, wycofywane `GoogleSignInClient`/
    // `GoogleSignInApi`), spójne z resztą projektu: zawsze najnowsze,
    // aktualnie zalecane API, nie przestarzałe.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Etap 7 — Travel Map.
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
}
