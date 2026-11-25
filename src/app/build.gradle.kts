plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.notium.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.notium.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // Solo le architetture dei telefoni reali (dal 2015 in poi): APK piu' leggero e build piu' veloce.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // Firma release: solo se le variabili d'ambiente sono presenti (vedi docs/DISTRIBUZIONE.md).
    val releaseKeystore: String? = System.getenv("NOTIUM_KEYSTORE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("NOTIUM_STORE_PASSWORD")
                keyAlias = System.getenv("NOTIUM_KEY_ALIAS")
                keyPassword = System.getenv("NOTIUM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    // L'analisi lint della release non serve per produrre l'APK e rallenta la build di minuti.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    // Nessun metadato di dipendenza firmato nell'APK/AAB.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Installa i profili di avvio (baseline profile) inclusi nelle librerie Compose: avvio e scorrimento piu' fluidi.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Biometria (BiometricPrompt)
    implementation("androidx.biometric:biometric:1.1.0")

    // Database cifrato (SQLite + SQLCipher) e Argon2id
    implementation("net.zetetic:sqlcipher-android:4.6.1@aar")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("com.lambdapioneer.argon2kt:argon2kt:1.6.0")
}
