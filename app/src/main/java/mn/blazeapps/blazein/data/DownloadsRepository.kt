package mn.blazeapps.blazein.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mn.blazeapps.blazein.data.model.MovieMetadata
import mn.blazeapps.blazein.media.iTunesMetadataService
import java.io.File

data class DownloadedVideo(
    val file: File,
    val title: String,
    val size: Long,
    val metadata: MovieMetadata? = null,
    val thumbnailPath: String? = null
)

class DownloadsRepository(private val context: Context) {

    private val itunesService = iTunesMetadataService(context)

    suspend fun getDownloadedVideos(): List<DownloadedVideo> = withContext(Dispatchers.IO) {
        val filesDir = File(context.filesDir, "tdlib_files")
        val allFiles = mutableListOf<File>()

        if (filesDir.exists()) {
            filesDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    allFiles.add(file)
                }
            }
        }

        val videoExtensions = setOf("mp4", "mkv", "mov", "webm", "avi", "flv", "wmv", "m4v")
        
        allFiles.filter { videoExtensions.contains(it.extension.lowercase()) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                DownloadedVideo(
                    file = file,
                    title = file.name,
                    size = file.length(),
                    thumbnailPath = null
                )
            }
    }

    suspend fun deleteVideo(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) {
            file.delete()
        } else false
    }

    suspend fun deleteAllVideos(): Int = withContext(Dispatchers.IO) {
        val filesDir = File(context.filesDir, "tdlib_files")
        var deletedCount = 0
        if (filesDir.exists()) {
            val videoExtensions = setOf("mp4", "mkv", "mov", "webm", "avi", "flv", "wmv", "m4v")
            filesDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val isVideoExt = videoExtensions.contains(file.extension.lowercase())
                    val isMediaFolder = file.parentFile?.name?.lowercase() in setOf("videos", "video", "documents", "document", "animations", "animation")
                    if (isVideoExt || isMediaFolder) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
        }
        deletedCount
    }

    suspend fun fetchMetadata(rawFileName: String): MovieMetadata? = withContext(Dispatchers.IO) {
        itunesService.fetchMetadataForFile(rawFileName)
    }
}
