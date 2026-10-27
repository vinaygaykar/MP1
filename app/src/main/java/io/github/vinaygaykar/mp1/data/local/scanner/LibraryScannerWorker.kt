package io.github.vinaygaykar.mp1.data.local.scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.vinaygaykar.mp1.MainActivity
import io.github.vinaygaykar.mp1.R
import io.github.vinaygaykar.mp1.data.local.MusicDatabase
import io.github.vinaygaykar.mp1.data.local.dao.AlbumDao
import io.github.vinaygaykar.mp1.data.local.dao.ArtistDao
import io.github.vinaygaykar.mp1.data.local.dao.GenreDao
import io.github.vinaygaykar.mp1.data.local.dao.ScanFolderDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import io.github.vinaygaykar.mp1.data.local.entities.AlbumEntity
import io.github.vinaygaykar.mp1.data.local.entities.ArtistEntity
import io.github.vinaygaykar.mp1.data.local.entities.GenreEntity
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import io.github.vinaygaykar.mp1.data.local.entities.SongArtistCrossRef
import io.github.vinaygaykar.mp1.data.local.entities.SongEntity
import io.github.vinaygaykar.mp1.playback.PlaybackService
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.extractArtistsFromTitle
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import io.github.vinaygaykar.mp1.utils.normalizeMetadataText
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.LinkedList

