package mn.blazeapps.blazein.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.data.model.ChatSummary
import mn.blazeapps.blazein.data.model.ChatType
import mn.blazeapps.blazein.data.model.VideoItem
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TelegramRepository private constructor(private val context: Context) : Client.ResultHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = UserPreferences(context)

    private var client: Client? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val chatMap = ConcurrentHashMap<Long, TdApi.Chat>()
    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()

    private val _isLoadingChats = MutableStateFlow(false)
    val isLoadingChats: StateFlow<Boolean> = _isLoadingChats.asStateFlow()

    private val _selectedChatId = MutableStateFlow<Long?>(null)
    val selectedChatId: StateFlow<Long?> = _selectedChatId.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _hasMoreVideos = MutableStateFlow(true)
    val hasMoreVideos: StateFlow<Boolean> = _hasMoreVideos.asStateFlow()

    private var nextFromMessageIdVideo = 0L
    private var nextFromMessageIdDoc = 0L
    private var nextFromMessageIdHistory = 0L
    private val videoCache = ConcurrentHashMap<Long, VideoItem>()
    private val itunesService by lazy { mn.blazeapps.blazein.media.iTunesMetadataService(context) }
    private var metadataJob: kotlinx.coroutines.Job? = null

    private val _activeDownloadFileId = MutableStateFlow<Int?>(null)
    val activeDownloadFileId: StateFlow<Int?> = _activeDownloadFileId.asStateFlow()

    init {
        try {
            System.loadLibrary("tdjni")
        } catch (t: Throwable) {
            Log.w(TAG, "System.loadLibrary(tdjni) caught: ${t.message}")
        }
        createClient()
    }

    private fun createClient() {
        if (client != null) return
        client = Client.create(this, null, null)
    }

    override fun onResult(obj: TdApi.Object?) {
        if (obj == null) return
        when (obj.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val update = obj as TdApi.UpdateAuthorizationState
                handleAuthorizationState(update.authorizationState)
            }
            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val update = obj as TdApi.UpdateNewChat
                chatMap[update.chat.id] = update.chat
                refreshChatList()
            }
            TdApi.UpdateChatTitle.CONSTRUCTOR -> {
                val update = obj as TdApi.UpdateChatTitle
                chatMap[update.chatId]?.let {
                    it.title = update.title
                    refreshChatList()
                }
            }
            TdApi.UpdateChatPosition.CONSTRUCTOR -> {
                refreshChatList()
            }
            TdApi.UpdateFile.CONSTRUCTOR -> {
                val update = obj as TdApi.UpdateFile
                handleFileUpdate(update.file)
            }
        }
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        Log.d(TAG, "AuthorizationState: ${state.javaClass.simpleName}")
        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                _authState.value = AuthState.NeedParameters
                if (prefs.hasApiCredentials()) {
                    applyTdlibParameters(prefs.apiId, prefs.apiHash)
                }
            }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                _authState.value = AuthState.NeedPhoneNumber
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                _authState.value = AuthState.NeedCode
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                _authState.value = AuthState.NeedPassword
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                _authState.value = AuthState.Ready()
                fetchCurrentUser()
                loadAllChats()
            }
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                _authState.value = AuthState.LoggingOut
                _isLoadingChats.value = false
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                _authState.value = AuthState.Closed
                _isLoadingChats.value = false
                chatMap.clear()
                _chats.value = emptyList()
                _videos.value = emptyList()
                client = null
                createClient()
            }
            else -> {
                Log.d(TAG, "Unhandled auth state: ${state.constructor}")
            }
        }
    }

    fun applyTdlibParameters(apiId: Int, apiHash: String) {
        prefs.apiId = apiId
        prefs.apiHash = apiHash

        val dbDir = File(context.filesDir, "tdlib")
        if (!dbDir.exists()) dbDir.mkdirs()

        val filesDir = File(context.filesDir, "tdlib_files")
        if (!filesDir.exists()) filesDir.mkdirs()

        val request = TdApi.SetTdlibParameters().apply {
            this.useTestDc = false
            this.databaseDirectory = dbDir.absolutePath
            this.filesDirectory = filesDir.absolutePath
            this.databaseEncryptionKey = ByteArray(0)
            this.useFileDatabase = true
            this.useChatInfoDatabase = true
            this.useMessageDatabase = true
            this.useSecretChats = false
            this.apiId = apiId
            this.apiHash = apiHash
            this.systemLanguageCode = "en"
            this.deviceModel = "Android"
            this.systemVersion = android.os.Build.VERSION.RELEASE ?: "Unknown"
            this.applicationVersion = "1.0"
        }

        client?.send(request) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "SetTdlibParameters error: ${result.message}")
                _authState.value = AuthState.Error(result.message)
            }
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        prefs.phoneNumber = phoneNumber
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "SetAuthenticationPhoneNumber error: ${result.message}")
                _authState.value = AuthState.Error(result.message)
            }
        }
    }

    fun sendCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code)) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "CheckAuthenticationCode error: ${result.message}")
                _authState.value = AuthState.Error(result.message)
            }
        }
    }

    fun sendPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password)) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "CheckAuthenticationPassword error: ${result.message}")
                _authState.value = AuthState.Error(result.message)
            }
        }
    }

    private fun fetchCurrentUser() {
        client?.send(TdApi.GetMe()) { result ->
            if (result is TdApi.User) {
                val name = listOfNotNull(result.firstName, result.lastName)
                    .joinToString(" ").ifBlank { result.phoneNumber ?: "User" }
                _authState.value = AuthState.Ready(name)
            }
        }
    }

    fun loadAllChats() {
        if (_isLoadingChats.value) return
        _isLoadingChats.value = true
        fetchNextChatBatch()
    }

    private fun fetchNextChatBatch() {
        val currentClient = client
        if (currentClient == null) {
            _isLoadingChats.value = false
            return
        }
        currentClient.send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
            when (result) {
                is TdApi.Ok -> {
                    refreshChatList()
                    fetchNextChatBatch()
                }
                is TdApi.Error -> {
                    Log.d(TAG, "LoadChats finished: [${result.code}] ${result.message}")
                    _isLoadingChats.value = false
                    refreshChatList()
                }
                else -> {
                    _isLoadingChats.value = false
                    refreshChatList()
                }
            }
        }
    }

    fun loadChats(limit: Int = 100) {
        loadAllChats()
    }

    private fun refreshChatList() {
        val list = chatMap.values.map { chat ->
            val chatType = when (chat.type) {
                is TdApi.ChatTypePrivate -> ChatType.PRIVATE
                is TdApi.ChatTypeBasicGroup -> ChatType.BASIC_GROUP
                is TdApi.ChatTypeSupergroup -> {
                    val sg = chat.type as TdApi.ChatTypeSupergroup
                    if (sg.isChannel) ChatType.CHANNEL else ChatType.SUPERGROUP
                }
                else -> ChatType.UNKNOWN
            }
            ChatSummary(
                id = chat.id,
                title = chat.title.ifBlank { "Chat #${chat.id}" },
                type = chatType,
                unreadCount = chat.unreadCount
            )
        }.sortedBy { it.title.lowercase() }

        _chats.value = list
    }

    fun selectChat(chatId: Long) {
        _selectedChatId.value = chatId
        loadVideosForChat(chatId)
    }

    fun clearChatSelection() {
        _selectedChatId.value = null
        _videos.value = emptyList()
    }

    fun loadVideosForChat(chatId: Long) {
        _selectedChatId.value = chatId
        metadataJob?.cancel()
        videoCache.clear()
        _videos.value = emptyList()
        nextFromMessageIdVideo = 0L
        nextFromMessageIdDoc = 0L
        nextFromMessageIdHistory = 0L
        _hasMoreVideos.value = true
        fetchVideosBatch(chatId)
    }

    fun loadMoreVideos() {
        val chatId = _selectedChatId.value ?: return
        if (_isLoadingVideos.value || !_hasMoreVideos.value) return
        fetchVideosBatch(chatId)
    }

    private fun fetchVideosBatch(chatId: Long) {
        _isLoadingVideos.value = true

        var pendingCallbacks = 3
        var newFoundCount = 0

        val checkCompletion: () -> Unit = {
            pendingCallbacks--
            if (pendingCallbacks <= 0) {
                _isLoadingVideos.value = false
                if (newFoundCount == 0 && nextFromMessageIdVideo == 0L && nextFromMessageIdDoc == 0L) {
                    _hasMoreVideos.value = false
                }
                enrichVideosMetadata()
            }
        }

        // 1. Search Videos in Chat (cloud index for video content)
        val searchVideosQuery = TdApi.SearchChatMessages().apply {
            this.chatId = chatId
            this.query = ""
            this.fromMessageId = nextFromMessageIdVideo
            this.offset = 0
            this.limit = 100
            this.filter = TdApi.SearchMessagesFilterVideo()
        }
        client?.send(searchVideosQuery) { result ->
            if (result is TdApi.FoundChatMessages) {
                nextFromMessageIdVideo = result.nextFromMessageId
                val items = result.messages.mapNotNull { extractVideoItem(it) }
                newFoundCount += items.size
                items.forEach { videoCache[it.messageId] = it }
                updateVideosList()
            }
            checkCompletion()
        }

        // 2. Search Documents in Chat (for MKV, MP4, WebM sent as document files)
        val searchDocsQuery = TdApi.SearchChatMessages().apply {
            this.chatId = chatId
            this.query = ""
            this.fromMessageId = nextFromMessageIdDoc
            this.offset = 0
            this.limit = 100
            this.filter = TdApi.SearchMessagesFilterDocument()
        }
        client?.send(searchDocsQuery) { result ->
            if (result is TdApi.FoundChatMessages) {
                nextFromMessageIdDoc = result.nextFromMessageId
                val items = result.messages.mapNotNull { extractVideoItem(it) }
                newFoundCount += items.size
                items.forEach { videoCache[it.messageId] = it }
                updateVideosList()
            }
            checkCompletion()
        }

        // 3. Scan recent history (catches animations and newly arrived messages)
        client?.send(TdApi.GetChatHistory(chatId, nextFromMessageIdHistory, 0, 100, false)) { result ->
            if (result is TdApi.Messages) {
                if (result.messages.isNotEmpty()) {
                    nextFromMessageIdHistory = result.messages.last().id
                }
                val items = result.messages.mapNotNull { extractVideoItem(it) }
                newFoundCount += items.size
                items.forEach { videoCache[it.messageId] = it }
                updateVideosList()
            }
            checkCompletion()
        }
    }

    private fun updateVideosList() {
        _videos.value = videoCache.values.sortedByDescending { it.messageId }
    }

    private fun enrichVideosMetadata() {
        // Thumbnail & poster functionality completely removed per user request
    }

    private fun isVideoMime(mime: String): Boolean {
        if (mime.isBlank()) return false
        val lower = mime.lowercase()
        return lower.startsWith("video/") ||
                lower.contains("matroska") ||
                lower.contains("mp4") ||
                lower.contains("webm") ||
                lower.contains("quicktime") ||
                lower.contains("avi") ||
                lower.contains("x-msvideo") ||
                lower.contains("x-flv") ||
                lower.contains("x-matroska")
    }

    private fun isVideoFileName(fileName: String): Boolean {
        if (fileName.isBlank()) return false
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return VIDEO_EXTENSIONS.contains(ext)
    }

    private fun extractVideoItem(msg: TdApi.Message): VideoItem? {
        return when (val content = msg.content) {
            is TdApi.MessageVideo -> {
                val v = content.video
                val file = v.video
                val isDownloaded = file.local?.isDownloadingCompleted == true
                val localPath = if (isDownloaded) file.local?.path else null
                val expected = if (file.expectedSize > 0) file.expectedSize else file.size
                val downloaded = file.local?.downloadedSize ?: 0
                val progress = if (expected > 0) downloaded.toFloat() / expected else 0f

                VideoItem(
                    messageId = msg.id,
                    chatId = msg.chatId,
                    fileId = file.id,
                    fileName = v.fileName?.ifBlank { "video_${msg.id}.mp4" } ?: "video_${msg.id}.mp4",
                    caption = content.caption?.text ?: "",
                    durationSeconds = v.duration,
                    fileSize = expected,
                    width = v.width,
                    height = v.height,
                    localPath = localPath,
                    isDownloaded = isDownloaded,
                    downloadProgress = progress,
                    isDownloading = file.local?.isDownloadingActive == true,
                    thumbnailPath = null
                )
            }
            is TdApi.MessageDocument -> {
                val doc = content.document
                val mime = doc.mimeType ?: ""
                val fileName = doc.fileName ?: ""
                val isVideo = isVideoMime(mime) || isVideoFileName(fileName)

                if (!isVideo) return null

                val file = doc.document
                val isDownloaded = file.local?.isDownloadingCompleted == true
                val localPath = if (isDownloaded) file.local?.path else null
                val expected = if (file.expectedSize > 0) file.expectedSize else file.size
                val downloaded = file.local?.downloadedSize ?: 0
                val progress = if (expected > 0) downloaded.toFloat() / expected else 0f

                VideoItem(
                    messageId = msg.id,
                    chatId = msg.chatId,
                    fileId = file.id,
                    fileName = fileName.ifBlank { "video_${msg.id}.mp4" },
                    caption = content.caption?.text ?: "",
                    durationSeconds = 0,
                    fileSize = expected,
                    width = 0,
                    height = 0,
                    localPath = localPath,
                    isDownloaded = isDownloaded,
                    downloadProgress = progress,
                    isDownloading = file.local?.isDownloadingActive == true,
                    thumbnailPath = null
                )
            }
            is TdApi.MessageAnimation -> {
                val anim = content.animation
                val file = anim.animation
                val isDownloaded = file.local?.isDownloadingCompleted == true
                val localPath = if (isDownloaded) file.local?.path else null
                val expected = if (file.expectedSize > 0) file.expectedSize else file.size
                val downloaded = file.local?.downloadedSize ?: 0
                val progress = if (expected > 0) downloaded.toFloat() / expected else 0f

                VideoItem(
                    messageId = msg.id,
                    chatId = msg.chatId,
                    fileId = file.id,
                    fileName = anim.fileName?.ifBlank { "clip_${msg.id}.mp4" } ?: "clip_${msg.id}.mp4",
                    caption = content.caption?.text ?: "",
                    durationSeconds = anim.duration,
                    fileSize = expected,
                    width = anim.width,
                    height = anim.height,
                    localPath = localPath,
                    isDownloaded = isDownloaded,
                    downloadProgress = progress,
                    isDownloading = file.local?.isDownloadingActive == true,
                    thumbnailPath = null
                )
            }
            else -> null // Strictly exclude photos, audio, voice notes, stickers, text
        }
    }

    private val fileUpdateListeners = ConcurrentHashMap<Int, MutableList<() -> Unit>>()

    fun registerFileUpdateListener(fileId: Int, listener: () -> Unit) {
        fileUpdateListeners.computeIfAbsent(fileId) { java.util.concurrent.CopyOnWriteArrayList() }.add(listener)
    }

    fun unregisterFileUpdateListener(fileId: Int, listener: () -> Unit) {
        fileUpdateListeners[fileId]?.remove(listener)
    }

    fun downloadVideo(fileId: Int) {
        _activeDownloadFileId.value = fileId
        client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false)) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "DownloadFile error: ${result.message}")
            }
        }
        // Also look for matching SRT in background and download alongside video
        scope.launch {
            try {
                val videoItem = videoCache.values.firstOrNull { it.fileId == fileId } ?: return@launch
                val matchingDoc = findMatchingSrt(videoItem.chatId, videoItem.messageId, videoItem.fileName)
                if (matchingDoc != null) {
                    downloadSrtDocument(matchingDoc)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Search the Telegram chat for a corresponding .srt or .vtt subtitle file.
     * Checks nearby messages first, then searches chat documents if needed.
     */
    suspend fun findMatchingSrt(chatId: Long, videoMessageId: Long, videoFileName: String): TdApi.Document? = withContext(Dispatchers.IO) {
        if (chatId == 0L) return@withContext null

        val videoBase = videoFileName.substringBeforeLast('.').trim().lowercase()
        val videoTokens = extractSearchTokens(videoBase)

        val candidates = mutableListOf<Pair<Long, TdApi.Document>>() // Pair(messageId, Document)

        // 1. Check nearby messages around videoMessageId (typically subtitles are posted immediately adjacent)
        if (videoMessageId != 0L) {
            try {
                val history = execute(TdApi.GetChatHistory(chatId, videoMessageId, -10, 21, false))
                for (msg in history.messages) {
                    if (msg.id == videoMessageId) continue
                    val content = msg.content
                    if (content is TdApi.MessageDocument) {
                        val doc = content.document
                        val docName = doc.fileName?.lowercase() ?: ""
                        if (docName.endsWith(".srt") || docName.endsWith(".vtt")) {
                            candidates.add(msg.id to doc)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch nearby chat history for srt search: ${e.message}")
            }
        }

        // 2. If no nearby candidates, search chat documents for ".srt"
        if (candidates.isEmpty()) {
            try {
                val searchQuery = TdApi.SearchChatMessages().apply {
                    this.chatId = chatId
                    this.query = ".srt"
                    this.fromMessageId = 0
                    this.offset = 0
                    this.limit = 50
                    this.filter = TdApi.SearchMessagesFilterDocument()
                }
                val searchResult = execute(searchQuery)
                for (msg in searchResult.messages) {
                    if (msg.id == videoMessageId) continue
                    val content = msg.content
                    if (content is TdApi.MessageDocument) {
                        val doc = content.document
                        val docName = doc.fileName?.lowercase() ?: ""
                        if (docName.endsWith(".srt") || docName.endsWith(".vtt")) {
                            candidates.add(msg.id to doc)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "SearchChatMessages for srt failed: ${e.message}")
            }
        }

        if (candidates.isEmpty()) return@withContext null

        // 3. Score candidates to find the best match
        var bestDoc: TdApi.Document? = null
        var bestScore = -1

        for ((msgId, doc) in candidates) {
            val docName = doc.fileName?.lowercase() ?: continue
            val docBase = docName.substringBeforeLast('.').trim()
            val docTokens = extractSearchTokens(docBase)

            var score = 0

            // Exact or prefix match
            if (docBase == videoBase || docBase.startsWith(videoBase) || videoBase.startsWith(docBase)) {
                score += 1000
            } else {
                // Token overlap
                val commonTokens = videoTokens.intersect(docTokens)
                score += commonTokens.size * 100
            }

            // Proximity bonus if messageId is close (very common in movie channels)
            if (videoMessageId != 0L) {
                val distance = kotlin.math.abs(msgId - videoMessageId)
                if (distance <= 2) {
                    score += 350
                } else if (distance <= 5) {
                    score += 200
                } else if (distance <= 10) {
                    score += 100
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestDoc = doc
            }
        }

        if (bestScore >= 150) bestDoc else null
    }

    /**
     * Download an SRT document via TDLib and await completion.
     */
    suspend fun downloadSrtDocument(doc: TdApi.Document): File? = withContext(Dispatchers.IO) {
        val fileId = doc.document.id
        if (doc.document.local?.isDownloadingCompleted == true && !doc.document.local.path.isNullOrBlank()) {
            val f = File(doc.document.local.path)
            if (f.exists()) return@withContext f
        }

        try {
            // Trigger high-priority download
            client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false), null)

            // Await completion with 15s timeout
            withTimeoutOrNull(15000L) {
                suspendCancellableCoroutine<File?> { cont ->
                    val listener = object : () -> Unit {
                        override fun invoke() {
                            client?.send(TdApi.GetFile(fileId)) { res ->
                                if (res is TdApi.File && res.local?.isDownloadingCompleted == true) {
                                    val path = res.local.path
                                    if (!path.isNullOrBlank()) {
                                        val file = File(path)
                                        if (file.exists() && cont.isActive) {
                                            cont.resume(file)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    registerFileUpdateListener(fileId, listener)
                    cont.invokeOnCancellation {
                        unregisterFileUpdateListener(fileId, listener)
                    }
                    // Initial check
                    listener.invoke()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error downloading SRT document $fileId: ${e.message}")
            null
        }
    }

    private fun extractSearchTokens(text: String): Set<String> {
        val stopWords = setOf(
            "1080p", "720p", "480p", "2160p", "4k", "bluray", "webrip", "web-dl", "hdtv",
            "x264", "x265", "hevc", "dvdrip", "aac", "aac5", "yts", "yify", "hindi",
            "english", "telugu", "tamil", "dual", "audio", "sub", "subs", "srt", "vtt",
            "the", "and", "mp4", "mkv", "avi"
        )
        return text.split(Regex("[^a-zA-Z0-9]+"))
            .map { it.lowercase().trim() }
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()
    }

    fun downloadVideoFromOffset(fileId: Int, offset: Long, limit: Long = 0L, priority: Int = 32) {
        client?.send(TdApi.DownloadFile(fileId, priority, offset, limit, false)) { result ->
            if (result is TdApi.Error) {
                Log.w(TAG, "DownloadFile from offset $offset error: ${result.message}")
            }
        }
    }

    suspend fun getDownloadedPrefixSize(fileId: Int, offset: Long): Long {
        return try {
            val res = execute(TdApi.GetFileDownloadedPrefixSize(fileId, offset))
            res.size
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun readFilePart(fileId: Int, offset: Long, count: Long): ByteArray? {
        return try {
            val res = execute(TdApi.ReadFilePart(fileId, offset, count))
            res.data
        } catch (e: Exception) {
            null
        }
    }

    fun cancelDownload(fileId: Int) {
        client?.send(TdApi.CancelDownloadFile(fileId, false), null)
        if (_activeDownloadFileId.value == fileId) {
            _activeDownloadFileId.value = null
        }
    }

    private fun handleFileUpdate(file: TdApi.File) {
        // Notify streaming listeners waiting for new chunks
        fileUpdateListeners[file.id]?.forEach { it.invoke() }

        val currentList = _videos.value
        if (currentList.isEmpty()) return

        var updated = false
        val newList = currentList.map { item ->
            if (item.fileId == file.id) {
                updated = true
                val isDownloaded = file.local?.isDownloadingCompleted == true
                val expected = if (file.expectedSize > 0) file.expectedSize else file.size
                val downloaded = file.local?.downloadedSize ?: 0
                val progress = if (expected > 0) downloaded.toFloat() / expected else 0f

                if (isDownloaded && _activeDownloadFileId.value == file.id) {
                    _activeDownloadFileId.value = null
                }

                item.copy(
                    localPath = if (isDownloaded) file.local?.path else item.localPath,
                    isDownloaded = isDownloaded,
                    downloadProgress = progress,
                    isDownloading = file.local?.isDownloadingActive == true
                )
            } else {
                item
            }
        }

        if (updated) {
            _videos.value = newList
        }
    }

    fun logout() {
        client?.send(TdApi.LogOut()) {
            prefs.clear()
        }
    }

    suspend fun <T : TdApi.Object> execute(query: TdApi.Function<T>): T =
        suspendCancellableCoroutine { cont ->
            client?.send(query) { result ->
                if (result is TdApi.Error) {
                    cont.resumeWithException(Exception("TDLib Error [${result.code}]: ${result.message}"))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    cont.resume(result as T)
                }
            } ?: cont.resumeWithException(IllegalStateException("Client is null"))
        }

    companion object {
        private const val TAG = "TelegramRepository"
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "mov", "webm", "avi", "flv", "wmv", "m4v",
            "3gp", "ts", "mpg", "mpeg", "m2ts", "vob", "ogv", "divx", "asf"
        )

        @Volatile
        private var instance: TelegramRepository? = null

        fun getInstance(context: Context): TelegramRepository {
            return instance ?: synchronized(this) {
                instance ?: TelegramRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
