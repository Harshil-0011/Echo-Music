package echo.music.iad1tya.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import echo.music.iad1tya.platform.AudioPlayerEngine
import echo.music.iad1tya.platform.LocalPreferences
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center),
        size = DpSize(1280.dp, 800.dp)
    )

    val playerEngine = remember { AudioPlayerEngine() }
    val preferences = remember { LocalPreferences() }

    Window(
        onCloseRequest = {
            playerEngine.release()
            this.exitApplication()
        },
        title = "Echo Music Desktop",
        state = windowState,
        undecorated = false
    ) {
        window.minimumSize = Dimension(900, 600)
        DesktopApp(
            playerEngine = playerEngine,
            preferences = preferences
        )
    }
}
