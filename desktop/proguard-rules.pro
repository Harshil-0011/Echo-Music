# Compose Multiplatform / Skia / Skiko
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

# Kotlin stdlib & coroutines
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Material Kolor
-keep class com.materialkolor.** { *; }

# Ktor
-keep class io.ktor.** { *; }

-dontwarn javax.**
-dontwarn sun.**
-dontwarn com.sun.**
-dontwarning org.jetbrains.compose.**
EOF