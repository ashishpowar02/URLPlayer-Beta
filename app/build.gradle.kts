plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.samyak.urlplayerbeta"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.samyak.urlplayerbeta"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "UrlPlayerBeta-${variant.versionName}-${variant.baseName}.apk"
                output.outputFileName = outputFileName
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    //for exoplayer
    implementation(libs.exoplayerCore)
    implementation(libs.exoplayerUi)

    //for playing online content
    implementation(libs.exoplayerDash)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.app.update.ktx)
//    implementation(libs.firebase.database.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.code.gson:gson:2.8.8")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("com.airbnb.android:lottie:4.2.2")


    //for vertical progress bar
    implementation(libs.verticalSeekbar)

    //for doubleTapFeature
    implementation(libs.doubleTapPlayerView)

    //custom chrome tabs for integrating youtube
    implementation(libs.androidx.browser)

    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.database.ktx)

    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")


    // Gauge Library
//    implementation("com.github.Gruzer:simple-gauge-android:0.3.1")

//    //Toast
//    implementation("com.github.samyak2403:TastyToasts:1.0.2")

    implementation("com.google.android.gms:play-services-ads:23.6.0")

    implementation("com.facebook.shimmer:shimmer:0.5.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0") // For older versions of LiveData

    // Add Cast dependencies
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
    implementation("androidx.mediarouter:mediarouter:1.6.0")
}