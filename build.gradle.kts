buildscript {
    val kotlinVersion by extra("1.5.10")
    val navVersion by extra("2.3.3")

    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        mavenLocal()
    }
}

//subprojects {
//    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
//        kotlinOptions {
//            jvmTarget = "1.8"
//        }
//    }
//}
