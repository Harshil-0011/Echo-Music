package echo.music.iad1tya.desktop.playback

import echo.music.iad1tya.desktop.network.InnerTubeClient
import echo.music.iad1tya.desktop.models.PlaybackState
import echo.music.iad1tya.desktop.models.SongModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

class DesktopPlaybackEngine(
    private val innerTube: InnerTubeClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var playJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _playerState = MutableStateFlow(PlaybackState.IDLE)
    val playerState: StateFlow<PlaybackState> = _playerState.asStateFlow()

    private val _currentTrack = MutableStateFlow<SongModel?>(null)
    val currentTrack: StateFlow<SongModel?> = _currentTrack.asStateFlow()

    private var activeLine: SourceDataLine? = null
    @Volatile private var isPausedFlag = false
    @Volatile private var isStoppedFlag = false

    suspend fun loadAndPlay(song: SongModel) {
        stop()
        _currentTrack.value = song
        _durationMs.value = song.durationMs
        _positionMs.value = 0L
        _playerState.value = PlaybackState.BUFFERING

        val audioUrl = song.audioUrl
        if (audioUrl.isNullOrEmpty()) {
            val streamUrl = innerTube.getStreamUrl(song.id)
            if (streamUrl != null) {
                playHttpStream(streamUrl, emptyMap())
            } else {
                _playerState.value = PlaybackState.ERROR
                _isPlaying.value = false
            }
            return
        }

        isStoppedFlag = false
        isPausedFlag = false

        playJob = scope.launch {
            try {
                if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                    playHttpStream(audioUrl, emptyMap())
                } else {
                    playLocalFile(File(audioUrl))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _playerState.value = PlaybackState.ERROR
                _isPlaying.value = false
            }
        }
    }

    private fun playHttpStream(urlString: String, headers: Map<String, String>) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.connect()

        val inputStream = BufferedInputStream(conn.inputStream)
        val audioStream = try {
            AudioSystem.getAudioInputStream(inputStream)
        } catch (e: Exception) {
            inputStream
        }

        if (audioStream is javax.sound.sampled.AudioInputStream) {
            val format = audioStream.format
            val info = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as SourceDataLine
            activeLine = line
            line.open(format)
            line.start()

            setLineVolume(line, _volume.value)

            _isPlaying.value = true
            _playerState.value = PlaybackState.PLAYING

            val buffer = ByteArray(4096)
            var totalBytesRead = 0L
            val frameSize = format.frameSize.coerceAtLeast(1)
            val frameRate = format.frameRate.coerceAtLeast(1.0f)

            while (!isStoppedFlag) {
                val bytesRead = audioStream.read(buffer)
                if (bytesRead == -1) break

                while (isPausedFlag && !isStoppedFlag) {
                    Thread.sleep(50)
                }
                if (isStoppedFlag) break

                line.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val currentFrame = totalBytesRead / frameSize
                val currentSec = (currentFrame / frameRate).toLong()
                _positionMs.value = currentSec * 1000L
            }

            line.drain()
            line.close()
            activeLine = null
        }

        if (!isStoppedFlag) {
            _playerState.value = PlaybackState.ENDED
            _isPlaying.value = false
        }
    }

    private fun playLocalFile(file: File) {
        if (!file.exists()) {
            _playerState.value = PlaybackState.ERROR
            return
        }
        val audioStream = AudioSystem.getAudioInputStream(file)
        val format = audioStream.format
        val info = javax.sound.sampled.DataLine.Info(SourceDataLine::class.java, format)
        val line = AudioSystem.getLine(info) as SourceDataLine
        activeLine = line
        line.open(format)
        line.start()

        setLineVolume(line, _volume.value)

        _isPlaying.value = true
        _playerState.value = PlaybackState.PLAYING

        val buffer = ByteArray(4096)
        var totalBytesRead = 0L
        val frameSize = format.frameSize.coerceAtLeast(1)
        val frameRate = format.frameRate.coerceAtLeast(1.0f)

        while (!isStoppedFlag) {
            val bytesRead = audioStream.read(buffer)
            if (bytesRead == -1) break

            while (isPausedFlag && !isStoppedFlag) {
                Thread.sleep(50)
            }
            if (isStoppedFlag) break

            line.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead

            val currentFrame = totalBytesRead / frameSize
            val currentSec = (currentFrame / frameRate).toLong()
            _positionMs.value = currentSec * 1000L
        }

        line.drain()
        line.close()
        activeLine = null

        if (!isStoppedFlag) {
            _playerState.value = PlaybackState.ENDED
            _isPlaying.value = false
        }
    }

    private fun setLineVolume(line: SourceDataLine, vol: Float) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val clampedVol = vol.coerceIn(0.0f, 1.0f)
                val dB = if (clampedVol > 0.0f) {
                    (20.0 * Math.log10(clampedVol.toDouble())).toFloat()
                } else {
                    gainControl.minimum
                }
                gainControl.value = dB.coerceIn(gainControl.minimum, gainControl.maximum)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (isPausedFlag) {
            isPausedFlag = false
            _isPlaying.value = true
            _playerState.value = PlaybackState.PLAYING
        }
    }

    fun pause() {
        if (_isPlaying.value) {
            isPausedFlag = true
            _isPlaying.value = false
            _playerState.value = PlaybackState.PAUSED
        }
    }

    fun stop() {
        isStoppedFlag = true
        isPausedFlag = false
        playJob?.cancel()
        playJob = null
        try {
            activeLine?.stop()
            activeLine?.close()
        } catch (_: Exception) {}
        activeLine = null
        _isPlaying.value = false
        _playerState.value = PlaybackState.IDLE
    }

    fun seekTo(positionMs: Long) {
        _positionMs.value = positionMs
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        activeLine?.let { setLineVolume(it, clamped) }
    }

    fun release() {
        stop()
    }
}
