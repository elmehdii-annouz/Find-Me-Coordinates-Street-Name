plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.dev.localisation_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dev.localisation_app"
        minSdk = 27
        targetSdk = 34
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.location)
    implementation(libs.play.services.location.v2101)
    implementation(libs.play.services.maps)
    implementation (libs.play.services.location)
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation (libs.cardview)
    implementation(libs.play.services.ads.v2340)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


}