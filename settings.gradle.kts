pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }

}
rootProject.name = "WebRtcKmpDemo"
enableFeaturePreview("GRADLE_METADATA")

include(":shared", ":androidApp")

