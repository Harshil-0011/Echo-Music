package echo.music.iad1tya.platform

import kotlinx.coroutines.flow.StateFlow

expect class LocalPreferences() {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)
    fun <T> observeKey(key: String, defaultValue: T): StateFlow<T>
    fun clear()
    fun remove(key: String)
}

expect object SystemUtils {
    fun copyToClipboard(text: String, label: String = "Echo Music")
    fun getClipboardText(): String?
    fun openUrl(url: String)
    fun showNotification(title: String, message: String)
    fun showToast(message: String)
    fun pickFile(): java.io.File?
}
