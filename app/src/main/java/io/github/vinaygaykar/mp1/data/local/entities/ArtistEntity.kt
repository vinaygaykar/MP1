package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)]
)
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trackCount: Int,
    val imageUrl: String? = null
)
