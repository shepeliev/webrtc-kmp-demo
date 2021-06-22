plugins {
    kotlin("js")
}

kotlin {
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
        implementation(project(":shared"))
    }

    js {
        browser {
        }
        binaries.executable()
    }
}
