package mn.blazeapps.blazein.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mn.blazeapps.blazein.data.DownloadedVideo
import mn.blazeapps.blazein.data.DownloadsRepository
import java.io.File

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadsRepository(application)

    private val _downloadedVideos = MutableStateFlow<List<DownloadedVideo>>(emptyList())
    val downloadedVideos: StateFlow<List<DownloadedVideo>> = _downloadedVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val videos = repository.getDownloadedVideos()
            _downloadedVideos.value = videos
            _isLoading.value = false
            
            // Fetch metadata sequentially or concurrently
            fetchMetadataForVideos(videos)
        }
    }

    private fun fetchMetadataForVideos(videos: List<DownloadedVideo>) {
        viewModelScope.launch {
            val updatedList = videos.toMutableList()
            for (i in updatedList.indices) {
                val video = updatedList[i]
                if (video.metadata == null) {
                    val metadata = repository.fetchMetadata(video.title)
                    if (metadata != null) {
                        updatedList[i] = video.copy(metadata = metadata)
                        _downloadedVideos.value = updatedList.toList()
                    }
                }
            }
        }
    }

    fun deleteVideo(file: File) {
        viewModelScope.launch {
            if (repository.deleteVideo(file)) {
                _downloadedVideos.value = _downloadedVideos.value.filter { it.file.absolutePath != file.absolutePath }
            }
        }
    }

    fun deleteAllVideos(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.deleteAllVideos()
            _downloadedVideos.value = emptyList()
            onComplete?.invoke(count)
        }
    }
}
