package echo.music.iad1tya.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.desktop.models.*
import echo.music.iad1tya.desktop.db.MusicDatabase
import echo.music.iad1tya.desktop.network.InnerTubeClient
import echo.music.iad1tya.desktop.lyrics.DesktopLyricsProvider
import echo.music.iad1tya.desktop.playback.DesktopPlaybackEngine
import echo.music.iad1tya.platform.LocalPreferences
import echo.music.iad1tya.platform.SystemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// Shared UI constants
import echo.music.iad1tya.ui.*

@Composable
fun DesktopEchoMusicTheme(
    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    seedColor: Long = 0xFFED5564,
    content: @Composable () -> Unit
) {
    val surface = if (pureBlack && darkTheme) Color.Black else VoidSlateDark
    val surfaceContainer = if (pureBlack && darkTheme) Color(0xFF121212) else VoidSlateSurface
    val surfaceCard = if (pureBlack && darkTheme) Color(0xFF1A1A1A) else VoidSlateCard

    val colorScheme = darkColorScheme(
        primary = ElectricCyan,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF00363D),
        onPrimaryContainer = ElectricCyan,
        secondary = AccentMagenta,
        onSecondary = Color.White,
        tertiary = AccentPurple,
        onTertiary = Color.White,
        surface = surface,
        onSurface = Color.White,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceCard,
        onSurfaceVariant = TextSecondary,
        background = surface,
        onBackground = Color.White,
        error = Color(0xFFFF5252),
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White),
            displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White),
            displaySmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White),
            headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White),
            headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White),
            headlineSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White),
            titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White),
            titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
            bodyLarge = TextStyle(fontSize = 16.sp, color = Color.White),
            bodyMedium = TextStyle(fontSize = 14.sp, color = Color.White),
            bodySmall = TextStyle(fontSize = 12.sp, color = TextSecondary),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary),
            labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary),
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(
    playerEngine: DesktopPlaybackEngine,
    database: MusicDatabase,
    preferences: LocalPreferences,
    innerTube: InnerTubeClient,
    lyricsProvider: DesktopLyricsProvider
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("home") }
    var searchQuery by remember { mutableStateOf("") }
    var isLyricsExpanded by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val darkMode by preferences.observeKey("dark_mode", true).collectAsState(initial = true)
    val pureBlack by preferences.observeKey("pure_black", false).collectAsState(initial = false)
    val seedColorStr by preferences.observeKey("theme_seed_color", "FFED5564").collectAsState(initial = "FFED5564")
    val liquidGlass by preferences.observeKey("liquid_glass", true).collectAsState(initial = true)

    val isPlaying by playerEngine.isPlaying.collectAsState()
    val positionMs by playerEngine.positionMs.collectAsState()
    val durationMs by playerEngine.durationMs.collectAsState()
    val volume by playerEngine.volume.collectAsState()
    val playerState by playerEngine.playerState.collectAsState()
    val currentTrack by playerEngine.currentTrack.collectAsState()

    val songCount by database.songCount.collectAsState()
    val favoriteCount by database.favoriteCount.collectAsState()

    val searchFocus = remember { FocusRequester() }

    DesktopEchoMusicTheme(
        darkTheme = darkMode,
        pureBlack = pureBlack,
        seedColor = seedColorStr.toLong(16)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Spacebar -> { if (isPlaying) playerEngine.pause() else playerEngine.play(); true }
                            Key.DirectionLeft -> { playerEngine.seekTo((positionMs - 5000).coerceAtLeast(0)); true }
                            Key.DirectionRight -> { playerEngine.seekTo((positionMs + 5000).coerceAtMost(durationMs)); true }
                            Key.L -> { isLyricsExpanded = !isLyricsExpanded; true }
                            Key.O -> { showFilePicker = true; true }
                            Key.Escape -> { if (isLyricsExpanded) isLyricsExpanded = false else showFilePicker = false; true }
                            else -> false
                        }
                    } else false
                },
            color = if (pureBlack && darkMode) Color.Black else VoidSlateDark
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 900.dp

                // Top Bar
                EchoTopBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onOpenFile = { showFilePicker = true },
                    onSearchFocus = { searchFocus.requestFocus() },
                    onSettingsClick = { showSettings = true },
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    onDarkModeToggle = { preferences.putBoolean("dark_mode", !darkMode) },
                    onPureBlackToggle = { preferences.putBoolean("pure_black", !pureBlack) }
                )

                // Main Content Area
                Row(Modifier.fillMaxSize()) {
                    if (wide) {
                        NavigationRailPanel(
                            selectedTab = selectedTab,
                            onSelect = { selectedTab = it },
                            modifier = Modifier.width(220.dp).fillMaxHeight()
                        )
                    }

                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        val content = when (selectedTab) {
                            "home" -> PlaceholderScreen("Home", "Home screen content")
                            "search" -> PlaceholderScreen("Search", "Search screen content")
                            "library" -> PlaceholderScreen("Library", "Library screen content")
                            "charts" -> PlaceholderScreen("Charts", "Charts screen content")
                            "mood" -> PlaceholderScreen("Moods", "Moods screen content")
                            "new_release" -> PlaceholderScreen("New Releases", "New releases screen content")
                            "generate" -> PlaceholderScreen("Generate", "Generate screen content")
                            "recognition" -> PlaceholderScreen("Echo Find", "Recognition screen content")
                            "equalizer" -> PlaceholderScreen("Equalizer", "Equalizer screen content")
                            "queue" -> PlaceholderScreen("Queue", "Queue screen content")
                            "together" -> PlaceholderScreen("Together", "Together screen content")
                            "settings" -> PlaceholderScreen("Settings", "Settings screen content")
                            else -> PlaceholderScreen("Unknown", "Unknown screen")
                        }
                        content
                    }
                }

                if (!showSettings) {
                    PlayerBar(
                        playerEngine = playerEngine,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        volume = volume,
                        onLyricsToggle = { isLyricsExpanded = !isLyricsExpanded },
                        lyricsActive = isLyricsExpanded,
                        onOpenFile = { showFilePicker = true },
                        onNext = { /* next */ },
                        onPrevious = { playerEngine.seekTo(0) }
                    )
                }

                if (!wide && !showSettings) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        CompactBottomNav(selectedTab) { tab -> selectedTab = tab }
                    }
                }

                if (showSettings) {
                    PlaceholderScreen("Settings", "Settings dialog")
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ElectricCyan))
            Text(message, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
        }
    }
}

