package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(contentEntity = SongEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "songs_fts")
data class SongFtsEntity(
    val title: String,
    val trackArtist: String,
    val albumName: String
)
