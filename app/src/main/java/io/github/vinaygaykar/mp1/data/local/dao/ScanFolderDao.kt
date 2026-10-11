package io.github.vinaygaykar.mp1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanFolderDao {
    @Query("SELECT * FROM scan_folders")
    fun getAllFolders(): Flow<List<ScanFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ScanFolderEntity)

    @Update
    suspend fun updateFolder(folder: ScanFolderEntity)

    @Query("DELETE FROM scan_folders WHERE id = :id")
    suspend fun deleteFolder(id: String)
}
