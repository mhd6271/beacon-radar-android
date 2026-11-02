plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.maxm.beaconradar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maxm.beaconradar"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "0.6"
    }

    // Fester Signaturschlüssel, damit neue APKs sich drüberinstallieren lassen.
    // CI: aus GitHub-Secrets; lokal: KEYSTORE_FILE usw. aus ~/.android-signing/beacon-radar/.
    // Ohne KEYSTORE_FILE gilt der normale Debug-Schlüssel des Rechners.
    val keystoreFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }
    signingConfigs {
        if (keystoreFile != null) {
            create("fixed") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (keystoreFile != null) signingConfig = signingConfigs.getByName("fixed")
        }
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
}
