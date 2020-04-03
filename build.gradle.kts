plugins {
    kotlin("jvm") version "1.3.71"
}

allprojects {
    apply(plugin="kotlin")

    repositories {
        jcenter()
    }

    dependencies {
        api(kotlin("stdlib-jdk8"))
        api(kotlin("reflect"))
        api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")
    }

    tasks {
        compileKotlin {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
}