package echo.music.iad1tya.desktop.network

import echo.music.iad1tya.desktop.models.SearchResult
import echo.music.iad1tya.desktop.models.SongModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class InnerTubeClient {

    private fun buildSearchJson(query: String): String {
        val obj = JSONObject()
        obj.put("query", query)
        val context = JSONObject()
        val client = JSONObject()
        client.put("clientName", "ANDROID_MUSIC")
        client.put("clientVersion", "6.31")
        client.put("platform", "ANDROID")
        context.put("client", client)
        obj.put("context", context)
        return obj.toString()
    }

    private fun buildBrowseJson(browseId: String): String {
        val obj = JSONObject()
        obj.put("browseId", browseId)
        val context = JSONObject()
        val client = JSONObject()
        client.put("clientName", "ANDROID_MUSIC")
        client.put("clientVersion", "6.31")
        client.put("platform", "ANDROID")
        context.put("client", client)
        obj.put("context", context)
        return obj.toString()
    }

    private fun buildPlayerJson(videoId: String): String {
        val obj = JSONObject()
        obj.put("videoId", videoId)
        val context = JSONObject()
        val client = JSONObject()
        client.put("clientName", "ANDROID_MUSIC")
        client.put("clientVersion", "6.31")
        client.put("platform", "ANDROID")
        context.put("client", client)
        obj.put("context", context)
        val playbackContext = JSONObject()
        val contentPlaybackContext = JSONObject()
        contentPlaybackContext.put("signatureTimestamp", 0)
        playbackContext.put("contentPlaybackContext", contentPlaybackContext)
        obj.put("playbackContext", playbackContext)
        return obj.toString()
    }

    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        try {
            val response = postJson("https://music.youtube.com/youtubei/v1/search", buildSearchJson(query))
            parseSearchResponse(response, query)
        } catch (e: Exception) {
            e.printStackTrace()
            SearchResult(query = query, songs = emptyList())
        }
    }

    suspend fun getSongDetails(videoId: String): SongModel? = withContext(Dispatchers.IO) {
        try {
            val response = postJson("https://music.youtube.com/youtubei/v1/browse", buildBrowseJson("VL$videoId"))
            parseSongDetails(response, videoId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = postJson("https://www.youtube.com/youtubei/v1/player", buildPlayerJson(videoId))
            extractStreamUrl(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTrending(): List<SongModel> = withContext(Dispatchers.IO) {
        try {
            val response = postJson("https://music.youtube.com/youtubei/v1/browse", buildBrowseJson("FEmusic_trending"))
            parseTrending(response)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun postJson(urlString: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", "Music/6.31 (Linux; Android 13)")
        conn.doOutput = true
        conn.connect()

        conn.outputStream.use { os ->
            os.write(bodyJson.toByteArray())
            os.flush()
        }

        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            reader.readText()
        }
    }

    private fun parseSearchResponse(body: String, query: String): SearchResult {
        val songs = mutableListOf<SongModel>()
        try {
            val obj = JSONObject(body)
            val contents = obj.optJSONObject("contents") ?: return SearchResult(query, songs)
            val twoColumn = contents.optJSONObject("twoColumnSearchResultsRenderer") ?: return SearchResult(query, songs)
            val tabs = twoColumn.optJSONArray("contents") ?: return SearchResult(query, songs)

            for (i in 0 until tabs.length()) {
                val tab = tabs.getJSONObject(i)
                val tabRenderer = tab.optJSONObject("tabRenderer") ?: continue
                val content = tabRenderer.optJSONObject("content") ?: continue
                val sectionList = content.optJSONObject("sectionListRenderer") ?: continue
                val contentsArray = sectionList.optJSONArray("contents") ?: continue

                for (j in 0 until contentsArray.length()) {
                    val section = contentsArray.getJSONObject(j)
                    val musicSection = section.optJSONObject("musicSectionRenderer") ?: continue
                    val items = musicSection.optJSONArray("contents") ?: continue

                    for (k in 0 until items.length()) {
                        val item = items.getJSONObject(k)
                        val songRenderer = item.optJSONObject("songRenderer") ?: continue
                        val videoId = songRenderer.optString("videoId", "") ?: continue
                        if (videoId.isEmpty()) continue
                        val title = songRenderer.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.getJSONObject(0)
                            ?.optString("text", "") ?: continue
                        val artist = songRenderer.optJSONObject("artist")
                            ?.optJSONArray("runs")
                            ?.getJSONObject(0)
                            ?.optString("text", "Unknown Artist") ?: "Unknown Artist"
                        val durationMs = songRenderer.optLong("durationSeconds", 0L) * 1000
                        val thumbnail = songRenderer.optJSONObject("thumbnails")
                            ?.optJSONArray("thumbnails")
                            ?.getJSONObject(0)
                            ?.optString("url", "")

                        songs.add(SongModel(
                            id = videoId,
                            title = title,
                            artist = artist,
                            durationMs = durationMs
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return SearchResult(query = query, songs = songs)
    }

    private fun parseSongDetails(body: String, videoId: String): SongModel? {
        try {
            val obj = JSONObject(body)
            val contents = obj.optJSONObject("contents") ?: return null
            val singleColumn = contents.optJSONObject("singleColumnBrowseResultsRenderer") ?: return null
            val tabs = singleColumn.optJSONArray("tabs") ?: return null
            if (tabs.length() == 0) return null
            val tab = tabs.getJSONObject(0)
            val tabRenderer = tab.optJSONObject("tabRenderer") ?: return null
            val content = tabRenderer.optJSONObject("content") ?: return null
            val sectionList = content.optJSONObject("sectionListRenderer") ?: return null
            val contentsArray = sectionList.optJSONArray("contents") ?: return null

            for (i in 0 until contents.length()) {
                val section = contentsArray.getJSONObject(i)
                val musicRow = section.optJSONObject("musicRowRenderer") ?: continue
                val items = musicRow.optJSONArray("items") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.getJSONObject(j)
                    val listItem = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val flexColumns = listItem.optJSONArray("flexColumns") ?: continue
                    if (flexColumns.length() == 0) continue
                    val flexCol = flexColumns.getJSONObject(0)
                    val flexColRenderer = flexCol.optJSONObject("musicResponsiveListItemFlexColumnRenderer") ?: continue
                    val text = flexColRenderer.optJSONObject("text") ?: continue
                    val runs = text.optJSONArray("runs") ?: continue
                    if (runs.length() == 0) continue
                    val songTitle = runs.getJSONObject(0).optString("text", "")

                    return SongModel(id = videoId, title = songTitle, artist = "Unknown Artist")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun extractStreamUrl(body: String): String? {
        try {
            val obj = JSONObject(body)
            val streamingData = obj.optJSONObject("streamingData") ?: return null

            val formats = streamingData.optJSONArray("formats")
            if (formats != null) {
                for (i in 0 until formats.length()) {
                    val format = formats.getJSONObject(i)
                    val mimeType = format.optString("mimeType", "")
                    if (mimeType.contains("audio")) {
                        return format.optString("url", "")
                    }
                }
            }

            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.getJSONObject(i)
                    val mimeType = format.optString("mimeType", "")
                    if (mimeType.contains("audio")) {
                        return format.optString("url", "")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseTrending(body: String): List<SongModel> {
        return parseSearchResponse(body, "trending").songs
    }
}
