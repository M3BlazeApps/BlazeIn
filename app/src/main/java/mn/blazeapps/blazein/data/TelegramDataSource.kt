package mn.blazeapps.blazein.data

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

@OptIn(UnstableApi::class)
class TelegramDataSource(
    private val repository: TelegramRepository,
    private val fileId: Int,
    private val totalSize: Long,
    private val fileName: String
) : BaseDataSource(/* isNetwork = */ true) {

    private var currentUri: Uri? = null
    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var opened = false

    private var cachedChunk: ByteArray? = null
    private var cachedChunkOffset: Int = 0

    private var localRaf: RandomAccessFile? = null
    private val lock = Any()

    private val updateListener: () -> Unit = {
        synchronized(lock) {
            (lock as java.lang.Object).notifyAll()
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        transferInitializing(dataSpec)

        currentPosition = dataSpec.position
        val unset = C.LENGTH_UNSET.toLong()
        bytesRemaining = if (dataSpec.length != unset) {
            dataSpec.length
        } else {
            if (totalSize > 0) totalSize - currentPosition else unset
        }

        if (bytesRemaining != unset && bytesRemaining < 0) {
            throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        }

        opened = true
        cachedChunk = null
        cachedChunkOffset = 0

        // Check if local file is already fully downloaded
        val videoItem = repository.videos.value.find { it.fileId == fileId }
        if (videoItem?.isDownloaded == true && videoItem.localPath != null && File(videoItem.localPath).exists()) {
            try {
                localRaf = RandomAccessFile(File(videoItem.localPath), "r")
                localRaf?.seek(currentPosition)
            } catch (e: Exception) {
                Log.w(TAG, "Failed opening RandomAccessFile: ${e.message}")
                localRaf = null
            }
        }

        if (localRaf == null) {
            // Register file update listener to awaken thread when new chunks arrive from Telegram
            repository.registerFileUpdateListener(fileId, updateListener)
            // Trigger TDLib download from currentPosition with top priority (32)
            repository.downloadVideoFromOffset(fileId, currentPosition, 0L, 32)
        }

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val unset = C.LENGTH_UNSET.toLong()

        // 1. If we have a local completed file, read directly from disk
        localRaf?.let { raf ->
            val toRead = if (bytesRemaining != unset) {
                minOf(length.toLong(), bytesRemaining).toInt()
            } else {
                length
            }
            val readBytes = raf.read(buffer, offset, toRead)
            if (readBytes == -1) return C.RESULT_END_OF_INPUT
            currentPosition += readBytes
            if (bytesRemaining != unset) {
                bytesRemaining -= readBytes
            }
            bytesTransferred(readBytes)
            return readBytes
        }

        // 2. Consume any remaining bytes in our in-memory cached chunk
        val cached = cachedChunk
        if (cached != null && cachedChunkOffset < cached.size) {
            val available = cached.size - cachedChunkOffset
            val toCopy = minOf(length, available)
            System.arraycopy(cached, cachedChunkOffset, buffer, offset, toCopy)
            cachedChunkOffset += toCopy
            if (cachedChunkOffset >= cached.size) {
                cachedChunk = null
                cachedChunkOffset = 0
            }
            currentPosition += toCopy
            if (bytesRemaining != unset) {
                bytesRemaining -= toCopy
            }
            bytesTransferred(toCopy)
            return toCopy
        }

        // 3. Fetch next chunk from TDLib progressive stream
        val maxWaitMs = 15000L
        val startTime = System.currentTimeMillis()
        var prefix = runBlocking {
            withTimeoutOrNull(2000L) {
                repository.getDownloadedPrefixSize(fileId, currentPosition)
            } ?: 0L
        }

        // Wait until TDLib has downloaded bytes at currentPosition
        while (opened && prefix <= 0L && (System.currentTimeMillis() - startTime) < maxWaitMs) {
            repository.downloadVideoFromOffset(fileId, currentPosition, 0L, 32)
            synchronized(lock) {
                try {
                    (lock as java.lang.Object).wait(100)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return C.RESULT_END_OF_INPUT
                }
            }
            prefix = runBlocking {
                withTimeoutOrNull(2000L) {
                    repository.getDownloadedPrefixSize(fileId, currentPosition)
                } ?: 0L
            }
        }

        if (!opened) return C.RESULT_END_OF_INPUT

        if (prefix <= 0L) {
            // Check if download completed in the meantime
            val updatedItem = repository.videos.value.find { it.fileId == fileId }
            if (updatedItem?.isDownloaded == true && updatedItem.localPath != null && File(updatedItem.localPath).exists()) {
                try {
                    localRaf = RandomAccessFile(File(updatedItem.localPath), "r")
                    localRaf?.seek(currentPosition)
                    return read(buffer, offset, length)
                } catch (e: Exception) {
                    Log.w(TAG, "Error switching to completed local file: ${e.message}")
                }
            }
            throw IOException("Timeout waiting for Telegram video chunk at offset $currentPosition")
        }

        // Read up to 256 KB or available prefix
        val toFetch = minOf(prefix, 262144L)
        val chunk = runBlocking {
            withTimeoutOrNull(4000L) {
                repository.readFilePart(fileId, currentPosition, toFetch)
            }
        }

        if (chunk == null || chunk.isEmpty()) {
            // If download completed, try local file
            val item = repository.videos.value.find { it.fileId == fileId }
            if (item?.isDownloaded == true && item.localPath != null && File(item.localPath).exists()) {
                localRaf = RandomAccessFile(File(item.localPath), "r")
                localRaf?.seek(currentPosition)
                return read(buffer, offset, length)
            }
            throw IOException("Failed to read chunk from TDLib at offset $currentPosition")
        }

        val toCopy = minOf(length, chunk.size)
        System.arraycopy(chunk, 0, buffer, offset, toCopy)
        if (chunk.size > toCopy) {
            cachedChunk = chunk
            cachedChunkOffset = toCopy
        } else {
            cachedChunk = null
            cachedChunkOffset = 0
        }

        currentPosition += toCopy
        if (bytesRemaining != unset) {
            bytesRemaining -= toCopy
        }
        bytesTransferred(toCopy)
        return toCopy
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        if (opened) {
            opened = false
            repository.unregisterFileUpdateListener(fileId, updateListener)
            try {
                localRaf?.close()
            } catch (_: Exception) {}
            localRaf = null
            cachedChunk = null
            cachedChunkOffset = 0
            transferEnded()
        }
    }

    companion object {
        private const val TAG = "TelegramDataSource"
    }
}

@OptIn(UnstableApi::class)
class TelegramDataSourceFactory(
    private val repository: TelegramRepository,
    private val fileId: Int,
    private val totalSize: Long,
    private val fileName: String
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return TelegramDataSource(repository, fileId, totalSize, fileName)
    }
}
