import com.nikitakrapo.configuration.applyCompose
import com.nikitakrapo.configuration.setupAndroidApp

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

setupAndroidApp(
    applicationId = "com.nikitakrapo.monkeybusiness",
    versionCode = 1,
    versionName = "1.0"
)

applyCompose()

android {
    buildTypes {
        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.design)
    implementation(projects.features.analytics)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.activity.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
