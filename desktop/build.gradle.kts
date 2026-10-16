import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    attributesSchema {
        attribute(com.android.build.api.attributes.BuildTypeAttr.ATTRIBUTE)
    }
}

configurations.matching { it.isCanBeResolved }.configureEach {
    attributes {
        attribute(
            com.android.build.api.attributes.BuildTypeAttr.ATTRIBUTE,
            objects.named(com.android.build.api.attributes.BuildTypeAttr::class.java, "release")
        )
    }
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                implementation(libs.materialKolor)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.vlcj)
                implementation(libs.apache.lang3)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "echo.music.iad1tya.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "EchoMusic"
            packageVersion = "1.4.3"
            description = "Echo Music Desktop Application"
            copyright = "© 2025 Echo Music"
            vendor = "Echo Music"
            windows {
                menuGroup = "Echo Music"
                upgradeUuid = "6d8f804d-736f-40e1-9538-2a74c2e6f43e"
                iconFile.set(rootProject.file("assets/icon.ico"))
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
            }
        }
    }
}
