# Complete Telegram & TDLib Integration Guide for Android

A comprehensive engineering guide for connecting an Android app to Telegram's MTProto network using **TDLib (Telegram Database Library)**, Jetpack Compose, and ExoPlayer / Media3.

---

## 1. Architecture Overview

Unlike the Telegram Bot API (which is stateless, limited to bot interactions, and capped at 50MB files), **TDLib** is the official, full-featured client library powering Telegram apps.

### Key Capabilities:
- Direct connection to MTProto servers with end-to-end encryption.
- Full access to the user's subscribed **Channels, Supergroups, Groups, and Direct Messages**.
- Streaming and downloading files up to **2 GB** (or **4 GB** for Telegram Premium).
- Chunk-by-chunk progressive streaming into media players (ExoPlayer / Media3) without waiting for full downloads.
- Offline database caching (chat history, users, file metadata).

### Layered Architecture:
```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (HomeScreen, VideoPlayerScreen, SettingsScreen)      │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Actions
┌───────────────────────────┴────────────────────────────┐
│                    TelegramViewModel                   │
│         (Auth inputs, chat selection, playback)        │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutines / Repository API
┌───────────────────────────┴────────────────────────────┐
│                   TelegramRepository                   │
│     (Client.ResultHandler, StateFlows, File cache)     │
└───────────────────────────▲────────────────────────────┘
                            │ JNI (Java Native Interface)
┌───────────────────────────┴────────────────────────────┐
│                TDLib Native Library (C++)              │
│        (libtdjni.so, libsslx.so, libcryptox.so)        │
└────────────────────────────────────────────────────────┘
```

---

## 2. Gradle & NDK Configuration

### A. Add Dependency
Add the prebuilt Android TDLib library to your project (`gradle/libs.versions.toml` and `build.gradle.kts`):

**`gradle/libs.versions.toml`**:
```toml
[versions]
tdlib = "11850efeb5791d4fd386c0eeec1ebce4c5080435"
media3 = "1.5.1"

[libraries]
tdlib-android = { group = "com.github.capullo-tech", name = "lib-tdlib-android", version.ref = "tdlib" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
```

**`app/build.gradle.kts`**:
```kotlin
android {
    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    packaging {
        jniLibs {
            // Essential for extracting TDLib native shared libraries (.so)
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.tdlib.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
}
```

---

## 3. Data Models

Create sealed classes and data structures to represent the state machine:

```kotlin
// AuthState.kt
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

// ChatType.kt
enum class ChatType {
    CHANNEL,
    SUPERGROUP,
    BASIC_GROUP,
    PRIVATE,
    UNKNOWN
}

// ChatSummary.kt
data class ChatSummary(
    val id: Long,
    val title: String,
    val type: ChatType,
    val unreadCount: Int = 0
)

// VideoItem.kt
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
```

---

## 4. Initializing TDLib & Handling Updates

Create a singleton repository implementing `Client.ResultHandler`:

```kotlin
class TelegramRepository private constructor(private val context: Context) : Client.ResultHandler {

    private var client: Client? = null
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Load native JNI library
        try {
            System.loadLibrary("tdjni")
        } catch (t: Throwable) {
            Log.w("TelegramRepo", "System.loadLibrary caught: ${t.message}")
        }
        createClient()
    }

    private fun createClient() {
        if (client != null) return
        // Pass 'this' as update handler
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
}
```

---

## 5. The Authentication State Machine

Telegram uses a progressive, multi-step authentication flow:

```
[Start] ──> WaitTdlibParameters ──> WaitPhoneNumber ──> WaitCode ──> [WaitPassword] ──> Ready
```

### Implementing Auth Handlers:
```kotlin
private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
    when (state.constructor) {
        TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
            _authState.value = AuthState.NeedParameters
            // If stored credentials exist, automatically apply them:
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
            _authState.value = AuthState.NeedPassword // 2FA is enabled
        }
        TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
            _authState.value = AuthState.Ready()
            fetchCurrentUser()
            loadAllChats()
        }
        TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
            _authState.value = AuthState.LoggingOut
        }
        TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
            _authState.value = AuthState.Closed
            chatMap.clear()
            _chats.value = emptyList()
            client = null
            createClient() // Re-initialize for future logins
        }
    }
}
```

