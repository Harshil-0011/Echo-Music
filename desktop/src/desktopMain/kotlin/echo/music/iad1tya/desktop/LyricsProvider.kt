package echo.music.iad1tya.desktop.lyrics

import echo.music.iad1tya.desktop.models.LyricsData
import echo.music.iad1tya.desktop.models.LyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Matcher
import java.util.regex.Pattern

class DesktopLyricsProvider {

    suspend fun fetchLyrics(songId: String, title: String, artist: String): LyricsData? = withContext(Dispatchers.IO) {
        val results = mutableListOf<LyricsData>()
        try {
            val r = fetchLrclib(title, artist)
            if (r != null) results.add(r)
        } catch (e: Exception) { e.printStackTrace() }
        try {
            val r = fetchGenius(title, artist)
            if (r != null) results.add(r)
        } catch (e: Exception) { e.printStackTrace() }
        try {
            val r = fetchMusixmatch(title, artist)
            if (r != null) results.add(r)
        } catch (e: Exception) { e.printStackTrace() }
        if (results.isNotEmpty()) {
            results.first().copy(songId = songId, provider = results.first().provider)
        } else {
            null
        }
    }

    private suspend fun fetchLrclib(title: String, artist: String): LyricsData? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val url = URL("https://lrclib.net/api/search?q=$query")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "EchoMusic/1.4.3")
            conn.connect()

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val body = reader.readText()
            reader.close()

            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext null
            val results = arr.getJSONObject(0)

            val syncedLyrics = results.optString("syncedLyrics", null)
            val plainLyrics = results.optString("plainLyrics", null)
            val trackName = results.optString("track_name", title)
            val artistName = results.optString("artist_name", artist)

            val lines = syncedLyrics?.let { parseLrc(it) } ?: parsePlainLyrics(plainLyrics)

            return@withContext LyricsData(
                songId = "$artistName-$trackName",
                title = trackName,
                artist = artistName,
                lines = lines,
                provider = "lrclib",
                synced = syncedLyrics != null
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchGenius(title: String, artist: String): LyricsData? = null
    private suspend fun fetchMusixmatch(title: String, artist: String): LyricsData? = null

    private fun parseLrc(lrc: String): List<LyricsLine> {
        val lines = mutableListOf<LyricsLine>()
        val lrcPattern = Pattern.compile("\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\](.*)")
        lrc.lines().forEach { line ->
            val matcher: Matcher? = lrcPattern.matcher(line)
            if (matcher == null || !matcher.matches()) return@forEach
            val minutes = matcher.group(1).toInt()
            val seconds = matcher.group(2).toInt()
            val millis = matcher.group(3).toIntOrNull() ?: 0
            val text = matcher.group(4)
            val startMs = ((minutes * 60 + seconds) * 1000L) + (millis * 10L)
            lines.add(LyricsLine(startMs = startMs, endMs = startMs + 5000L, text = text))
        }
        for (i in lines.indices) {
            if (i + 1 < lines.size) {
                lines[i] = lines[i].copy(endMs = lines[i + 1].startMs)
            }
        }
        return lines
    }

    private fun parsePlainLyrics(plain: String?): List<LyricsLine> {
        if (plain.isNullOrBlank()) return emptyList()
        return plain.lines()
            .filter { it.isNotBlank() }
            .withIndex()
            .map { (index, text) ->
                LyricsLine(startMs = index.toLong() * 5000L, endMs = (index + 1).toLong() * 5000L, text = text)
            }
    }
}
