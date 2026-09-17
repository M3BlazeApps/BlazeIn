package mn.blazeapps.blazein.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mn.blazeapps.blazein.data.TelegramRepository
import mn.blazeapps.blazein.data.UserPreferences
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.data.model.ChatSummary
import mn.blazeapps.blazein.data.model.VideoItem

class TelegramViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TelegramRepository.getInstance(application)
    private val prefs = UserPreferences(application)

    val authState: StateFlow<AuthState> = repository.authState
    val chats: StateFlow<List<ChatSummary>> = repository.chats
    val isLoadingChats: StateFlow<Boolean> = repository.isLoadingChats
    val selectedChatId: StateFlow<Long?> = repository.selectedChatId
    val videos: StateFlow<List<VideoItem>> = repository.videos
    val isLoadingVideos: StateFlow<Boolean> = repository.isLoadingVideos
    val hasMoreVideos: StateFlow<Boolean> = repository.hasMoreVideos
    val activeDownloadFileId: StateFlow<Int?> = repository.activeDownloadFileId

    var apiIdInput by mutableStateOf(if (prefs.apiId > 0) prefs.apiId.toString() else "")
    var apiHashInput by mutableStateOf(prefs.apiHash)
    var phoneInput by mutableStateOf(prefs.phoneNumber)
    var otpInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)

    // Current video to play in player screen
    var selectedVideoForPlayback by mutableStateOf<VideoItem?>(null)

    fun submitParameters() {
        val apiId = apiIdInput.trim().toIntOrNull()
        val apiHash = apiHashInput.trim()
        if (apiId == null || apiId <= 0 || apiHash.isBlank()) {
            errorMessage = "Please enter valid API ID and API Hash"
            return
        }
        errorMessage = null
        repository.applyTdlibParameters(apiId, apiHash)
    }

    fun submitPhoneNumber() {
        val phone = phoneInput.trim()
        if (phone.isBlank()) {
            errorMessage = "Please enter a valid phone number"
            return
        }
        errorMessage = null
        repository.setPhoneNumber(phone)
    }

    fun submitOtp() {
        val code = otpInput.trim()
        if (code.isBlank()) {
            errorMessage = "Please enter the OTP verification code"
            return
        }
        errorMessage = null
        repository.sendCode(code)
    }

    fun submitPassword() {
        val password = passwordInput.trim()
        if (password.isBlank()) {
            errorMessage = "Please enter your 2FA password"
            return
        }
        errorMessage = null
        repository.sendPassword(password)
    }

    fun selectChat(chatId: Long) {
        repository.selectChat(chatId)
    }

    fun loadAllChats() {
        repository.loadAllChats()
    }

    fun loadMoreVideos() {
        repository.loadMoreVideos()
    }

    fun refreshVideos() {
        selectedChatId.value?.let { repository.loadVideosForChat(it) }
    }

    fun downloadVideo(fileId: Int) {
        repository.downloadVideo(fileId)
    }

    fun cancelDownload(fileId: Int) {
        repository.cancelDownload(fileId)
    }

    fun logout() {
        repository.logout()
        apiIdInput = ""
        apiHashInput = ""
        phoneInput = ""
        otpInput = ""
        passwordInput = ""
        errorMessage = null
        selectedVideoForPlayback = null
    }

    fun clearError() {
        errorMessage = null
    }
}
