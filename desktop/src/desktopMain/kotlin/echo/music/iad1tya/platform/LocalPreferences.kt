package echo.music.iad1tya.platform

import java.io.File
import java.util.Properties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class LocalPreferences actual constructor() {
    private val prefFile: File
    private val dataMap = mutableMapOf<String, String>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val lock = Any()

    init {
        val appData = System.getenv("APPDATA")
        val xdgConfig = System.getenv("XDG_CONFIG_HOME")
        val dir = if (!appData.isNullOrEmpty()) {
            File(appData, "EchoMusic")
        } else if (!xdgConfig.isNullOrEmpty()) {
            File(xdgConfig, "echomusic")
        } else {
            File(System.getProperty("user.home"), ".echomusic")
        }
        dir.mkdirs()
        prefFile = File(dir, "preferences.properties")
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!prefFile.exists()) return
        try {
            val props = Properties()
            prefFile.inputStream().use { props.load(it) }
            synchronized(lock) {
                dataMap.clear()
                for ((k, v) in props) {
                    dataMap[k.toString()] = v.toString()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun saveToDisk() {
        try {
            val props = Properties()
            synchronized(lock) {
                for ((k, v) in dataMap) {
                    props.setProperty(k, v)
                }
            }
            prefFile.parentFile.mkdirs()
            prefFile.outputStream().use { props.store(it, "Echo Music Preferences") }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun notifyFlow(key: String, value: Any?) {
        synchronized(lock) {
            (flows[key] as? MutableStateFlow<*>)?.let {
                @Suppress("UNCHECKED_CAST")
                (it as MutableStateFlow<Any?>).value = value
            }
        }
    }

    actual fun getString(key: String, defaultValue: String): String {
        synchronized(lock) { return dataMap[key] ?: defaultValue }
    }

    actual fun putString(key: String, value: String) {
        synchronized(lock) { dataMap[key] = value }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val str = synchronized(lock) { dataMap[key] } ?: return defaultValue
        return str.toBooleanStrictOrNull() ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        synchronized(lock) { dataMap[key] = value.toString() }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        val str = synchronized(lock) { dataMap[key] } ?: return defaultValue
        return str.toIntOrNull() ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        synchronized(lock) { dataMap[key] = value.toString() }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        val str = synchronized(lock) { dataMap[key] } ?: return defaultValue
        return str.toLongOrNull() ?: defaultValue
    }

    actual fun putLong(key: String, value: Long) {
        synchronized(lock) { dataMap[key] = value.toString() }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        val str = synchronized(lock) { dataMap[key] } ?: return defaultValue
        return str.split(";;;").filter { it.isNotEmpty() }.toSet()
    }

    actual fun putStringSet(key: String, value: Set<String>) {
        synchronized(lock) { dataMap[key] = value.joinToString(";;;") }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun <T> observeKey(key: String, defaultValue: T): StateFlow<T> {
        synchronized(lock) {
            if (!flows.containsKey(key)) {
                val initialValue: Any? = when (defaultValue) {
                    is Boolean -> getBoolean(key, defaultValue)
                    is Int -> getInt(key, defaultValue)
                    is Long -> getLong(key, defaultValue)
                    is String -> getString(key, defaultValue)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") getStringSet(key, defaultValue as Set<String>)
                    else -> getString(key, defaultValue.toString())
                }
                flows[key] = MutableStateFlow(initialValue)
            }
            @Suppress("UNCHECKED_CAST")
            return (flows[key] as MutableStateFlow<T>).asStateFlow()
        }
    }

    actual fun clear() {
        synchronized(lock) { dataMap.clear() }
        saveToDisk()
    }

    actual fun remove(key: String) {
        synchronized(lock) { dataMap.remove(key) }
        saveToDisk()
        notifyFlow(key, null)
    }
}
