package mn.blazeapps.blazein.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLEncoder

data class DownloadedVideo(
    val file: File,
    val title: String,
    val size: Long,
    val metadata: MovieMetadata? = null
)

data class MovieMetadata(
    val title: String,
    val year: String,
    val genre: String,
    val plot: String,
    val posterUrl: String
)

class DownloadsRepository(private val context: Context) {

    suspend fun getDownloadedVideos(): List<DownloadedVideo> = withContext(Dispatchers.IO) {
        val filesDir = File(context.filesDir, "tdlib_files")
        val videoDir = File(filesDir, "video")
        val docDir = File(filesDir, "document")
        val animDir = File(filesDir, "animation")

        val allFiles = mutableListOf<File>()
        
        if (videoDir.exists()) allFiles.addAll(videoDir.listFiles()?.toList() ?: emptyList())
        if (docDir.exists()) allFiles.addAll(docDir.listFiles()?.toList() ?: emptyList())
        if (animDir.exists()) allFiles.addAll(animDir.listFiles()?.toList() ?: emptyList())

        val videoExtensions = setOf("mp4", "mkv", "mov", "webm", "avi", "flv", "wmv", "m4v")
        
        allFiles.filter { it.isFile && videoExtensions.contains(it.extension.lowercase()) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val cleanTitle = cleanFileName(file.nameWithoutExtension)
                DownloadedVideo(
                    file = file,
                    title = cleanTitle,
                    size = file.length()
                )
            }
    }

    suspend fun deleteVideo(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) {
            file.delete()
        } else false
    }

    suspend fun fetchMetadata(title: String): MovieMetadata? = withContext(Dispatchers.IO) {
        try {
            // Use iTunes Search API
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedTitle&entity=movie&limit=1"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getInt("resultCount") > 0) {
                val result = json.getJSONArray("results").getJSONObject(0)
                return@withContext MovieMetadata(
                    title = result.optString("trackName", title),
                    year = result.optString("releaseDate", "").take(4),
                    genre = result.optString("primaryGenreName", ""),
                    plot = result.optString("longDescription", result.optString("shortDescription", "")),
                    posterUrl = result.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                )
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanFileName(fileName: String): String {
        // Remove common release tags
        var clean = fileName.replace(".", " ").replace("_", " ")
        val tagsToRemove = listOf(
            "1080p", "720p", "480p", "2160p", "4k", "bluray", "web-dl", "webrip", "hdrip",
            "x264", "x265", "hevc", "aac", "dts", "dual audio", "hindi", "english"
        )
        for (tag in tagsToRemove) {
            clean = clean.replace(Regex("(?i)\\b$tag\\b"), "")
        }
        // Remove stuff in brackets like [Erai-raws] or (2023) if needed, though year might be useful for search
        clean = clean.replace(Regex("\\[.*?\\]"), "")
        
        // Trim extra spaces
        return clean.replace(Regex("\\s+"), " ").trim()
    }
}
