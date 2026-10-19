@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "echomusic"
include(
    ":app",
    ":innertube",
    ":paxsenixlyrics",
    ":kugou",
    ":betterlyrics",
    ":lrclib",
    ":simpmusic",
    ":youlyplus",
    ":shazamkit",
    ":artistvideo",
    ":canvas",
    ":echomusiccanvas",
    ":applecanvas",
    ":unison",
    ":core",
    ":lyrics",
    ":playback",
    ":usbaudio",
    ":metadata",
    ":dsp-core",
    ":audio-dsp",
    ":domain",
    ":desktop"
)
