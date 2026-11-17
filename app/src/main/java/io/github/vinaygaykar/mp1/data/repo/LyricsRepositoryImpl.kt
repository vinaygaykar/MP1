package io.github.vinaygaykar.mp1.data.repo

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaygaykar.mp1.data.network.LrcLibService
import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.LyricsHttpException
import io.github.vinaygaykar.mp1.domain.repository.LyricsNetworkException
import io.github.vinaygaykar.mp1.domain.repository.LyricsNotFoundException
import io.github.vinaygaykar.mp1.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class LyricsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lrcLibService: LrcLibService
) : LyricsRepository {

    override suspend fun getLyrics(song: Song): Result<Lyrics> = withContext(Dispatchers.IO) {
        val localLyrics = readLocalOrEmbeddedLyrics(song)
        if (localLyrics != null && (localLyrics.content.isNotBlank() || localLyrics.lines.isNotEmpty())) {
            return@withContext Result.success(localLyrics)
        }

        return@withContext fetchFromLrcLib(song)
    }

    private suspend fun fetchFromLrcLib(song: Song): Result<Lyrics> {
        return try {
            val duration = if (song.durationMs > 0) (song.durationMs / 1000).toInt() else null
            val response = lrcLibService.getLyrics(
                artistName = song.artist,
                trackName = song.title,
                albumName = song.album.takeIf { it.isNotBlank() && it != "Unknown Album" },
                duration = duration
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val content = body.syncedLyrics ?: body.plainLyrics
                    if (content != null) {
                        val isSynced = body.syncedLyrics != null
                        Result.success(LyricsParser.parse(content, isSynced))
                    } else if (body.instrumental == true) {
                        val instrumentalText = "[Instrumental]"
                        Result.success(Lyrics(content = instrumentalText, isSynced = false))
                    } else {
                        Result.failure(LyricsNotFoundException())
                    }
                } else {
                    Result.failure(LyricsNotFoundException())
                }
            } else if (response.code() == 404) {
                Result.failure(LyricsNotFoundException())
            } else {
                Result.failure(LyricsHttpException(response.code()))
            }
        } catch (e: Exception) {
            if (e is LyricsNotFoundException || e is LyricsHttpException) {
                Result.failure(e)
            } else {
                Result.failure(LyricsNetworkException())
            }
        }
    }

    private suspend fun readLocalOrEmbeddedLyrics(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val songUriString = song.uri.toString()
        val fileName = song.fileName
        val uri = try { Uri.parse(songUriString) } catch (_: Exception) { null } ?: return@withContext null

        val lrcFileName = if (fileName.contains(".")) {
            fileName.substringBeforeLast(".") + ".lrc"
        } else {
            "$fileName.lrc"
        }

        var localContent: String? = null

        try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                val parent = file.parentFile
                if (parent != null) {
                    val lrcFile = File(parent, lrcFileName)
                    if (lrcFile.exists()) {
                        localContent = lrcFile.readText()
                    }
                }
            } else if (uri.scheme == "content") {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val lrcDocId = if (docId.contains(".")) {
                        docId.substringBeforeLast(".") + ".lrc"
                    } else {
                        "$docId.lrc"
                    }
                    val lrcUri = DocumentsContract.buildDocumentUriUsingTree(uri, lrcDocId)
                    localContent = context.contentResolver.openInputStream(lrcUri)?.use { input ->
                        input.bufferedReader().readText()
                    }
                }

                if (localContent.isNullOrBlank()) {
                    val projection = arrayOf(MediaStore.Audio.Media.DATA)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                            if (dataIdx != -1) {
                                val path = cursor.getString(dataIdx)
                                if (!path.isNullOrBlank()) {
                                    val audioFile = File(path)
                                    val parent = audioFile.parentFile
                                    if (parent != null && parent.exists()) {
                                        val lrcFile = File(parent, lrcFileName)
                                        if (lrcFile.exists()) {
                                            localContent = lrcFile.readText()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (localContent.isNullOrBlank()) {
            try {
                val retriever = MediaMetadataRetriever()
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val embedded = retriever.extractMetadata(35) // METADATA_KEY_LYRICS
                        if (!embedded.isNullOrBlank()) {
                            localContent = embedded
                        }
                    }
                }
                retriever.release()
            } catch (_: Exception) {}
        }

        if (!localContent.isNullOrBlank()) {
            val isSynced = localContent!!.contains(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]"))
            return@withContext LyricsParser.parse(localContent!!, isSynced)
        }

        return@withContext null
    }

    override suspend fun searchLyrics(query: String, songId: String): Result<Lyrics> {
        return try {
            val response = lrcLibService.searchLyrics(query)
            if (response.isSuccessful) {
                val bodyList = response.body()
                if (!bodyList.isNullOrEmpty()) {
                    val body = bodyList.first()
                    val content = body.syncedLyrics ?: body.plainLyrics
                    if (content != null) {
                        val isSynced = body.syncedLyrics != null
                        Result.success(LyricsParser.parse(content, isSynced))
                    } else if (body.instrumental == true) {
                        val instrumentalText = "[Instrumental]"
                        Result.success(Lyrics(content = instrumentalText, isSynced = false))
                    } else {
                        Result.failure(LyricsNotFoundException())
                    }
                } else {
                    Result.failure(LyricsNotFoundException())
                }
            } else if (response.code() == 404) {
                Result.failure(LyricsNotFoundException())
            } else {
                Result.failure(LyricsHttpException(response.code()))
            }
        } catch (e: Exception) {
            if (e is LyricsNotFoundException || e is LyricsHttpException) {
                Result.failure(e)
            } else {
                Result.failure(LyricsNetworkException())
            }
        }
    }

    override suspend fun isLocalOrEmbeddedLyrics(song: Song): Boolean = withContext(Dispatchers.IO) {
        return@withContext readLocalOrEmbeddedLyrics(song) != null
    }

    override suspend fun saveLyrics(song: Song, lyricsContent: String): Boolean = withContext(Dispatchers.IO) {
        saveLrcFileToStorage(context, song.uri.toString(), song.fileName, lyricsContent)
    }

    private fun saveLrcFileToStorage(context: Context, songUriString: String, fileName: String, lyricsContent: String): Boolean {
        return try {
            val uri = Uri.parse(songUriString)
            val lrcFileName = if (fileName.contains(".")) {
                fileName.substringBeforeLast(".") + ".lrc"
            } else {
                "$fileName.lrc"
            }

            if (uri.scheme == "file") {
                val file = File(uri.path ?: return false)
                val parent = file.parentFile ?: return false
                val lrcFile = File(parent, lrcFileName)
                lrcFile.writeText(lyricsContent)
                true
            } else if (uri.scheme == "content") {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val parentDocId = if (docId.contains("/")) docId.substringBeforeLast("/") else null
                    if (parentDocId != null) {
                        val treeUri = DocumentsContract.buildDocumentUriUsingTree(uri, parentDocId)
                        val parentFolderUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
                        val newLrcUri = DocumentsContract.createDocument(
                            context.contentResolver,
                            parentFolderUri,
                            "*/*",
                            lrcFileName
                        )
                        if (newLrcUri != null) {
                            context.contentResolver.openOutputStream(newLrcUri)?.use { out ->
                                out.write(lyricsContent.toByteArray())
                            }
                            return true
                        }
                    }
                }
                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (dataIdx != -1) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrBlank()) {
                                val audioFile = File(path)
                                val parent = audioFile.parentFile
                                if (parent != null && parent.exists()) {
                                    val lrcFile = File(parent, lrcFileName)
                                    lrcFile.writeText(lyricsContent)
                                    return true
                                }
                            }
                        }
                    }
                }
                false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
