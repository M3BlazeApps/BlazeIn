package mn.blazeapps.blazein.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mn.blazeapps.blazein.data.DownloadedVideo
import mn.blazeapps.blazein.ui.theme.*
import mn.blazeapps.blazein.ui.viewmodel.DownloadsViewModel
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onNavigateBack: () -> Unit,
    onPlayVideo: (File) -> Unit
) {
    val videos by viewModel.downloadedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(
                title = {
                    Text(
                        "Downloads",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppleTextPrimary
                    )
                },
                navigationIcon = {
                    GlassIconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppleTextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        GlassBackground(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppleCyan)
                }
            } else if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No downloaded videos found.", color = AppleTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(videos, key = { it.file.absolutePath }) { video ->
                        DownloadedVideoCard(
                            video = video,
                            onPlay = { onPlayVideo(video.file) },
                            onDelete = { viewModel.deleteVideo(video.file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadedVideoCard(
    video: DownloadedVideo,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0x1A25344E),
        borderAlphaTop = 0.35f,
        borderAlphaBottom = 0.08f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1F162030))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (video.metadata?.posterUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = video.metadata.posterUrl,
                            contentDescription = "Poster",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = AppleTextSecondary, modifier = Modifier.size(40.dp))
                    }
                }

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.metadata?.title ?: video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (video.metadata != null) {
                        Text(
                            text = "${video.metadata.year} • ${video.metadata.genre}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = video.metadata.plot,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleTextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Size: ${formatFileSize(video.size)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleTextSecondary
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
            
            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.metadata != null) {
                    Text(
                        text = formatFileSize(video.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppleTextTertiary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x20FF453A))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppleRed, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = AppleCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(size.toDouble()) / ln(1024.0)).toInt()
    return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}
