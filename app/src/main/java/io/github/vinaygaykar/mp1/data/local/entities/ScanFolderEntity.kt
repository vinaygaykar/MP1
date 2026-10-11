package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_folders")
data class ScanFolderEntity(
    @PrimaryKey val id: String,
    val treeUri: String,
    val displayName: String,
    val dateAdded: Long,
    val lastScanned: Long?,
    val isActive: Boolean
)
