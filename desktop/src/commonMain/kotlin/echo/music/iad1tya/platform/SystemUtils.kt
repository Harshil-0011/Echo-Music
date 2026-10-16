package echo.music.iad1tya.platform

expect object SystemUtils {
    fun copyToClipboard(text: String, label: String = "Echo Music")
    fun getClipboardText(): String?
    fun openUrl(url: String)
    fun showNotification(title: String, message: String)
    fun showToast(message: String)
}
