import org.jetbrains.kotlin.gradle.plugin.mpp.Framework

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

version = "1.0.0"
val webRtcKmmVersion = "0.89.4"

kotlin {
    cocoapods {
        ios.deploymentTarget = "11.0"
        summary = "WebRTC Kotlin Multiplatform sample shared module"
        homepage = "https://github.com/shepeliev/webrtc-kmp"

        podfile = project.file("../iosAppSwift/Podfile")
        frameworkName = "shared"
    }

    android()
    ios {
        binaries
            .filterIsInstance<Framework>()
            .forEach {
                it.transitiveExport = true
                it.export("com.shepeliev:webrtc-kmp:$webRtcKmmVersion")
            }
    }

    js {
        useCommonJs()
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.shepeliev:webrtc-kmp:$webRtcKmmVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
            }
        }
        val commonTest by getting
        val androidMain by getting
        val androidTest by getting
        val iosMain by getting
        val iosTest by getting
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}
