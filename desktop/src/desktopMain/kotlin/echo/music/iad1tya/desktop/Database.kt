package echo.music.iad1tya.desktop.db

import echo.music.iad1tya.desktop.models.PlayHistoryEntry
import echo.music.iad1tya.desktop.models.PlaylistEntry
import echo.music.iad1tya.desktop.models.SongModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MusicDatabase(private val dbDir: File) {
    private val lock = Any()
    private val songs = mutableMapOf<String, SongModel>()
    private val history = mutableListOf<PlayHistoryEntry>()
    private val favoriteSet = mutableSetOf<String>()
    private val playlists = mutableMapOf<String, PlaylistEntry>()
    private val playlistSongs = mutableMapOf<String, MutableList<String>>()
    private val settings = mutableMapOf<String, String>()

    private val _songCount = MutableStateFlow(0)
    val songCount: StateFlow<Int> = _songCount.asStateFlow()
    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()
    private val _favorites = MutableStateFlow(emptyList<SongModel>())
    val favorites: StateFlow<List<SongModel>> = _favorites.asStateFlow()

    private val songsFile: File
    private val historyFile: File
    private val favoritesFile: File
    private val playlistsFile: File
    private val settingsFile: File

    init {
        dbDir.mkdirs()
        songsFile = File(dbDir, "songs.json")
        historyFile = File(dbDir, "history.json")
        favoritesFile = File(dbDir, "favorites.json")
        playlistsFile = File(dbDir, "playlists.json")
        settingsFile = File(dbDir, "settings.json")
        loadAll()
    }

    fun init() {
        loadAll()
    }

    fun close() {
        saveAll()
    }

    private fun loadAll() {
        synchronized(lock) {
            try {
                if (songsFile.exists()) {
                    val obj = JSONObject(songsFile.readText())
                    for (key in obj.keys()) {
                        val songObj = obj.getJSONObject(key)
                        songs[key] = SongModel(
                            id = songObj.optString("id", key),
                            title = songObj.optString("title", ""),
                            artist = songObj.optString("artist", ""),
                            album = songObj.optString("album", null),
                            durationMs = songObj.optLong("durationMs", 0L),
                            artworkUrl = songObj.optString("artworkUrl", null),
                            audioUrl = songObj.optString("audioUrl", null)
                        )
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            try {
                if (historyFile.exists()) {
                    val arr = JSONArray(historyFile.readText())
                    // history loading skipped for simplicity
                }
            } catch (e: Exception) { e.printStackTrace() }

            try {
                if (favoritesFile.exists()) {
                    val arr = JSONArray(favoritesFile.readText())
                    for (i in 0 until arr.length()) {
                        favoriteSet.add(arr.getString(i))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        _songCount.value = songs.size
        _favoriteCount.value = favoriteSet.size
        _favorites.value = getFavoritesInternal()
    }

    private fun saveAll() {
        synchronized(lock) {
            try {
                val obj = JSONObject()
                for ((key, song) in songs) {
                    val songObj = JSONObject()
                    songObj.put("id", song.id)
                    songObj.put("title", song.title)
                    songObj.put("artist", song.artist)
                    songObj.put("album", song.album)
                    songObj.put("durationMs", song.durationMs)
                    songObj.put("artworkUrl", song.artworkUrl)
                    songObj.put("audioUrl", song.audioUrl)
                    obj.put(key, songObj)
                }
                songsFile.writeText(obj.toString(2))
            } catch (e: Exception) { e.printStackTrace() }

            try {
                val arr = JSONArray()
                for (id in favoriteSet) {
                    arr.put(id)
                }
                favoritesFile.writeText(arr.toString(2))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveSong(song: SongModel) {
        synchronized(lock) {
            songs[song.id] = song
        }
        _songCount.value = songs.size
        saveAll()
    }

    fun getSong(id: String): SongModel? {
        synchronized(lock) { return songs[id] }
    }

    fun getAllSongs(): List<SongModel> {
        synchronized(lock) { return songs.values.toList() }
    }

    fun deleteSong(id: String) {
        synchronized(lock) {
            songs.remove(id)
            favoriteSet.remove(id)
        }
        _songCount.value = songs.size
        _favoriteCount.value = favoriteSet.size
        _favorites.value = getFavoritesInternal()
        saveAll()
    }

    fun addToHistory(songId: String, playedAt: Long, durationMs: Long) {
        synchronized(lock) {
            history.add(PlayHistoryEntry(songId, playedAt, durationMs))
            if (history.size > 1000) {
                history.removeAt(0)
            }
        }
        saveAll()
    }

    fun getHistory(limit: Int): List<PlayHistoryEntry> {
        synchronized(lock) { return history.reversed().take(limit).toList() }
    }

    fun addFavorite(songId: String) {
        synchronized(lock) { favoriteSet.add(songId) }
        _favoriteCount.value = favoriteSet.size
        _favorites.value = getFavoritesInternal()
        saveAll()
    }

    fun removeFavorite(songId: String) {
        synchronized(lock) { favoriteSet.remove(songId) }
        _favoriteCount.value = favoriteSet.size
        _favorites.value = getFavoritesInternal()
        saveAll()
    }

    fun isFavorite(songId: String): Boolean {
        synchronized(lock) { return favoriteSet.contains(songId) }
    }

    fun getFavorites(): List<SongModel> {
        return getFavoritesInternal()
    }

    private fun getFavoritesInternal(): List<SongModel> {
        synchronized(lock) {
            return favoriteSet
                .mapNotNull { songs[it] }
                .toList()
        }
    }

    fun savePlaylist(name: String, songIds: List<String>): String {
        val id = "playlist_${System.currentTimeMillis()}"
        synchronized(lock) {
            playlists[id] = PlaylistEntry(id, name, songIds.size)
            playlistSongs[id] = songIds.toMutableList()
        }
        saveAll()
        return id
    }

    fun getPlaylists(): List<PlaylistEntry> {
        synchronized(lock) { return playlists.values.toList() }
    }

    fun getPlaylistSongs(playlistId: String): List<SongModel> {
        synchronized(lock) {
            val ids = playlistSongs[playlistId] ?: return emptyList()
            return ids.mapNotNull { songs[it] }
        }
    }

    fun addToPlaylist(playlistId: String, songId: String) {
        synchronized(lock) {
            playlistSongs.getOrPut(playlistId) { mutableListOf() }.add(songId)
            val current = playlists[playlistId]
            if (current != null) {
                playlists[playlistId] = current.copy(songCount = playlistSongs[playlistId]?.size ?: 0)
            }
        }
        saveAll()
    }

    fun deletePlaylist(playlistId: String) {
        synchronized(lock) {
            playlists.remove(playlistId)
            playlistSongs.remove(playlistId)
        }
        saveAll()
    }

    fun putSetting(key: String, value: String) {
        synchronized(lock) { settings[key] = value }
        saveAll()
    }

    fun getSetting(key: String, defaultValue: String = ""): String {
        synchronized(lock) { return settings[key] ?: defaultValue }
    }

    fun removeSetting(key: String) {
        synchronized(lock) { settings.remove(key) }
        saveAll()
    }
}
