package io.github.vinaygaykar.mp1.data.local.scanner

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.vinaygaykar.mp1.data.local.MusicDatabase
import io.github.vinaygaykar.mp1.data.local.dao.AlbumDao
import io.github.vinaygaykar.mp1.data.local.dao.ArtistDao
import io.github.vinaygaykar.mp1.data.local.dao.GenreDao
import io.github.vinaygaykar.mp1.data.local.dao.ScanFolderDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanFolderDao: ScanFolderDao,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao
) {
    companion object {
        const val SCAN_WORK_TAG = "library_scan"
    }

    val isScanning: Flow<Boolean> = WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData(SCAN_WORK_TAG)
        .asFlow()
        .map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }

    val currentScanningFolder: Flow<String?> = WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData(SCAN_WORK_TAG)
        .asFlow()
        .map { workInfos ->
            val runningWork = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            runningWork?.progress?.getString("currentFolder")
        }

    fun startScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val scanRequest = OneTimeWorkRequestBuilder<LibraryScannerWorker>()
            .setConstraints(constraints)
            .addTag(SCAN_WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SCAN_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            scanRequest
        )
    }

    fun checkAndTriggerMigrationScan(currentDbVersion: Int = MusicDatabase.VERSION) {
        val prefs = context.getSharedPreferences("scanner_prefs", Context.MODE_PRIVATE)
        val lastScannedDbVersion = prefs.getInt("last_scanned_db_version", 0)

        if (lastScannedDbVersion < currentDbVersion) {
            startScan()
            prefs.edit().putInt("last_scanned_db_version", currentDbVersion).apply()
        }
    }

    suspend fun addScanFolder(uri: String, displayName: String) {
        val folder = ScanFolderEntity(
            id = UUID.randomUUID().toString(),
            treeUri = uri,
            displayName = displayName,
            dateAdded = System.currentTimeMillis(),
            lastScanned = null,
            isActive = true
        )
        scanFolderDao.insertFolder(folder)
        startScan()
    }

    suspend fun removeScanFolder(id: String) {
        scanFolderDao.deleteFolder(id)
        songDao.deleteSongsByFolder(id)
        albumDao.deleteOrphanedAlbums()
        artistDao.deleteOrphanedArtists()
        genreDao.deleteOrphanedGenres()
        albumDao.updateTrackCounts()
        artistDao.updateTrackCounts()
        genreDao.updateTrackCounts()
    }
}
