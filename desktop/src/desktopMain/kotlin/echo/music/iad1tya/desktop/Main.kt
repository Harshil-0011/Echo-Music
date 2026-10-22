package echo.music.iad1tya.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import echo.music.iad1tya.desktop.db.MusicDatabase
import echo.music.iad1tya.desktop.lyrics.DesktopLyricsProvider
import echo.music.iad1tya.desktop.network.InnerTubeClient
import echo.music.iad1tya.desktop.playback.DesktopPlaybackEngine
import echo.music.iad1tya.platform.LocalPreferences
import java.awt.Dimension
import java.io.File

fun main() = application {
    // Smooth live resizing on Windows (avoids frame tearing / black flashes)
    System.setProperty("skiko.rendering.windows.direct3DSynchronousLiveResize", "true")

    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center),
        size = DpSize(1280.dp, 800.dp)
    )

    val dbDir = File(System.getProperty("user.home"), ".echomusic/db")
    val database = MusicDatabase(dbDir)
    val innerTube = InnerTubeClient()
    val lyricsProvider = DesktopLyricsProvider()
    val playerEngine = DesktopPlaybackEngine(innerTube)

    database.init()

    Window(
        onCloseRequest = {
            database.close()
            playerEngine.release()
            exitApplication()
        },
        title = "Echo Music",
        state = windowState,
        undecorated = false
    ) {
        window.minimumSize = Dimension(900, 600)
        window.title = "Echo Music Desktop"
        DesktopApp(
            playerEngine = playerEngine,
            database = database,
            preferences = LocalPreferences(),
            innerTube = innerTube,
            lyricsProvider = lyricsProvider
        )
    }
}