@HiltWorker
class LibraryScannerWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: MusicDatabase,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val scanFolderDao: ScanFolderDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "LibraryScannerWorker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "library_scan_channel"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(currentFolder: String? = null): ForegroundInfo {
        createNotificationChannel()

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanFolderName = currentFolder
            ?.removePrefix("primary:")
            ?.removePrefix("primary")
            ?.trim()

        val contentText = if (!cleanFolderName.isNullOrBlank()) {
            "Scanning $cleanFolderName..."
        } else {
            "Indexing music files..."
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Scanning Music Library")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Library Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when scanning music library"
            }
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result {
        // Lower thread priority to background to prevent UI lag or playback stutters
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

        // Stop current playing song and clear queue before scanning
        stopPlaybackAndClearQueue()

        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start foreground notification for scanner", e)
        }

        Log.i(TAG, "Library scan started")
        val folders = scanFolderDao.getAllFolders().first().filter { it.isActive }

        if (folders.isEmpty()) {
            Log.w(TAG, "No active scan folders found")
            database.withTransaction {
                songDao.deleteAllSongs()
                albumDao.deleteAll()
                artistDao.deleteAll()
                genreDao.deleteAll()
            }
            return Result.success()
        }

        folders.forEach { folder ->
            try {
                setProgress(workDataOf("currentFolder" to folder.displayName))
                try {
                    setForeground(createForegroundInfo(folder.displayName))
                } catch (_: Exception) {}
                val rootUri = Uri.parse(folder.treeUri)
                val documentId = try {
                    DocumentsContract.getTreeDocumentId(rootUri)
                } catch (e: Exception) {
                    ""
                }

                if (documentId.startsWith("primary:")) {
                    val relativePath = documentId.removePrefix("primary:").trim('/')
                    Log.i(TAG, "Scanning folder '${folder.displayName}' (Path: '$relativePath') [Mode: MediaStore]")
                    val scannedWithMediaStore = scanFolderViaMediaStore(folder, relativePath)
                    if (scannedWithMediaStore) {
                        return@forEach
                    }
                }

                Log.i(TAG, "Scanning folder '${folder.displayName}' (URI: ${folder.treeUri}) [Mode: Storage Access Framework (SAF)]")
                scanFolder(folder)
            } catch (e: Exception) {
                Log.e(TAG, "Critical error scanning folder: ${folder.treeUri}", e)
            }
        }

        // Remove orphaned metadata and recalculate track counts in a single Room transaction
        database.withTransaction {
            albumDao.deleteOrphanedAlbums()
            artistDao.deleteOrphanedArtists()
            genreDao.deleteOrphanedGenres()

            albumDao.updateTrackCounts()
            artistDao.updateTrackCounts()
            genreDao.updateTrackCounts()
        }

        Log.i(TAG, "Library scan finished")
        return Result.success()
    }

    private suspend fun stopPlaybackAndClearQueue() {
        try {
            withContext(Dispatchers.Main) {
                try {
                    val sessionToken = SessionToken(appContext,
                        ComponentName(appContext, PlaybackService::class.java)
                    )
                    val hints = android.os.Bundle().apply { putBoolean("NEEDS_TIMELINE", false) }
                    val controller = MediaController.Builder(appContext, sessionToken)
                        .setConnectionHints(hints)
                        .buildAsync().await()
                    controller.stop()
                    controller.clearMediaItems()
                    controller.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop MediaController playback and clear media items", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error switching to Main dispatcher for MediaController", e)
        }

        try {
            database.queueDao().clearQueue()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear database queue", e)
        }
    }

    private suspend fun scanFolderViaMediaStore(folder: ScanFolderEntity, relativePath: String): Boolean {
        val existingSongsMap = songDao.getSongsByFolderSync(folder.id).associateBy { it.id }
        val foundSongIds = mutableSetOf<String>()
        val songEntities = mutableListOf<SongEntity>()
        val songArtistCrossRefs = mutableListOf<SongArtistCrossRef>()
        val albumEntities = mutableMapOf<String, AlbumEntity>()
        val artistEntities = mutableMapOf<String, ArtistEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/$relativePath/%")

        var totalCount = 0
        var skippedUnchanged = 0
        var lastReportedSubfolder: String? = null

        try {
            appContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.count == 0) return false

                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    totalCount++
                    val filePath = cursor.getString(dataCol) ?: ""
                    val currentSubfolder = filePath.substringBeforeLast("/")
                        .removePrefix("/storage/emulated/0/")
                        .removePrefix("primary:")
                        .trim('/')
                    if (currentSubfolder.isNotEmpty() && (currentSubfolder != lastReportedSubfolder)) {
                        lastReportedSubfolder = currentSubfolder
                        setProgress(workDataOf("currentFolder" to currentSubfolder))
                        try {
                            setForeground(createForegroundInfo(currentSubfolder))
                        } catch (_: Exception) {}
                    }

                    val msId = cursor.getLong(idCol)
                    val mediaStoreUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, msId)
                    val songId = mediaStoreUri.toString().toHash()
                    foundSongIds.add(songId)

                    val lastModified = cursor.getLong(modCol)
                    val fileSize = cursor.getLong(sizeCol)
                    val existingSong = existingSongsMap[songId]

                    if (existingSong != null &&
                        existingSong.lastModified == lastModified &&
                        existingSong.fileSize == fileSize &&
                        isArtworkCacheValid(existingSong.artworkCachePath)
                    ) {
                        skippedUnchanged++
                        val extractedArtists = collectArtistNames(rawArtistName = existingSong.trackArtist, title = existingSong.title)
                        extractedArtists.forEach { name ->
                            val artistId = name.toHash()
                            val art = ArtistEntity(artistId, name, 0)
                            artistEntities[art.id] = art
                            songArtistCrossRefs.add(SongArtistCrossRef(songId = songId, artistId = artistId))
                        }
                        continue
                    }

                    val rawTitle = cursor.getString(titleCol).normalizeMetadataText() ?: "Unknown Title"
                    val rawArtist = cursor.getString(artistCol).normalizeMetadataText() ?: "Unknown Artist"
                    val albumName = cursor.getString(albumCol).normalizeMetadataText() ?: "Unknown Album"
                    val msAlbumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val trackNumber = cursor.getInt(trackCol).takeIf { it > 0 }
                    val year = cursor.getInt(yearCol).takeIf { it > 0 }
                    val fileName = filePath.substringAfterLast("/")
                    val mimeType = cursor.getString(mimeCol) ?: "audio/*"

                    val extractedArtists = collectArtistNames(rawArtistName = rawArtist, title = rawTitle)
                    val primaryArtistName = extractedArtists.firstOrNull() ?: rawArtist
                    val primaryArtistId = primaryArtistName.toHash()

                    var rawAlbumArtist: String? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val albumArtistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                        if (albumArtistCol != -1) {
                            rawAlbumArtist = cursor.getString(albumArtistCol).normalizeMetadataText()
                        }
                    }

                    // Artwork handling: Try embedded picture first, fallback to MediaStore system album art URI
                    var artworkPath: String? = null
                    val retriever = MediaMetadataRetriever()
                    try {
                        appContext.contentResolver.openFileDescriptor(mediaStoreUri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                            if (rawAlbumArtist.isNullOrBlank()) {
                                rawAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                                    ?.normalizeMetadataText()
                            }
                            val effectiveAlbumArtist = rawAlbumArtist?.takeIf { it.isNotBlank() } ?: primaryArtistName
                            val albumId = (albumName + effectiveAlbumArtist).toHash()

                            artworkPath = extractAndCacheArtwork(retriever, albumId)
                        }
                    } catch (_: Exception) {
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }

                    val effectiveAlbumArtist = rawAlbumArtist?.takeIf { it.isNotBlank() } ?: primaryArtistName
                    val albumId = (albumName + effectiveAlbumArtist).toHash()

                    if (artworkPath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        artworkPath = tryExtractMediaStoreThumbnail(mediaStoreUri, albumId)
                    } else if (artworkPath == null && msAlbumId > 0) {
                        artworkPath = "content://media/external/audio/albumart/$msAlbumId"
                    }

                    val album = AlbumEntity(albumId, albumName, effectiveAlbumArtist, year, artworkPath, 0)
                    albumEntities[albumId] = album

                    extractedArtists.forEach { name ->
                        val artistId = name.toHash()
                        val art = ArtistEntity(artistId, name, 0)
                        artistEntities[art.id] = art
                        songArtistCrossRefs.add(SongArtistCrossRef(songId = songId, artistId = artistId))
                    }

                    val song = SongEntity(
                        id = songId,
                        uri = mediaStoreUri.toString(),
                        fileName = fileName,
                        relativePath = relativePath,
                        scanFolderId = folder.id,
                        title = rawTitle,
                        trackArtist = rawArtist,
                        albumName = albumName,
                        artistName = primaryArtistName,
                        artistId = primaryArtistId,
                        albumId = albumId,
                        genreId = null,
                        trackNumber = trackNumber,
                        discNumber = null,
                        year = year,
                        durationMs = duration,
                        dateAdded = System.currentTimeMillis(),
                        lastModified = lastModified,
                        fileSize = fileSize,
                        artworkCachePath = artworkPath,
                        mimeType = mimeType,
                        artistsDisplayString = extractedArtists.joinArtistsForDisplay()
                    )
                    songEntities.add(song)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query failed for relativePath '$relativePath', falling back to SAF", e)
            return false
        }

        if (totalCount == 0) {
            return false
        }

        database.withTransaction {
            if (artistEntities.isNotEmpty()) artistDao.insertArtists(artistEntities.values.toList())
            if (albumEntities.isNotEmpty()) albumDao.insertAlbums(albumEntities.values.toList())
            if (songEntities.isNotEmpty()) songDao.insertSongs(songEntities)
            if (songArtistCrossRefs.isNotEmpty()) songDao.insertSongArtistCrossRefs(songArtistCrossRefs)
        }

        val deletedSongIds = existingSongsMap.keys - foundSongIds
        if (deletedSongIds.isNotEmpty()) {
            database.withTransaction {
                songDao.deleteSongsByIds(deletedSongIds.toList())
            }
        }

        scanFolderDao.updateFolder(folder.copy(lastScanned = System.currentTimeMillis()))
        Log.i(
            TAG,
            "Folder '${folder.displayName}' scan complete [Mode: MediaStore]. Total: $totalCount, Unchanged: $skippedUnchanged, New/Updated: ${songEntities.size}"
        )
        return true
    }

    private suspend fun scanFolder(folder: ScanFolderEntity) {
        val rootUri = Uri.parse(folder.treeUri)
        val documentId = DocumentsContract.getTreeDocumentId(rootUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, documentId)

        val existingSongsMap = songDao.getSongsByFolderSync(folder.id).associateBy { it.id }
        val foundSongIds = mutableSetOf<String>()

        val queue = LinkedList<Uri>()
        queue.add(childrenUri)

        val tasksToProcess = mutableListOf<AudioTask>()

        val existingArtistEntities = mutableMapOf<String, ArtistEntity>()
        val existingSongArtistCrossRefs = mutableListOf<SongArtistCrossRef>()

        var totalFilesProcessed = 0
        var audioFilesFound = 0
        var skippedUnchanged = 0

        while (queue.isNotEmpty()) {
            val currentUri = queue.removeFirst()
            val children = mutableListOf<DocumentInfo>()

            appContext.contentResolver.query(
                currentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null, null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    children.add(
                        DocumentInfo(
                            docId = cursor.getString(idIdx),
                            mimeType = cursor.getString(mimeIdx) ?: "",
                            displayName = cursor.getString(nameIdx) ?: "Unknown",
                            lastModified = cursor.getLong(modIdx),
                            fileSize = cursor.getLong(sizeIdx)
                        )
                    )
                }
            }

            // O(1) sidecar lyric document indexing per directory pass
            val lyricsMap = children
                .filter { isLyricDoc(it.displayName) }
                .associateBy { it.displayName.substringBeforeLast(".") }

            var lastReportedSubfolder: String? = null

            children.forEach { doc ->
                totalFilesProcessed++
                val docUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, doc.docId)

                if (doc.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val subfolder = doc.docId.removePrefix("primary:").trim('/')
                    if (subfolder.isNotEmpty() && subfolder != lastReportedSubfolder) {
                        lastReportedSubfolder = subfolder
                        setProgress(workDataOf("currentFolder" to subfolder))
                        try {
                            setForeground(createForegroundInfo(subfolder))
                        } catch (_: Exception) {}
                    }
                    queue.add(DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, doc.docId))
                } else if (isAudioFile(doc.mimeType, doc.displayName)) {
                    val subfolder = doc.docId.substringBeforeLast("/", "").removePrefix("primary:").trim('/')
                    if (subfolder.isNotEmpty() && subfolder != lastReportedSubfolder) {
                        lastReportedSubfolder = subfolder
                        setProgress(workDataOf("currentFolder" to subfolder))
                        try {
                            setForeground(createForegroundInfo(subfolder))
                        } catch (_: Exception) {}
                    }
                    audioFilesFound++
                    val songId = docUri.toString().toHash()
                    foundSongIds.add(songId)

                    val existingSong = existingSongsMap[songId]
                    if (existingSong != null &&
                        existingSong.lastModified == doc.lastModified &&
                        existingSong.fileSize == doc.fileSize &&
                        isArtworkCacheValid(existingSong.artworkCachePath)
                    ) {
                        skippedUnchanged++
                        val extractedArtists = collectArtistNames(rawArtistName = existingSong.trackArtist, title = existingSong.title)
                        extractedArtists.forEach { name ->
                            val artistId = name.toHash()
                            val art = ArtistEntity(artistId, name, 0)
                            existingArtistEntities[art.id] = art
                            existingSongArtistCrossRefs.add(SongArtistCrossRef(songId = songId, artistId = artistId))
                        }
                    } else {
                        val baseName = doc.displayName.substringBeforeLast(".")
                        val lyricDoc = lyricsMap[baseName]
                        tasksToProcess.add(AudioTask(docUri, doc, rootUri, lyricDoc))
                    }
                }
            }
        }

        Log.i(
            TAG,
            "Folder '${folder.displayName}' scan complete [Mode: SAF]. Total files: $totalFilesProcessed, Audio files: $audioFilesFound, Unchanged: $skippedUnchanged, New/Modified: ${tasksToProcess.size}"
        )

        val results = if (tasksToProcess.isNotEmpty()) {
            val maxParallelism = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
            val semaphore = Semaphore(maxParallelism)
            coroutineScope {
                tasksToProcess.map { task ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val sidecarLyrics = task.lyricDoc?.let { readLyricContent(task.rootUri, it) }
                            extractMetadata(
                                uri = task.uri,
                                docId = task.doc.docId,
                                folderId = folder.id,
                                fileName = task.doc.displayName,
                                lastModified = task.doc.lastModified,
                                fileSize = task.doc.fileSize,
                                sidecarLyrics = sidecarLyrics
                            )
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } else emptyList()

        val newSongsToInsert = results.map { it.song }
        val newCrossRefsToInsert = results.flatMap { res ->
            res.artists.map { artist ->
                SongArtistCrossRef(songId = res.song.id, artistId = artist.id)
            }
        }
        val albumsToInsert = results.mapNotNull { it.album }.distinctBy { it.id }
        val newArtistsToInsert = results.flatMap { it.artists }.distinctBy { it.id }
        val genresToInsert = results.mapNotNull { it.genre }.distinctBy { it.id }

        val allArtistsToInsert = (existingArtistEntities.values + newArtistsToInsert).distinctBy { it.id }
        val allCrossRefsToInsert = (existingSongArtistCrossRefs + newCrossRefsToInsert).distinctBy { "${it.songId}_${it.artistId}" }

        if (allArtistsToInsert.isNotEmpty() || newSongsToInsert.isNotEmpty() || allCrossRefsToInsert.isNotEmpty()) {
            database.withTransaction {
                if (allArtistsToInsert.isNotEmpty()) artistDao.insertArtists(allArtistsToInsert)
                if (albumsToInsert.isNotEmpty()) albumDao.insertAlbums(albumsToInsert)
                if (genresToInsert.isNotEmpty()) genreDao.insertGenres(genresToInsert)
                if (newSongsToInsert.isNotEmpty()) songDao.insertSongs(newSongsToInsert)
                if (allCrossRefsToInsert.isNotEmpty()) songDao.insertSongArtistCrossRefs(allCrossRefsToInsert)
            }
            Log.d(TAG, "Batch updated scanner entities in SAF transaction")
        }

        // Delete songs that no longer exist on disk inside transaction
        val deletedSongIds = existingSongsMap.keys - foundSongIds
        if (deletedSongIds.isNotEmpty()) {
            database.withTransaction {
                songDao.deleteSongsByIds(deletedSongIds.toList())
            }
            Log.d(TAG, "Purged ${deletedSongIds.size} deleted songs from folder '${folder.displayName}'")
        }

        scanFolderDao.updateFolder(folder.copy(lastScanned = System.currentTimeMillis()))
    }

    private fun isAudioFile(mimeType: String, fileName: String): Boolean {
        return mimeType.startsWith("audio/") ||
                fileName.endsWith(".mp3", true) ||
                fileName.endsWith(".flac", true) ||
                fileName.endsWith(".m4a", true) ||
                fileName.endsWith(".wav", true) ||
                fileName.endsWith(".ogg", true) ||
                fileName.endsWith(".aac", true)
    }

    private fun isLyricDoc(fileName: String): Boolean {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return ext == "lrc" || ext == "ttml"
    }

    private fun extractMetadata(
        uri: Uri,
        docId: String,
        folderId: String,
        fileName: String,
        lastModified: Long,
        fileSize: Long,
        sidecarLyrics: LyricsData?
    ): MetadataResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            appContext.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)

                val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    .normalizeMetadataText() ?: fileName
                val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    .normalizeMetadataText() ?: "Unknown Artist"
                val rawAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    .normalizeMetadataText()
                val albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    .normalizeMetadataText() ?: "Unknown Album"
                val genreName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                    .normalizeMetadataText() ?: "Unknown Genre"
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val trackRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                val trackNumber = trackRaw?.parseTrackOrDiscNumber()
                val discRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                val discNumber = discRaw?.parseTrackOrDiscNumber()
                val year =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
                val mimeType =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/*"

                // Extract embedded lyrics if available (API 31+)
                var embeddedLyrics: String? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // MediaMetadataRetriever.METADATA_KEY_LYRICS = 35
                    embeddedLyrics = retriever.extractMetadata(35)
                }

                val lyrics = sidecarLyrics?.content ?: embeddedLyrics
                val isLyricsSynced = sidecarLyrics?.isSynced ?: false

                // Extract multi-artist metadata and title artists
                val (cleanedTitle, _) = rawTitle.extractArtistsFromTitle()
                val finalTitle = cleanedTitle.ifBlank { rawTitle }
                val extractedArtists = collectArtistNames(
                    rawArtistName = rawArtist,
                    title = rawTitle
                )

                val primaryArtistName = extractedArtists.firstOrNull() ?: rawArtist
                val primaryArtistId = primaryArtistName.toHash()

                val effectiveAlbumArtist = rawAlbumArtist?.takeIf { it.isNotBlank() } ?: primaryArtistName

                val albumId = (albumName + effectiveAlbumArtist).toHash()
                val genreId = genreName.toHash()
                val songId = uri.toString().toHash()

                val artworkPath = extractAndCacheArtwork(retriever, albumId)

                val album = AlbumEntity(albumId, albumName, effectiveAlbumArtist, year, artworkPath, 0)
                val genre = GenreEntity(genreId, genreName, 0)

                val artistEntitiesList = extractedArtists.map { name ->
                    ArtistEntity(name.toHash(), name, 0)
                }

                val song = SongEntity(
                    id = songId,
                    uri = uri.toString(),
                    fileName = fileName,
                    relativePath = docId,
                    scanFolderId = folderId,
                    title = finalTitle,
                    trackArtist = rawArtist,
                    albumName = albumName,
                    artistName = primaryArtistName,
                    artistId = primaryArtistId,
                    albumId = albumId,
                    genreId = genreId,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    year = year,
                    durationMs = duration,
                    dateAdded = System.currentTimeMillis(),
                    lastModified = lastModified,
                    fileSize = fileSize,
                    artworkCachePath = artworkPath,
                    mimeType = mimeType,
                    artistsDisplayString = extractedArtists.joinArtistsForDisplay()
                )

                MetadataResult(song, album, artistEntitiesList, genre)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract metadata for $fileName: ${e.message}")
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun readLyricContent(rootUri: Uri, lyricDoc: DocumentInfo): LyricsData? {
        return try {
            val lyricsUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, lyricDoc.docId)
            appContext.contentResolver.openInputStream(lyricsUri)?.use { input ->
                val content = input.bufferedReader().readText()
                val isSynced = lyricDoc.displayName.endsWith(".lrc", true) ||
                        content.contains(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}]"))
                LyricsData(content, isSynced)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read sidecar lyrics for ${lyricDoc.displayName}", e)
            null
        }
    }

    private fun extractAndCacheArtwork(retriever: MediaMetadataRetriever, albumId: String): String? {
        val artworkDir = File(appContext.filesDir, "artwork")
        if (!artworkDir.exists()) artworkDir.mkdirs()

        val artworkFile = File(artworkDir, "$albumId.jpg")
        if (artworkFile.exists() && artworkFile.length() > 0) {
            return artworkFile.absolutePath
        }

        val embedPic = retriever.embeddedPicture ?: return null
        return try {
            FileOutputStream(artworkFile).use { fos ->
                fos.write(embedPic)
            }
            artworkFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun tryExtractMediaStoreThumbnail(mediaStoreUri: Uri, albumId: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val artworkDir = File(appContext.filesDir, "artwork")
        if (!artworkDir.exists()) artworkDir.mkdirs()

        val artworkFile = File(artworkDir, "$albumId.jpg")
        if (artworkFile.exists() && artworkFile.length() > 0) {
            return artworkFile.absolutePath
        }

        return try {
            val bitmap = appContext.contentResolver.loadThumbnail(
                mediaStoreUri,
                Size(500, 500),
                null
            )
            FileOutputStream(artworkFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            artworkFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun isArtworkCacheValid(artworkCachePath: String?): Boolean {
        if (artworkCachePath.isNullOrBlank()) return false
        if (artworkCachePath.startsWith("content://") ||
            artworkCachePath.startsWith("http://") ||
            artworkCachePath.startsWith("https://")
        ) {
            return true
        }
        val path = if (artworkCachePath.startsWith("file://")) artworkCachePath.substring(7) else artworkCachePath
        val file = File(path)
        if (path.startsWith(appContext.cacheDir.absolutePath)) {
            return false
        }
        return file.exists() && file.length() > 0
    }

    private fun String.toHash(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun String.parseTrackOrDiscNumber(): Int? {
        if (isBlank()) return null
        val clean = substringBefore("/").trim()
        return clean.toIntOrNull()
    }

    data class MetadataResult(
        val song: SongEntity,
        val album: AlbumEntity?,
        val artists: List<ArtistEntity>,
        val genre: GenreEntity?
    )

    private data class AudioTask(
        val uri: Uri,
        val doc: DocumentInfo,
        val rootUri: Uri,
        val lyricDoc: DocumentInfo?
    )

    private data class DocumentInfo(
        val docId: String,
        val mimeType: String,
        val displayName: String,
        val lastModified: Long,
        val fileSize: Long
    )

    private data class LyricsData(
        val content: String,
        val isSynced: Boolean
    )
}
