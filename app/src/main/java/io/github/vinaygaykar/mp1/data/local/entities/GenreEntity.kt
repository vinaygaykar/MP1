package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "genres",
    indices = [Index(value = ["name"], unique = true)]
)
data class GenreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trackCount: Int
)
