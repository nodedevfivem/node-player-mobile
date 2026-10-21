import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingProps = Properties()
val signingFile = rootProject.file("signing.properties")
if (signingFile.exists()) signingFile.inputStream().use { signingProps.load(it) }

android {
    namespace = "com.nodedevelopment.nodeplayermobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nodedevelopment.nodeplayermobile"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
        buildConfigField("String", "GITHUB_OWNER", "\"nodedevfivem\"")
        // Create this repository on GitHub before publishing updates.
        buildConfigField("String", "GITHUB_REPO", "\"NODE-Player-Mobile\"")
    }
    signingConfigs {
        if (signingFile.exists()) {
            create("releaseConfig") {
                storeFile = rootProject.file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (signingFile.exists()) signingConfig = signingConfigs.getByName("releaseConfig")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.concurrent:concurrent-futures:1.2.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
