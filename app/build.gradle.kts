plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystoreEnv = System.getenv("KEYSTORE_FILE")
val releaseStoreFile = if (keystoreEnv != null) file(keystoreEnv) else rootProject.file("spendwise-release.jks")

android {
    namespace = "com.shahid.spendwise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shahid.spendwise"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE")?.toIntOrNull()) ?: 10000
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "REPO_OWNER", "\"flexblesoftware\"")
        buildConfigField("String", "REPO_NAME", "\"spendwise\"")
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile.exists()) {
                storeFile = releaseStoreFile
                val storePass = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "SwKeyStore2026!7xQm9K4vLpW"
                val keyPass = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: storePass
                storePassword = storePass
                keyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "spendwise"
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseStoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Background WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Biometrics & Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // Lottie Animations
    implementation(libs.lottie.compose)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Image loading for receipts
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("generateLauncherIcons") {
    description = "Generates high-precision raster launcher icons for legacy Android densities and Play Store"
    doLast {
        val bgDarkGreen = java.awt.Color(0x0B, 0x4D, 0x3B)
        val fgBrightGreen = java.awt.Color(0x2E, 0xCC, 0x8F)

        val densities = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )

        fun renderIcon(size: Int, isRound: Boolean): java.awt.image.BufferedImage {
            val img = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val g2 = img.createGraphics()
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE)

            g2.color = bgDarkGreen
            if (isRound) {
                g2.fill(java.awt.geom.Ellipse2D.Float(0f, 0f, size.toFloat(), size.toFloat()))
            } else {
                g2.fillRect(0, 0, size, size)
            }

            val scale = size.toFloat() / 108f
            g2.color = fgBrightGreen

            val barHeight = 8f * scale
            val barRadius = 4f * scale
            val barGap = 6f * scale
            val totalGlyphHeight = 36f * scale
            val startY = (size.toFloat() - totalGlyphHeight) / 2f

            val barWidths = floatArrayOf(48f * scale, 36f * scale, 24f * scale)

            for (i in 0 until 3) {
                val w = barWidths[i]
                val x = (size.toFloat() - w) / 2f
                val y = startY + i * (barHeight + barGap)
                val pill = java.awt.geom.RoundRectangle2D.Float(x, y, w, barHeight, barRadius * 2, barRadius * 2)
                g2.fill(pill)
            }

            g2.dispose()
            return img
        }

        val resDir = file("src/main/res")
        densities.forEach { (density, size) ->
            val dir = File(resDir, "mipmap-$density")
            dir.mkdirs()
            val squareImg = renderIcon(size, isRound = false)
            javax.imageio.ImageIO.write(squareImg, "png", File(dir, "ic_launcher.png"))

            val roundImg = renderIcon(size, isRound = true)
            javax.imageio.ImageIO.write(roundImg, "png", File(dir, "ic_launcher_round.png"))
        }

        val playStoreImg = renderIcon(512, isRound = false)
        val playStoreFile = rootProject.file("play_store_512.png")
        javax.imageio.ImageIO.write(playStoreImg, "png", playStoreFile)
        val appPlayStoreFile = file("src/main/play_store_512.png")
        javax.imageio.ImageIO.write(playStoreImg, "png", appPlayStoreFile)

        println("SpendWise: Generated all launcher icons and Play Store 512x512 icon successfully!")
    }
}

tasks.named("preBuild") {
    dependsOn("generateLauncherIcons")
}
