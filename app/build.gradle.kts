import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}


val makeDummyGoogleServicesJson = run {
    val googleServicesJson = file("google-services.json")
    if (!googleServicesJson.exists()) {
        googleServicesJson.writeText(
            """
            {
              "project_info": {
                "project_number": "1234567890",
                "project_id": "dummy-id",
                "storage_bucket": "dummy.appspot.com"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "1:1234567890:android:abcdef",
                    "android_client_info": {
                      "package_name": "iti.grad.nutriscan"
                    }
                  },
                  "oauth_client": [],
                  "api_key": [
                    {
                      "current_key": "dummy_key"
                    }
                  ],
                  "services": {}
                }
              ],
              "configuration_version": "1"
            }
            """.trimIndent()
        )
    }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

android {
    namespace = "iti.grad.nutriscan"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "iti.grad.nutriscan"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = libs.versions.appVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField(
            "String", "NUTRISCAN_BASE_URL",
            "\"${localProperties.getProperty("NUTRISCAN_BASE_URL", "")}\""
        )
        buildConfigField(
            "String", "KEYCLOAK_BASE_URL",
            "\"${localProperties.getProperty("KEYCLOAK_BASE_URL", "")}\""
        )
        buildConfigField(
            "String", "NEWS_API_BASE_URL",
            "\"${localProperties.getProperty("NEWS_API_BASE_URL", "")}\""
        )
        buildConfigField(
            "String", "NEWS_API_KEY",
            "\"${localProperties.getProperty("NEWS_API_KEY", "")}\""
        )
        buildConfigField(
            "String", "EXERCISES_API_BASE_URL",
            "\"${localProperties.getProperty("EXERCISES_API_BASE_URL", "")}\""
        )
        buildConfigField(
          "String", "NUTRI_GPT_BASE_URL",
           "\"${localProperties.getProperty("NUTRI_GPT_BASE_URL", "")}\""
        )

        manifestPlaceholders["appAuthRedirectScheme"] = "nutriscan"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        val storeFilePath = keystoreProperties["storeFile"] as? String
        val storePass = keystoreProperties["storePassword"] as? String
        val alias = keystoreProperties["keyAlias"] as? String
        val keyPass = keystoreProperties["keyPassword"] as? String

        if (!storeFilePath.isNullOrEmpty() &&
            !storePass.isNullOrEmpty() &&
            !alias.isNullOrEmpty() &&
            !keyPass.isNullOrEmpty()
        ) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.timber)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    
    implementation(project(":presentation"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(libs.androidx.datastore.preferences)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.room.runtime)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.coil3.compose)
    implementation(libs.coil3.network)
    implementation(libs.coil3.gif)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    implementation("net.openid:appauth:0.11.1")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appdistribution.api)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
