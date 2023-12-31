plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.ucsb.ece251.DATQ.mymelody"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.ucsb.ece251.DATQ.mymelody"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "edu.ucsb.ece251.DATQ.mymelody"
        manifestPlaceholders["redirectSchemeName"] = "mymelody"
        manifestPlaceholders["redirectHostName"] = "callback"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.material:material:1.11.0")
    implementation ("com.google.firebase:firebase-database")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-common:20.4.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.auth0.android:jwtdecode:2.0.0")
    implementation("net.openid:appauth:0.11.1")
    implementation("com.spotify.android:auth:1.2.6")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("com.google.code.gson:gson:2.8.9")

}