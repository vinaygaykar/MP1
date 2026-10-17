package io.github.vinaygaykar.mp1.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinaygaykar.mp1.data.local.dao.ScanFolderDao
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import io.github.vinaygaykar.mp1.data.local.scanner.ScannerRepository
import io.github.vinaygaykar.mp1.data.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scannerRepository: ScannerRepository,
    private val scanFolderDao: ScanFolderDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    init {
        scannerRepository.checkAndTriggerMigrationScan()
    }

    val useCleanAlbumGrid: StateFlow<Boolean> = settingsRepository.useCleanAlbumGrid

    fun setUseCleanAlbumGrid(enabled: Boolean) {
        settingsRepository.setUseCleanAlbumGrid(enabled)
    }

    val playbackWave: StateFlow<String> = settingsRepository.playbackWave

    fun setPlaybackWave(wave: String) {
        settingsRepository.setPlaybackWave(wave)
    }

    val scanFolders: StateFlow<List<ScanFolderEntity>> = scanFolderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasScanFolders: StateFlow<Boolean?> = scanFolderDao.getAllFolders()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isScanning: StateFlow<Boolean> = scannerRepository.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentScanningFolder: StateFlow<String?> = scannerRepository.currentScanningFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addFolder(uri: String, displayName: String) {
        viewModelScope.launch {
            scannerRepository.addScanFolder(uri, displayName)
        }
    }

    fun removeFolder(id: String) {
        viewModelScope.launch {
            scannerRepository.removeScanFolder(id)
        }
    }

    fun startScan() {
        scannerRepository.startScan()
    }
}
