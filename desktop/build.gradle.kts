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
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.desktop:desktop:1.7.3")
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)

                implementation(libs.materialKolor)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
                implementation(libs.apache.lang3)
                implementation(libs.json)
                // Explicit Skiko native runtime for Windows/Linux
                implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.37.3")
                implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.9.37.3")
                implementation("org.jetbrains.skiko:skiko:0.9.37.3")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "echo.music.iad1tya.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "EchoMusic"
            packageVersion = "1.4.3"
            description = "Echo Music Desktop Application"
            copyright = "Copyright 2025 Echo Music"
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
            linux {
                packageName = "echo-music"
                debMaintainer = "Echo Music <hello@echomusic.fun>"
                menuGroup = "Audio"
                appCategory = "Audio"
            }
        }
        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}
