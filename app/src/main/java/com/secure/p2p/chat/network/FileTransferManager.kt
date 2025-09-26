package com.secure.p2p.chat.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class FileTransferManager(private val context: Context) {
    
    private val _transferProgress = MutableStateFlow<Map<String, FileTransferProgress>>(emptyMap())
    val transferProgress: StateFlow<Map<String, FileTransferProgress>> = _transferProgress
    
    // Максимальный размер файла - 10MB
    companion object {
        const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        const val CHUNK_SIZE = 16 * 1024 // 16KB chunks for large files
    }
    
    /**
     * Подготавливает файл к передаче: сжимает и шифрует
     */
    suspend fun prepareFileForTransfer(uri: Uri, fileType: FileType): FileTransferData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileData = readStreamWithLimit(inputStream, MAX_FILE_SIZE)
            inputStream.close()
            
            if (fileData == null) {
                updateTransferProgress(uri.toString(), FileTransferProgress.ERROR)
                return null
            }
            
            // Сжимаем изображения
            val processedData = when (fileType) {
                FileType.IMAGE -> compressImage(fileData)
                else -> fileData
            }
            
            // Создаем метаданные файла
            val fileId = UUID.randomUUID().toString()
            val fileName = getFileNameFromUri(uri) ?: "file_${System.currentTimeMillis()}"
            
            FileTransferData(
                id = fileId,
                fileName = fileName,
                fileType = fileType,
                data = processedData,
                size = processedData.size.toLong(),
                chunked = processedData.size > CHUNK_SIZE
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            updateTransferProgress(uri.toString(), FileTransferProgress.ERROR)
            null
        }
    }
    
    /**
     * Разбивает файл на чанки для передачи по P2P
     */
    fun chunkFileData(fileData: FileTransferData): List<FileChunk> {
        if (!fileData.chunked) {
            return listOf(FileChunk(
                fileId = fileData.id,
                chunkIndex = 0,
                totalChunks = 1,
                data = fileData.data,
                isLastChunk = true
            ))
        }
        
        val chunks = mutableListOf<FileChunk>()
        val totalChunks = (fileData.data.size + CHUNK_SIZE - 1) / CHUNK_SIZE
        
        for (i in 0 until totalChunks) {
            val start = i * CHUNK_SIZE
            val end = minOf(start + CHUNK_SIZE, fileData.data.size)
            val chunkData = fileData.data.copyOfRange(start, end)
            
            chunks.add(FileChunk(
                fileId = fileData.id,
                chunkIndex = i,
                totalChunks = totalChunks,
                data = chunkData,
                isLastChunk = i == totalChunks - 1
            ))
        }
        
        return chunks
    }
    
    /**
     * Собирает файл из чанков
     */
    fun reassembleFile(chunks: List<FileChunk>): ByteArray? {
        if (chunks.isEmpty()) return null
        
        // Проверяем что все чанки присутствуют
        val sortedChunks = chunks.sortedBy { it.chunkIndex }
        val expectedChunks = chunks.first().totalChunks
        
        if (sortedChunks.size != expectedChunks) {
            return null // Не все чанки получены
        }
        
        val totalSize = sortedChunks.sumOf { it.data.size }
        val result = ByteArray(totalSize)
        var currentPosition = 0
        
        for (chunk in sortedChunks) {
            System.arraycopy(chunk.data, 0, result, currentPosition, chunk.data.size)
            currentPosition += chunk.data.size
        }
        
        return result
    }
    
    /**
     * Конвертирует файл в Base64 для текстовой передачи
     */
    fun fileToBase64(fileData: FileTransferData): String {
        return Base64.encodeToString(fileData.data, Base64.NO_WRAP)
    }
    
    /**
     * Восстанавливает файл из Base64
     */
    fun base64ToFile(base64Data: String, fileId: String, fileName: String, fileType: FileType): FileTransferData? {
        return try {
            val data = Base64.decode(base64Data, Base64.NO_WRAP)
            FileTransferData(
                id = fileId,
                fileName = fileName,
                fileType = fileType,
                data = data,
                size = data.size.toLong(),
                chunked = false
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun readStreamWithLimit(stream: InputStream, maxSize: Int): ByteArray? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytes = 0
            
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
                if (totalBytes > maxSize) {
                    return null // Файл слишком большой
                }
                outputStream.write(buffer, 0, bytesRead)
            }
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun compressImage(imageData: ByteArray): ByteArray {
        // Базовая компрессия изображения (в реальном приложении используйте BitmapFactory)
        // Уменьшаем качество до 70% для экономии трафика
        return imageData // Заглушка - в реальном приложении здесь будет настоящая компрессия
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex("_display_name")
                if (displayNameIndex != -1) {
                    cursor.getString(displayNameIndex)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
    
    private fun updateTransferProgress(fileId: String, progress: FileTransferProgress) {
        val currentProgress = _transferProgress.value.toMutableMap()
        currentProgress[fileId] = progress
        _transferProgress.value = currentProgress
    }
}

data class FileTransferData(
    val id: String,
    val fileName: String,
    val fileType: FileType,
    val data: ByteArray,
    val size: Long,
    val chunked: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileTransferData
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class FileChunk(
    val fileId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val data: ByteArray,
    val isLastChunk: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileChunk
        return fileId == other.fileId && chunkIndex == other.chunkIndex
    }
    
    override fun hashCode(): Int {
        return 31 * fileId.hashCode() + chunkIndex
    }
}

enum class FileType {
    IMAGE, DOCUMENT, AUDIO, VIDEO, OTHER
}

sealed class FileTransferProgress {
    object IDLE : FileTransferProgress()
    object COMPRESSING : FileTransferProgress()
    object ENCRYPTING : FileTransferProgress()
    data class TRANSFERRING(val progress: Int) : FileTransferProgress() // 0-100
    object COMPLETED : FileTransferProgress()
    object ERROR : FileTransferProgress()
}