### Step 1: Set TDLib Parameters
API credentials can be generated at [my.telegram.org](https://my.telegram.org):

```kotlin
fun applyTdlibParameters(apiId: Int, apiHash: String) {
    val dbDir = File(context.filesDir, "tdlib").apply { if (!exists()) mkdirs() }
    val filesDir = File(context.filesDir, "tdlib_files").apply { if (!exists()) mkdirs() }

    val request = TdApi.SetTdlibParameters().apply {
        this.useTestDc = false
        this.databaseDirectory = dbDir.absolutePath
        this.filesDirectory = filesDir.absolutePath
        this.databaseEncryptionKey = ByteArray(0) // or pass encryption key
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
            _authState.value = AuthState.Error(result.message)
        }
    }
}
```

### Step 2: Send Phone Number
```kotlin
fun setPhoneNumber(phoneNumber: String) {
    val settings = TdApi.PhoneNumberAuthenticationSettings()
    client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)) { result ->
        if (result is TdApi.Error) {
            _authState.value = AuthState.Error(result.message)
        }
    }
}
```

### Step 3: Verify OTP Code
```kotlin
fun sendCode(code: String) {
    client?.send(TdApi.CheckAuthenticationCode(code)) { result ->
        if (result is TdApi.Error) {
            _authState.value = AuthState.Error(result.message)
        }
    }
}
```

### Step 4: Verify 2FA Password (Optional)
```kotlin
fun sendPassword(password: String) {
    client?.send(TdApi.CheckAuthenticationPassword(password)) { result ->
        if (result is TdApi.Error) {
            _authState.value = AuthState.Error(result.message)
        }
    }
}
```

---

## 6. Discovering & Loading Channels & Chats

TDLib returns chats incrementally via `TdApi.LoadChats`. To load the user's complete chat list:

```kotlin
private val chatMap = ConcurrentHashMap<Long, TdApi.Chat>()
private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()

fun loadAllChats() {
    fetchNextChatBatch()
}

private fun fetchNextChatBatch() {
    val currentClient = client ?: return
    currentClient.send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
        when (result) {
            is TdApi.Ok -> {
                refreshChatList()
                // Recurse until TDLib returns 404 (all chats loaded)
                fetchNextChatBatch()
            }
            is TdApi.Error -> {
                // Code 404 means no more chats to load
                refreshChatList()
            }
        }
    }
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
```

---

## 7. Media Extraction (Videos, Documents, Clips)

Channels often send media as:
1. Native Videos (`MessageVideo`)
2. Files / Documents like `.mkv`, `.mp4` (`MessageDocument`)
3. Video animations / clips (`MessageAnimation`)

Using a 3-way parallel search ensures you never miss media in large channels:

```kotlin
fun fetchVideosBatch(chatId: Long) {
    // 1. Cloud Search for native Videos
    val videoQuery = TdApi.SearchChatMessages().apply {
        this.chatId = chatId
        this.fromMessageId = nextFromMessageIdVideo
        this.limit = 100
        this.filter = TdApi.SearchMessagesFilterVideo()
    }
    client?.send(videoQuery) { result ->
        if (result is TdApi.FoundChatMessages) {
            nextFromMessageIdVideo = result.nextFromMessageId
            result.messages.mapNotNull { extractVideoItem(it) }.forEach {
                videoCache[it.messageId] = it
            }
            updateVideosList()
        }
    }

    // 2. Cloud Search for Documents (MKV, MP4 sent as files)
    val docQuery = TdApi.SearchChatMessages().apply {
        this.chatId = chatId
        this.fromMessageId = nextFromMessageIdDoc
        this.limit = 100
        this.filter = TdApi.SearchMessagesFilterDocument()
    }
    client?.send(docQuery) { result ->
        if (result is TdApi.FoundChatMessages) {
            nextFromMessageIdDoc = result.nextFromMessageId
            result.messages.mapNotNull { extractVideoItem(it) }.forEach {
                videoCache[it.messageId] = it
            }
            updateVideosList()
        }
    }

    // 3. Scan recent chat history for animations / new posts
    client?.send(TdApi.GetChatHistory(chatId, nextFromMessageIdHistory, 0, 100, false)) { result ->
        if (result is TdApi.Messages) {
            if (result.messages.isNotEmpty()) {
                nextFromMessageIdHistory = result.messages.last().id
            }
            result.messages.mapNotNull { extractVideoItem(it) }.forEach {
                videoCache[it.messageId] = it
            }
            updateVideosList()
        }
    }
}
```

### Automatic Thumbnail Fetching
When extracting `VideoItem`, check if the thumbnail is already downloaded. If not, issue a low-priority download:
```kotlin
val thumb = video.thumbnail?.file
val thumbPath = if (thumb?.local?.isDownloadingCompleted == true) thumb.local.path else null
if (thumbPath == null && thumb != null && thumb.local?.isDownloadingActive == false) {
    client?.send(TdApi.DownloadFile(thumb.id, 1, 0, 0, false), null)
}
```

---

## 8. Progressive Chunk Streaming with Media3 / ExoPlayer

This is the key breakthrough: **Streaming without downloading the entire multi-gigabyte file first.**

### Implementing `TelegramDataSource`:
Extend `BaseDataSource` and implement chunk reading directly from TDLib's memory buffer:

```kotlin
@OptIn(UnstableApi::class)
class TelegramDataSource(
    private val repository: TelegramRepository,
    private val fileId: Int,
    private val totalSize: Long,
    private val fileName: String
) : BaseDataSource(/* isNetwork = */ true) {

    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var localRaf: RandomAccessFile? = null
    private val lock = Any()

    private val updateListener: () -> Unit = {
        synchronized(lock) {
            (lock as java.lang.Object).notifyAll()
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        currentPosition = dataSpec.position
        bytesRemaining = if (totalSize > 0) totalSize - currentPosition else C.LENGTH_UNSET.toLong()

        // 1. If file was already downloaded to disk, stream directly from disk
        val localFile = repository.getLocalFileIfDownloaded(fileId)
        if (localFile != null && localFile.exists()) {
            localRaf = RandomAccessFile(localFile, "r").apply { seek(currentPosition) }
        } else {
            // 2. Register listener and trigger TDLib download from offset with top priority (32)
            repository.registerFileUpdateListener(fileId, updateListener)
            repository.downloadVideoFromOffset(fileId, currentPosition, 0L, 32)
        }

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // A. Read from local disk if available
        localRaf?.let { raf ->
            val toRead = minOf(length.toLong(), bytesRemaining).toInt()
            val bytes = raf.read(buffer, offset, toRead)
            if (bytes == -1) return C.RESULT_END_OF_INPUT
            currentPosition += bytes
            bytesRemaining -= bytes
            return bytes
        }

        // B. Wait for TDLib to download bytes at currentPosition
        var prefix = runBlocking { repository.getDownloadedPrefixSize(fileId, currentPosition) }
        val startTime = System.currentTimeMillis()

        while (prefix <= 0L && (System.currentTimeMillis() - startTime) < 15000L) {
            repository.downloadVideoFromOffset(fileId, currentPosition, 0L, 32)
            synchronized(lock) {
                (lock as java.lang.Object).wait(100)
            }
            prefix = runBlocking { repository.getDownloadedPrefixSize(fileId, currentPosition) }
        }

        // C. Fetch chunk (up to 256KB) via ReadFilePart
        val toFetch = minOf(prefix, 262144L)
        val chunk = runBlocking { repository.readFilePart(fileId, currentPosition, toFetch) }
            ?: throw IOException("Failed to read chunk at $currentPosition")

        val toCopy = minOf(length, chunk.size)
        System.arraycopy(chunk, 0, buffer, offset, toCopy)
        currentPosition += toCopy
        bytesRemaining -= toCopy
        return toCopy
    }

    override fun close() {
        repository.unregisterFileUpdateListener(fileId, updateListener)
        localRaf?.close()
        localRaf = null
    }
}
```

### Hooking into ExoPlayer:
```kotlin
val dataSourceFactory = DataSource.Factory {
    TelegramDataSource(repository, videoItem.fileId, videoItem.fileSize, videoItem.fileName)
}

val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
    .createMediaSource(MediaItem.fromUri(Uri.parse("telegram://${videoItem.fileId}")))

exoPlayer.setMediaSource(mediaSource)
exoPlayer.prepare()
exoPlayer.playWhenReady = true
```

---

## 9. Background Downloads & File Management

### Starting a Download:
```kotlin
fun downloadVideo(fileId: Int) {
    // Priority 32 = active foreground download
    client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false)) { result ->
        if (result is TdApi.Error) Log.e("Download", result.message)
    }
}
```

### Listening to Progress via `UpdateFile`:
```kotlin
private fun handleFileUpdate(file: TdApi.File) {
    // Awaken any waiting streaming threads
    fileUpdateListeners[file.id]?.forEach { it.invoke() }

    val isDownloaded = file.local?.isDownloadingCompleted == true
    val downloadedBytes = file.local?.downloadedSize ?: 0L
    val totalBytes = if (file.expectedSize > 0) file.expectedSize else file.size
    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    val path = if (isDownloaded) file.local?.path else null

    // Update your StateFlow / ViewModel
    updateItemDownloadState(file.id, progress, isDownloaded, path)
}
```

### Cancelling a Download:
```kotlin
fun cancelDownload(fileId: Int) {
    client?.send(TdApi.CancelDownloadFile(fileId, false), null)
}
```

---

## 10. Coroutine Helper for Synchronous TDLib Calls

Convert asynchronous TDLib callback queries into Kotlin suspending functions:

```kotlin
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
```

---

## 11. Production Best Practices & Pitfalls

| Scenario | Problem | Solution |
|---|---|---|
| **JNI Native Libraries** | Missing `.so` files at runtime | Set `packaging.jniLibs.useLegacyPackaging = true` in `build.gradle.kts`. |
| **Flood Waits** | TDLib returns `420 FLOOD_WAIT_X` | Do not hammer the API in tight loops. Implement exponential backoff when fetching batches. |
| **Large Storage Footprint** | TDLib caches files and DB indefinitely | Clear cache using `TdApi.OptimizeStorage` or delete directory on explicit user request. |
| **Multi-Thread Callback** | `onResult` is called on a TDLib background thread | Use `ConcurrentHashMap` for caches and update UI states on `Dispatchers.Main` or StateFlow. |
| **Seeking in Videos** | Seeking triggers requests at arbitrary byte offsets | ExoPlayer's `DataSpec.position` handles seeking seamlessly via `downloadVideoFromOffset(fileId, offset, 0L, 32)`. |
| **App Termination / Logout** | Orphaned sessions or locked DB | Always call `TdApi.LogOut()` or `TdApi.Close()` before disposing of the `Client` instance. |
