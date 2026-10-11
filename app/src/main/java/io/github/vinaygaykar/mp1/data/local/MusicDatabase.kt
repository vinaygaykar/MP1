package io.github.vinaygaykar.mp1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.vinaygaykar.mp1.data.local.dao.AlbumDao
import io.github.vinaygaykar.mp1.data.local.dao.ArtistDao
import io.github.vinaygaykar.mp1.data.local.dao.GenreDao
import io.github.vinaygaykar.mp1.data.local.dao.PlaybackStateDao
import io.github.vinaygaykar.mp1.data.local.dao.PlaylistDao
import io.github.vinaygaykar.mp1.data.local.dao.QueueDao
import io.github.vinaygaykar.mp1.data.local.dao.ScanFolderDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import io.github.vinaygaykar.mp1.data.local.entities.AlbumEntity
import io.github.vinaygaykar.mp1.data.local.entities.ArtistEntity
import io.github.vinaygaykar.mp1.data.local.entities.GenreEntity
import io.github.vinaygaykar.mp1.data.local.entities.PlaybackStateEntity
import io.github.vinaygaykar.mp1.data.local.entities.PlaylistEntity
import io.github.vinaygaykar.mp1.data.local.entities.PlaylistSongEntity
import io.github.vinaygaykar.mp1.data.local.entities.QueueItemEntity
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import io.github.vinaygaykar.mp1.data.local.entities.SongArtistCrossRef
import io.github.vinaygaykar.mp1.data.local.entities.SongEntity
import io.github.vinaygaykar.mp1.data.local.entities.SongFtsEntity

@Database(
    entities = [
        SongEntity::class,
        SongFtsEntity::class,
        SongArtistCrossRef::class,
        AlbumEntity::class,
        ArtistEntity::class,
        GenreEntity::class,
        ScanFolderEntity::class,
        QueueItemEntity::class,
        PlaybackStateEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = MusicDatabase.VERSION,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 9
    }

    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun genreDao(): GenreDao
    abstract fun queueDao(): QueueDao
    abstract fun scanFolderDao(): ScanFolderDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStateDao(): PlaybackStateDao
}
