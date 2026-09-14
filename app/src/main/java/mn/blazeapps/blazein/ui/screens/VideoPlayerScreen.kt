package mn.blazeapps.blazein.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import mn.blazeapps.blazein.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import mn.blazeapps.blazein.data.TelegramDataSourceFactory
import mn.blazeapps.blazein.data.TelegramRepository
import mn.blazeapps.blazein.data.model.VideoItem
import java.io.File
import java.util.regex.Pattern

private const val TAG = "VideoPlayerScreen"

// ── Subtitle data model & parser ──────────────────────────────────────────────

private data class SubtitleEntry(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/** Parse an SRT file into a list of subtitle entries */
private fun parseSrt(content: String): List<SubtitleEntry> {
    val entries = mutableListOf<SubtitleEntry>()
    // Match SRT timestamp pattern: 00:01:23,456 --> 00:02:34,567
    val timePattern = Pattern.compile(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})"""
    )

    val blocks = content.replace("\r\n", "\n").replace("\r", "\n")
        .split(Regex("\n\\s*\n"))

    for (block in blocks) {
        val lines = block.trim().lines()
        if (lines.size < 2) continue

        // Find the line with the timestamp
        var timeLineIdx = -1
        for (i in lines.indices) {
            if (timePattern.matcher(lines[i].trim()).find()) {
                timeLineIdx = i
                break
            }
        }
        if (timeLineIdx < 0 || timeLineIdx + 1 >= lines.size) continue

        val matcher = timePattern.matcher(lines[timeLineIdx].trim())
        if (!matcher.find()) continue

        val startMs = matcher.group(1)!!.toLong() * 3600000 +
                matcher.group(2)!!.toLong() * 60000 +
                matcher.group(3)!!.toLong() * 1000 +
                matcher.group(4)!!.toLong()
        val endMs = matcher.group(5)!!.toLong() * 3600000 +
                matcher.group(6)!!.toLong() * 60000 +
                matcher.group(7)!!.toLong() * 1000 +
                matcher.group(8)!!.toLong()

        // All lines after the timestamp are subtitle text
        val text = lines.subList(timeLineIdx + 1, lines.size)
            .joinToString("\n")
            .replace(Regex("<[^>]+>"), "") // strip HTML tags like <i>, <b>
            .trim()

        if (text.isNotBlank()) {
            entries.add(SubtitleEntry(startMs, endMs, text))
        }
    }
    return entries.sortedBy { it.startMs }
}

/** Parse a WebVTT file into a list of subtitle entries */
private fun parseVtt(content: String): List<SubtitleEntry> {
    val entries = mutableListOf<SubtitleEntry>()
    val timePattern = Pattern.compile(
        """(\d{1,2}):(\d{2}):(\d{2})[.](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[.](\d{3})"""
    )
    // Also handle MM:SS.mmm format
    val shortTimePattern = Pattern.compile(
        """(\d{1,2}):(\d{2})[.](\d{3})\s*-->\s*(\d{1,2}):(\d{2})[.](\d{3})"""
    )

    val cleanContent = content.replace("\r\n", "\n").replace("\r", "\n")
    val blocks = cleanContent.split(Regex("\n\\s*\n"))

    for (block in blocks) {
        val lines = block.trim().lines()
        if (lines.isEmpty()) continue
        // Skip WEBVTT header block
        if (lines[0].trim().startsWith("WEBVTT")) continue

        var startMs = -1L
        var endMs = -1L
        var textStartIdx = -1

        for (i in lines.indices) {
            val line = lines[i].trim()
            val longMatcher = timePattern.matcher(line)
            val shortMatcher = shortTimePattern.matcher(line)

            if (longMatcher.find()) {
                startMs = longMatcher.group(1)!!.toLong() * 3600000 +
                        longMatcher.group(2)!!.toLong() * 60000 +
                        longMatcher.group(3)!!.toLong() * 1000 +
                        longMatcher.group(4)!!.toLong()
                endMs = longMatcher.group(5)!!.toLong() * 3600000 +
                        longMatcher.group(6)!!.toLong() * 60000 +
                        longMatcher.group(7)!!.toLong() * 1000 +
                        longMatcher.group(8)!!.toLong()
                textStartIdx = i + 1
                break
            } else if (shortMatcher.find()) {
                startMs = shortMatcher.group(1)!!.toLong() * 60000 +
                        shortMatcher.group(2)!!.toLong() * 1000 +
                        shortMatcher.group(3)!!.toLong()
                endMs = shortMatcher.group(4)!!.toLong() * 60000 +
                        shortMatcher.group(5)!!.toLong() * 1000 +
                        shortMatcher.group(6)!!.toLong()
                textStartIdx = i + 1
                break
            }
        }

        if (startMs < 0 || textStartIdx < 0 || textStartIdx >= lines.size) continue

        val text = lines.subList(textStartIdx, lines.size)
            .joinToString("\n")
            .replace(Regex("<[^>]+>"), "")
            .trim()

        if (text.isNotBlank()) {
            entries.add(SubtitleEntry(startMs, endMs, text))
        }
    }
    return entries.sortedBy { it.startMs }
}

/** Parse a subtitle file (auto-detects SRT vs VTT from content) */
private fun parseSubtitleFile(content: String): List<SubtitleEntry> {
    val trimmed = content.trimStart()
    return if (trimmed.startsWith("WEBVTT")) {
        parseVtt(content)
    } else {
        parseSrt(content)
    }
}

/** Find the active subtitle text for the given playback position */
private fun findActiveSubtitle(entries: List<SubtitleEntry>, positionMs: Long): String? {
    // Binary search for efficiency
    var lo = 0
    var hi = entries.size - 1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val entry = entries[mid]
        if (positionMs < entry.startMs) {
            hi = mid - 1
        } else if (positionMs > entry.endMs) {
            lo = mid + 1
        } else {
            return entry.text
        }
    }
    return null
}

// ── Helper functions ──────────────────────────────────────────────────────────

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

private fun queryFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        } catch (_: Exception) {}
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}

/** Read bytes from a content URI and normalize to clean UTF-8 */
private fun readAndNormalizeSubtitle(context: Context, uri: Uri): String? {
    val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    val cleanBytes = when {
        rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte() -> {
            rawBytes.copyOfRange(3, rawBytes.size)
        }
        rawBytes.size >= 2 &&
            rawBytes[0] == 0xFF.toByte() &&
            rawBytes[1] == 0xFE.toByte() -> {
            String(rawBytes.copyOfRange(2, rawBytes.size), Charsets.UTF_16LE).toByteArray(Charsets.UTF_8)
        }
        rawBytes.size >= 2 &&
            rawBytes[0] == 0xFE.toByte() &&
            rawBytes[1] == 0xFF.toByte() -> {
            String(rawBytes.copyOfRange(2, rawBytes.size), Charsets.UTF_16BE).toByteArray(Charsets.UTF_8)
        }
        else -> rawBytes
    }
    return String(cleanBytes, Charsets.UTF_8)
}

@OptIn(UnstableApi::class)
private fun buildVideoSource(
    context: Context,
    video: VideoItem,
    isLocalReady: Boolean,
    repository: TelegramRepository
): MediaSource {
    return if (isLocalReady) {
        val localUri = Uri.fromFile(File(video.localPath!!))
        val mediaItem = MediaItem.Builder()
            .setUri(localUri)
            .setMediaId(video.fileId.toString())
            .build()
        ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
            .createMediaSource(mediaItem)
    } else {
        val dataSourceFactory = TelegramDataSourceFactory(
            repository = repository,
            fileId = video.fileId,
            totalSize = video.fileSize,
            fileName = video.fileName
        )
        val streamUri = Uri.parse("https://telegram.stream/${video.fileId}/${Uri.encode(video.fileName)}")
        val mediaItem = MediaItem.Builder()
            .setUri(streamUri)
            .setMediaId(video.fileId.toString())
            .build()
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: VideoItem?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val repository = remember { TelegramRepository.getInstance(context) }

    var controlsVisible by remember { mutableStateOf(true) }
    var loadedSubtitleName by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }

    // Parsed subtitle entries and current active text
    var subtitleEntries by remember { mutableStateOf<List<SubtitleEntry>>(emptyList()) }
    var currentSubtitleText by remember { mutableStateOf<String?>(null) }

    // Lock screen into Landscape orientation and enable immersive mode during playback
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (video == null || video.fileId == 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Video not found.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Auto-hide overlay controls after delay
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    val isLocalFileReady = remember(video) {
        video.isDownloaded && video.localPath != null && File(video.localPath).exists()
    }

    // Build ExoPlayer with 10s seek increments and progressive streaming
    val exoPlayer = remember {
        val videoSource = buildVideoSource(
            context = context,
            video = video,
            isLocalReady = isLocalFileReady,
            repository = repository
        )

        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000L)
            .setSeekForwardIncrementMs(10000L)
            .build().apply {
                setMediaSource(videoSource)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                    }
                })
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Poll player position to update subtitle display (~4 times per second)
    LaunchedEffect(subtitleEntries) {
        if (subtitleEntries.isEmpty()) {
            currentSubtitleText = null
            return@LaunchedEffect
        }
        while (true) {
            val pos = exoPlayer.currentPosition
            currentSubtitleText = findActiveSubtitle(subtitleEntries, pos)
            delay(250)
        }
    }

    // Subtitle file picker launcher (.srt, .vtt, etc.)
    val subtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = queryFileName(context, uri) ?: "subtitles.srt"

                val content = readAndNormalizeSubtitle(context, uri)
                if (content.isNullOrBlank()) {
                    Toast.makeText(context, "Could not read subtitle file", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                val entries = parseSubtitleFile(content)
                if (entries.isEmpty()) {
                    Toast.makeText(context, "No subtitle entries found in file", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                subtitleEntries = entries
                loadedSubtitleName = fileName
                Log.d(TAG, "Loaded ${entries.size} subtitle entries from $fileName")
                Toast.makeText(context, "Subtitles loaded: $fileName (${entries.size} entries)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading subtitle", e)
                Toast.makeText(context, "Failed to load subtitle: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    setShowPreviousButton(false)
                    setShowNextButton(false)
                    setShowSubtitleButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    controllerShowTimeoutMs = 3500
                    controllerAutoShow = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom High-Contrast Subtitle Overlay (parsed from file, no ExoPlayer involvement)
        currentSubtitleText?.let { text ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (controlsVisible) 76.dp else 24.dp)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                text.split("\n").forEach { line ->
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.78f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    if (!isLocalFileReady) {
                        Text(
                            text = "Streaming from Telegram...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Custom Overlay Controls (Top bar & Quick Seek Buttons)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Floating Apple Frosted Glass Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .glassEffect(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color(0xCC0E131E),
                            borderAlphaTop = 0.45f,
                            borderAlphaBottom = 0.12f
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onNavigateBack,
                        size = 38.dp,
                        containerColor = Color(0x22FFFFFF)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.caption.ifBlank { video.fileName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusLabel = if (isLocalFileReady) "Offline (Downloaded)" else "Live Streaming"
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLocalFileReady) AppleGreen else AppleCyan
                            )
                            if (loadedSubtitleName != null) {
                                Text(
                                    text = "  •  Subtitles: $loadedSubtitleName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleOrange,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Subtitle Picker Glass Button
                    GlassIconButton(
                        onClick = {
                            subtitleLauncher.launch(arrayOf("text/*", "application/*", "*/*"))
                        },
                        size = 38.dp,
                        containerColor = if (loadedSubtitleName != null) Color(0x35FF9F0A) else Color(0x22FFFFFF)
                    ) {
                        Icon(
                            Icons.Default.Subtitles,
                            contentDescription = "Load Subtitles",
                            tint = if (loadedSubtitleName != null) AppleOrange else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Apple Glassy Seek Overlay Buttons (-10s / +10s)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.68f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    GlassIconButton(
                        onClick = { exoPlayer.seekBack() },
                        size = 60.dp,
                        containerColor = Color(0x40000000),
                        borderAlphaTop = 0.45f
                    ) {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Forward 10s
                    GlassIconButton(
                        onClick = { exoPlayer.seekForward() },
                        size = 60.dp,
                        containerColor = Color(0x40000000),
                        borderAlphaTop = 0.45f
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
