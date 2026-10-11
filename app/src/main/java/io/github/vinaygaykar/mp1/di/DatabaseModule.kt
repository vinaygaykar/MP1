package io.github.vinaygaykar.mp1.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.vinaygaykar.mp1.data.local.MusicDatabase
import io.github.vinaygaykar.mp1.data.local.dao.AlbumDao
import io.github.vinaygaykar.mp1.data.local.dao.ArtistDao
import io.github.vinaygaykar.mp1.data.local.dao.GenreDao
import io.github.vinaygaykar.mp1.data.local.dao.PlaybackStateDao
import io.github.vinaygaykar.mp1.data.local.dao.PlaylistDao
import io.github.vinaygaykar.mp1.data.local.dao.QueueDao
import io.github.vinaygaykar.mp1.data.local.dao.ScanFolderDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE songs ADD COLUMN artistsDisplayString TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE songs SET artistsDisplayString = trackArtist WHERE artistsDisplayString = ''")
        }
    }

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_db"
        ).addMigrations(MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSongDao(database: MusicDatabase): SongDao = database.songDao()

    @Provides
    fun provideAlbumDao(database: MusicDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideArtistDao(database: MusicDatabase): ArtistDao = database.artistDao()

    @Provides
    fun provideGenreDao(database: MusicDatabase): GenreDao = database.genreDao()

    @Provides
    fun provideQueueDao(database: MusicDatabase): QueueDao = database.queueDao()

    @Provides
    fun provideScanFolderDao(database: MusicDatabase): ScanFolderDao = database.scanFolderDao()

    @Provides
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlaybackStateDao(database: MusicDatabase): PlaybackStateDao = database.playbackStateDao()
}