// ============================================================================
// TOP BAR
// ============================================================================

@Composable
fun EchoTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenFile: () -> Unit,
    onSearchFocus: () -> Unit,
    onSettingsClick: () -> Unit,
    darkMode: Boolean,
    pureBlack: Boolean,
    onDarkModeToggle: () -> Unit,
    onPureBlackToggle: () -> Unit
) {
    Surface(
        color = if (pureBlack) Color.Black else VoidSlateSurface,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Echo Music",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = ElectricCyan
                    )
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search songs, artists, albums...", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VoidSlateCard,
                    unfocusedContainerColor = VoidSlateCard,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier.width(420.dp).height(48.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenFile) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = "Open local file", tint = ElectricCyan)
                }
                IconButton(onClick = onSearchFocus) {
                    Icon(Icons.Filled.Search, contentDescription = "Focus search", tint = TextSecondary)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        }
    }
}

// ============================================================================
// NAVIGATION RAIL
// ============================================================================

@Composable
fun NavigationRailPanel(selectedTab: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val items = listOf(
                "home" to ("Home" to Icons.Filled.Home),
                "search" to ("Search" to Icons.Filled.Search),
                "library" to ("Library" to Icons.Filled.LibraryMusic),
                "charts" to ("Charts" to Icons.Filled.TrendingUp),
                "mood" to ("Moods" to Icons.Filled.Mood),
                "new_release" to ("New Releases" to Icons.Filled.NewReleases),
                "generate" to ("Generate" to Icons.Filled.AutoAwesome),
                "recognition" to ("Echo Find" to Icons.Filled.Mic),
                "equalizer" to ("Equalizer" to Icons.Filled.Equalizer),
                "queue" to ("Queue" to Icons.Filled.QueueMusic),
                "together" to ("Together" to Icons.Filled.Group),
                "settings" to ("Settings" to Icons.Filled.Settings)
            )
            items.forEach { (key, pair) ->
                val (label, icon) = pair
                NavigationRailItem(
                    selected = selectedTab == key,
                    onClick = { onSelect(key) },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ElectricCyan.copy(alpha = 0.15f)
                    )
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "v1.4.3",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// ============================================================================
// COMPACT BOTTOM NAV (Narrow windows)
// ============================================================================

@Composable
fun CompactBottomNav(selectedTab: String, onSelect: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                "home" to Icons.Filled.Home,
                "search" to Icons.Filled.Search,
                "library" to Icons.Filled.LibraryMusic,
                "charts" to Icons.Filled.TrendingUp,
                "more" to Icons.Filled.MoreHoriz
            )
            items.forEach { (key, icon) ->
                IconButton(onClick = { onSelect(key) }) {
                    Icon(
                        icon,
                        contentDescription = key,
                        tint = if (selectedTab == key) ElectricCyan else TextSecondary
                    )
                }
            }
        }
    }
}

// ============================================================================
// PLAYER BAR
// ============================================================================

@Composable
fun PlayerBar(
    playerEngine: DesktopPlaybackEngine,
    currentTrack: SongModel?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    onLyricsToggle: () -> Unit,
    lyricsActive: Boolean,
    onOpenFile: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Info
            if (currentTrack != null) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentTrack.title.take(1).uppercase(), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(currentTrack.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentTrack.artist, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                Text("No track playing", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary), modifier = Modifier.weight(1f))
            }

            // Progress
            Column(
                modifier = Modifier.weight(2f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    value = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()) else 0f,
                    onValueChange = { playerEngine.seekTo((it * durationMs.toFloat()).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = durationMs > 0,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(positionMs), style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                }
            }

            // Controls
            Row(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = TextSecondary, modifier = Modifier.size(24.dp))
                }
                FilledIconButton(
                    onClick = { if (isPlaying) playerEngine.pause() else playerEngine.play() },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { onNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextSecondary, modifier = Modifier.size(24.dp))
                }
            }

            // Volume & Extras
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = volume,
                    onValueChange = { playerEngine.setVolume(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                )
                IconButton(onClick = { /* lyrics */ }) {
                    Icon(
                        if (lyricsActive) Icons.Filled.Lyrics else Icons.Filled.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (lyricsActive) ElectricCyan else TextSecondary
                    )
                }
                IconButton(onClick = { /* open file */ }) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = "Open file", tint = TextSecondary)
                }
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