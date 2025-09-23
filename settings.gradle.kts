pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Plugins
            version("kotlin", "2.1.21")
            plugin("multiplatform", "org.jetbrains.kotlin.multiplatform").versionRef("kotlin")
            plugin("serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("kover", "org.jetbrains.kotlinx.kover").version("0.8.3")
            plugin("detekt", "io.gitlab.arturbosch.detekt").version("1.23.8")
            plugin("dokka", "org.jetbrains.dokka").version("2.0.0")
            plugin("ksp", "com.google.devtools.ksp").version("2.1.21-2.0.2")
            plugin("maven", "com.vanniktech.maven.publish").version("0.30.0")

            // Kaccelero
            version("kaccelero", "0.6.6")
            library("kaccelero-core", "dev.kaccelero", "core").versionRef("kaccelero")

            // kdriver
            version("kdriver", "0.2.12")
            library("kdriver-core", "dev.kdriver", "core").versionRef("kdriver")

            // Tests
            library("tests-mockk", "io.mockk:mockk:1.13.12")
            library("tests-jsoup", "org.jsoup:jsoup:1.16.2")
            library("tests-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
    }
}

rootProject.name = "kdriver-nextjs"
include(":nextjs")
include(":nextjs-engine")
