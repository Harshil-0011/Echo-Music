package echo.music.iad1tya.desktop.models

data class SongModel(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0L,
    val artworkUrl: String? = null,
    val audioUrl: String? = null,
    val viewCount: Long = 0,
    val isExplicit: Boolean = false,
    val source: String = "youtube"
)

data class AlbumModel(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val songCount: Int = 0,
    val year: Int? = null
)

data class ArtistModel(
    val id: String,
    val name: String,
    val artworkUrl: String? = null,
    val subscriberCount: Long = 0,
    val songCount: Int = 0
)

data class PlaylistModel(
    val id: String,
    val title: String,
    val artworkUrl: String? = null,
    val songCount: Int = 0,
    val owner: String? = null,
    val isPublic: Boolean = true
)

data class SearchResult(
    val query: String,
    val songs: List<SongModel> = emptyList(),
    val albums: List<AlbumModel> = emptyList(),
    val artists: List<ArtistModel> = emptyList(),
    val playlists: List<PlaylistModel> = emptyList()
)

enum class PlaybackState {
    IDLE, BUFFERING, PLAYING, PAUSED, ENDED, ERROR
}

data class LyricsLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val translation: String? = null
)

data class LyricsData(
    val songId: String,
    val title: String,
    val artist: String,
    val lines: List<LyricsLine> = emptyList(),
    val provider: String = "unknown",
    val synced: Boolean = false
)

data class PlayHistoryEntry(
    val songId: String,
    val playedAt: Long,
    val durationMs: Long
)

data class PlaylistEntry(
    val id: String,
    val name: String,
    val songCount: Int
)
