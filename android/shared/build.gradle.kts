plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Navigation
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")

            // ViewModel
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

            // Room
            implementation("androidx.room:room-runtime:2.7.0")

            // Kotlinx
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.camera:camera-camera2:1.4.0")
            implementation("androidx.camera:camera-lifecycle:1.4.0")
            implementation("androidx.camera:camera-view:1.4.0")
            implementation("androidx.core:core-ktx:1.15.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }

        iosMain.dependencies {
        }
    }
}

android {
    namespace = "com.sitepinapp.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    listOf(
        "kspAndroid",
        "kspIosX64",
        "kspIosArm64",
        "kspIosSimulatorArm64"
    ).forEach {
        add(it, "androidx.room:room-compiler:2.7.0")
    }
}

// Register Xcode framework copy task for iOS builds
tasks.register("copyFrameworkToXcode") {
    group = "build"
    description = "Copy shared framework to xcode-frameworks directory for Xcode integration"
    dependsOn("linkDebugFrameworkIosSimulatorArm64")
    doLast {
        val xcFrameworkDir = project.layout.buildDirectory.get().asFile
            .resolve("xcode-frameworks/Debug/iphonesimulator")
        xcFrameworkDir.mkdirs()
        val builtFramework = project.layout.buildDirectory.get().asFile
            .resolve("bin/iosSimulatorArm64/debugFramework/shared.framework")
        if (builtFramework.exists()) {
            builtFramework.copyRecursively(xcFrameworkDir.resolve("shared.framework"), overwrite = true)
        }
    }
}
