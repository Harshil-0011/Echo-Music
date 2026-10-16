package echo.music.iad1tya.platform

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual class LocalPreferences actual constructor() {
    private val prefFile: File
    private val dataMap = mutableMapOf<String, String>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        val appData = System.getenv("APPDATA")
        val dir = if (!appData.isNullOrEmpty()) {
            File(appData, "EchoMusic")
        } else {
            File(System.getProperty("user.home"), ".echomusic")
        }
        dir.mkdirs()
        prefFile = File(dir, "preferences.json")
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!prefFile.exists()) return
        try {
            val content = prefFile.readText()
            val loaded: Map<String, String> = json.decodeFromString(content)
            synchronized(dataMap) {
                dataMap.clear()
                dataMap.putAll(loaded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToDisk() {
        try {
            val snapshot = synchronized(dataMap) { dataMap.toMap() }
            val content = json.encodeToString(snapshot)
            prefFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun <T> notifyFlow(key: String, value: T) {
        synchronized(flows) {
            @Suppress("UNCHECKED_CAST")
            (flows[key] as? MutableStateFlow<T>)?.value = value
        }
    }

    actual fun getString(key: String, defaultValue: String): String {
        return synchronized(dataMap) { dataMap[key] } ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        synchronized(dataMap) { dataMap[key] = value }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val str = synchronized(dataMap) { dataMap[key] } ?: return defaultValue
        return str.toBooleanStrictOrNull() ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        synchronized(dataMap) { dataMap[key] = value.toString() }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        val str = synchronized(dataMap) { dataMap[key] } ?: return defaultValue
        return str.toIntOrNull() ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        synchronized(dataMap) { dataMap[key] = value.toString() }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        val str = synchronized(dataMap) { dataMap[key] } ?: return defaultValue
        return str.toLongOrNull() ?: defaultValue
    }

    actual fun putLong(key: String, value: Long) {
        synchronized(dataMap) { dataMap[key] = value.toString() }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        val str = synchronized(dataMap) { dataMap[key] } ?: return defaultValue
        return str.split(";;;").filter { it.isNotEmpty() }.toSet()
    }

    actual fun putStringSet(key: String, value: Set<String>) {
        synchronized(dataMap) { dataMap[key] = value.joinToString(";;;") }
        saveToDisk()
        notifyFlow(key, value)
    }

    actual fun <T> observeKey(key: String, defaultValue: T): StateFlow<T> {
        synchronized(flows) {
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
}
