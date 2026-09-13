package mn.blazeapps.blazein.data.model

sealed class AuthState {
    object Initializing : AuthState()
    object NeedParameters : AuthState()
    object NeedPhoneNumber : AuthState()
    object NeedCode : AuthState()
    object NeedPassword : AuthState()
    data class Ready(val userFirstName: String = "") : AuthState()
    object LoggingOut : AuthState()
    object Closed : AuthState()
    data class Error(val message: String) : AuthState()
}

enum class ChatType {
    CHANNEL,
    SUPERGROUP,
    BASIC_GROUP,
    PRIVATE,
    UNKNOWN
}

data class ChatSummary(
    val id: Long,
    val title: String,
    val type: ChatType,
    val unreadCount: Int = 0
)

data class VideoItem(
    val messageId: Long,
    val chatId: Long,
    val fileId: Int,
    val fileName: String,
    val caption: String,
    val durationSeconds: Int,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val localPath: String? = null,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val thumbnailPath: String? = null
)
