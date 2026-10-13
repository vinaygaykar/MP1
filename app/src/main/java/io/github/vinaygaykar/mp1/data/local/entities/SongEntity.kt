package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["genreId"]),
        Index(value = ["scanFolderId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ScanFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val fileName: String,
    val relativePath: String,
    val scanFolderId: String,
    val title: String,
    val trackArtist: String,
    val albumName: String, // Denormalized
    val artistName: String, // Denormalized
    val artistId: String,
    val albumId: String?,
    val genreId: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val durationMs: Long,
    val dateAdded: Long,
    val lastModified: Long,
    val fileSize: Long,
    val artworkCachePath: String?,
    val mimeType: String,
    val artistsDisplayString: String = ""
)
