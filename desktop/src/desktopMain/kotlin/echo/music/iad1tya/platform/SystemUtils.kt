package echo.music.iad1tya.platform

import java.awt.Desktop
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.net.URI

actual object SystemUtils {
    actual fun copyToClipboard(text: String, label: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun getClipboardText(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                val os = System.getProperty("os.name").lowercase()
                if (os.contains("win")) {
                    ProcessBuilder("cmd", "/c", "start", url).start()
                } else if (os.contains("mac")) {
                    ProcessBuilder("open", url).start()
                } else {
                    ProcessBuilder("xdg-open", url).start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun showNotification(title: String, message: String) {
        try {
            if (SystemTray.isSupported()) {
                val tray = SystemTray.getSystemTray()
                val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
                val trayIcon = TrayIcon(image, "Echo Music")
                trayIcon.isImageAutoSize = true
                tray.add(trayIcon)
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO)
                Thread {
                    Thread.sleep(5000)
                    try { tray.remove(trayIcon) } catch (_: Exception) {}
                }.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun showToast(message: String) {
        println("[Toast] $message")
        showNotification("Echo Music", message)
    }
}
