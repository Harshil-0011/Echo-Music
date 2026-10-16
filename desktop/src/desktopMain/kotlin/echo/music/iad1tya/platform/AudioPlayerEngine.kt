package echo.music.iad1tya.platform

import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual class AudioPlayerEngine actual constructor() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var playJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    actual val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    actual val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackInfo?>(null)
    actual val currentTrack: StateFlow<TrackInfo?> = _currentTrack.asStateFlow()

    private var activeLine: SourceDataLine? = null
    @Volatile private var isPausedFlag = false
    @Volatile private var isStoppedFlag = false

    actual fun loadAndPlay(track: TrackInfo, headers: Map<String, String>) {
        stop()
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _positionMs.value = 0L
        _playerState.value = PlayerState.BUFFERING

        val audioUrl = track.audioUrl
        if (audioUrl.isNullOrEmpty()) {
            _playerState.value = PlayerState.ERROR
            return
        }

        isStoppedFlag = false
        isPausedFlag = false

        playJob = scope.launch {
            try {
                if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                    playHttpStream(audioUrl, headers)
                } else {
                    playLocalFile(File(audioUrl))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _playerState.value = PlayerState.ERROR
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
            _playerState.value = PlayerState.PLAYING

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
            _playerState.value = PlayerState.ENDED
            _isPlaying.value = false
        }
    }

    private fun playLocalFile(file: File) {
        if (!file.exists()) {
            _playerState.value = PlayerState.ERROR
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
        _playerState.value = PlayerState.PLAYING

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
            _playerState.value = PlayerState.ENDED
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

    actual fun play() {
        if (isPausedFlag) {
            isPausedFlag = false
            _isPlaying.value = true
            _playerState.value = PlayerState.PLAYING
        }
    }

    actual fun pause() {
        if (_isPlaying.value) {
            isPausedFlag = true
            _isPlaying.value = false
            _playerState.value = PlayerState.PAUSED
        }
    }

    actual fun stop() {
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
        _playerState.value = PlayerState.IDLE
    }

    actual fun seekTo(positionMs: Long) {
        _positionMs.value = positionMs
    }

    actual fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        activeLine?.let { setLineVolume(it, clamped) }
    }

    actual fun release() {
        stop()
    }
}
