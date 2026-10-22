package echo.music.iad1tya.ui

import androidx.compose.ui.graphics.Color

// Shared UI constants for Nothing OS 5.0 / Void Slate aesthetic
val VoidSlateDark = Color(0xFF0B0F17)
val VoidSlateSurface = Color(0xFF131924)
val VoidSlateCard = Color(0xFF1B2232)
val VoidSlateCardHover = Color(0xFF232B3E)
val ElectricCyan = Color(0xFF00F0FF)
val TextPrimary = Color.White
val TextSecondary = Color(0xFF94A3B8)
val AccentMagenta = Color(0xFFFF2D95)
val AccentPurple = Color(0xFF7C4DFF)

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}