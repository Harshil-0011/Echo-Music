package echo.music.iad1tya.desktop

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.platform.AudioPlayerEngine
import echo.music.iad1tya.platform.LocalPreferences
import echo.music.iad1tya.platform.PlayerState
import echo.music.iad1tya.platform.SystemUtils
import echo.music.iad1tya.platform.TrackInfo

val VoidSlateDark = Color(0xFF0B0F17)
val VoidSlateSurface = Color(0xFF131924)
val VoidSlateCard = Color(0xFF1B2232)
val ElectricCyan = Color(0xFF00F0FF)

@Composable
fun DesktopEchoMusicTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = ElectricCyan,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF00363D),
        onPrimaryContainer = ElectricCyan,
        surface = VoidSlateDark,
        onSurface = Color.White,
        surfaceContainer = VoidSlateSurface,
        surfaceContainerHigh = VoidSlateCard,
        onSurfaceVariant = Color(0xFF94A3B8),
        background = VoidSlateDark,
        onBackground = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(
    playerEngine: AudioPlayerEngine,
    preferences: LocalPreferences
) {
    var selectedTab by remember { mutableStateOf("home") }
    var searchQuery by remember { mutableStateOf("") }
    var isLyricsExpanded by remember { mutableStateOf(false) }

    val isPlaying by playerEngine.isPlaying.collectAsState()
    val positionMs by playerEngine.positionMs.collectAsState()
    val durationMs by playerEngine.durationMs.collectAsState()
    val volume by playerEngine.volume.collectAsState()
    val playerState by playerEngine.playerState.collectAsState()
    val currentTrack by playerEngine.currentTrack.collectAsState()

    DesktopEchoMusicTheme {
        Scaffold(
            containerColor = VoidSlateDark,
            topBar = {
                Surface(
                    color = VoidSlateSurface,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Echo Music",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = ElectricCyan
                                )
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search songs, artists, albums...", color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = VoidSlateCard,
                                unfocusedContainerColor = VoidSlateCard,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.width(420.dp).height(48.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { SystemUtils.showToast("Echo Find / Recognition feature ready") },
                                colors = ButtonDefaults.buttonColors(containerColor = VoidSlateCard),
                                shape = CircleShape
                            ) {
                                Text("Echo Find", color = ElectricCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = VoidSlateSurface,
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clickable {
                                playerEngine.seekTo((progress * durationMs).toLong())
                            },
                            color = ElectricCyan,
                            trackColor = VoidSlateCard
                        )

                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(VoidSlateCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("♪", color = ElectricCyan, fontSize = 24.sp)
                                }
                                Column {
                                    Text(
                                        text = currentTrack?.title ?: "No Track Playing",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    Text(
                                        text = currentTrack?.artist ?: "Echo Music Desktop",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { if (isPlaying) playerEngine.pause() else playerEngine.play() },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                ) {
                                    Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { isLyricsExpanded = !isLyricsExpanded },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isLyricsExpanded) ElectricCyan else VoidSlateCard)
                                ) {
                                    Text("LYRICS", color = if (isLyricsExpanded) Color.Black else Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                NavigationRail(
                    containerColor = VoidSlateSurface,
                    contentColor = Color.White,
                    modifier = Modifier.width(180.dp).fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == "home",
                            onClick = { selectedTab = "home" },
                            icon = { Text("🏠", fontSize = 18.sp) },
                            label = { Text("Home", color = if (selectedTab == "home") ElectricCyan else Color.Gray) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == "library",
                            onClick = { selectedTab = "library" },
                            icon = { Text("📚", fontSize = 18.sp) },
                            label = { Text("Library", color = if (selectedTab == "library") ElectricCyan else Color.Gray) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == "search",
                            onClick = { selectedTab = "search" },
                            icon = { Text("🔍", fontSize = 18.sp) },
                            label = { Text("Search", color = if (selectedTab == "search") ElectricCyan else Color.Gray) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == "together",
                            onClick = { selectedTab = "together" },
                            icon = { Text("👥", fontSize = 18.sp) },
                            label = { Text("Listen Together", color = if (selectedTab == "together") ElectricCyan else Color.Gray) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == "settings",
                            onClick = { selectedTab = "settings" },
                            icon = { Text("⚙️", fontSize = 18.sp) },
                            label = { Text("Settings", color = if (selectedTab == "settings") ElectricCyan else Color.Gray) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                    when (selectedTab) {
                        "home" -> HomeScreenStage(playerEngine)
                        "library" -> LibraryScreenStage()
                        "search" -> SearchScreenStage(searchQuery, playerEngine)
                        "together" -> ListenTogetherStage()
                        "settings" -> SettingsScreenStage(preferences)
                    }

                    if (isLyricsExpanded) {
                        LyricsOverlay(currentTrack, positionMs, onDismiss = { isLyricsExpanded = false })
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenStage(playerEngine: AudioPlayerEngine) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Quick Picks & Recommendations", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SampleTrackCard("Echoes in the Void", "Synthwave Master", "demo_1") {
                playerEngine.loadAndPlay(TrackInfo("demo_1", "Echoes in the Void", "Synthwave Master", durationMs = 210000L))
            }
            SampleTrackCard("Neon Cyberpunk", "Electric Pulse", "demo_2") {
                playerEngine.loadAndPlay(TrackInfo("demo_2", "Neon Cyberpunk", "Electric Pulse", durationMs = 180000L))
            }
        }
    }
}

@Composable
fun SampleTrackCard(title: String, artist: String, id: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidSlateCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(220.dp).height(160.dp).clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(ElectricCyan.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text("▶", color = ElectricCyan, fontSize = 20.sp)
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Text(artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun LibraryScreenStage() {
    Column {
        Text("Your Library", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Playlists, Liked Songs, Downloads & Custom Configurations", color = Color.Gray)
    }
}

@Composable
fun SearchScreenStage(query: String, playerEngine: AudioPlayerEngine) {
    Column {
        Text("Search Results for: ${query.ifEmpty { "All Tracks" }}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Stream YouTube tracks & music catalog ad-free.", color = Color.Gray)
    }
}

@Composable
fun ListenTogetherStage() {
    Column {
        Text("Listen Together (Synced Group Listening)", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Host or join synced listening sessions in real-time.", color = Color.Gray)
    }
}

@Composable
fun SettingsScreenStage(preferences: LocalPreferences) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Audio Customization & Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        Text("Audio Quality: High (Opus / FLAC)", color = ElectricCyan)
        Text("UI Density Scaling: Desktop Adaptive", color = Color.LightGray)
        Text("Music DNA & Echo Find: Active", color = Color.LightGray)
    }
}

@Composable
fun LyricsOverlay(track: TrackInfo?, positionMs: Long, onDismiss: () -> Unit) {
    Surface(
        color = VoidSlateDark.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Synchronized Lyrics", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = ElectricCyan))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = VoidSlateCard)) {
                    Text("Close", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track?.title ?: "No Song Selected",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "♪ Synchronized lyrics streaming in real-time ♪",
                    style = MaterialTheme.typography.headlineSmall.copy(color = ElectricCyan)
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
