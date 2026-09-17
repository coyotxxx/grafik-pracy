plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "pl.grafik.pracy"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.grafik.pracy"
        minSdk = 26
        targetSdk = 35
        versionCode = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionName = "1.29.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    /**
     * Dwa warianty aplikacji.
     *
     * „klasyczny" buduje się dokładnie z tego, co w src/main — bit w bit tak jak dotąd.
     * „nowy" dokłada src/nowy (własny ekran startowy, tokeny, fonty) i instaluje się
     * OBOK na telefonie, pod innym identyfikatorem. Dzięki temu działająca aplikacja
     * Macieja zostaje nietknięta, cokolwiek stanie się z nowym wyglądem.
     */
    flavorDimensions += "wyglad"
    productFlavors {
        create("klasyczny") {
            dimension = "wyglad"
            isDefault = true
        }
        create("nowy") {
            dimension = "wyglad"
            applicationIdSuffix = ".nowy"
            versionNameSuffix = "-nowy"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests { isReturnDefaultValues = true } }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)
    implementation(libs.play.services.location)
    implementation(libs.androidx.work)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.coroutines.test)
}
