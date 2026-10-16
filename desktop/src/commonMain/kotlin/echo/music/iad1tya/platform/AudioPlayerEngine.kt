package echo.music.iad1tya.platform

import kotlinx.coroutines.flow.StateFlow

enum class PlayerState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0L,
    val artworkUrl: String? = null,
    val audioUrl: String? = null
)

expect class AudioPlayerEngine() {
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val volume: StateFlow<Float>
    val playerState: StateFlow<PlayerState>
    val currentTrack: StateFlow<TrackInfo?>

    fun loadAndPlay(track: TrackInfo, headers: Map<String, String> = emptyMap())
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
}
