package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    indices = [Index(value = ["name", "albumArtist"], unique = true)]
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val albumArtist: String,
    val year: Int?,
    val artworkCachePath: String?,
    val trackCount: Int
)
