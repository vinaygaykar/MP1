package io.github.vinaygaykar.mp1.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vinaygaykar.mp1.data.local.entities.AlbumEntity
import io.github.vinaygaykar.mp1.data.local.entities.ArtistEntity
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import io.github.vinaygaykar.mp1.data.local.entities.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicDatabaseTest {
    private lateinit var db: MusicDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MusicDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadScanFolder() = runBlocking {
        val folder = ScanFolderEntity(
            id = "test_id",
            treeUri = "content://test",
            displayName = "Test Folder",
            dateAdded = 12345L,
            lastScanned = null,
            isActive = true
        )
        db.scanFolderDao().insertFolder(folder)
        val allFolders = db.scanFolderDao().getAllFolders().first()
        assertEquals(1, allFolders.size)
        assertEquals("Test Folder", allFolders[0].displayName)
    }

    @Test
    fun deleteOrphanedMetadataCleansUpUnusedAlbumsAndArtists() = runBlocking {
        val folder = ScanFolderEntity(
            id = "folder_1",
            treeUri = "content://test_1",
            displayName = "Folder 1",
            dateAdded = 1000L,
            lastScanned = null,
            isActive = true
        )
        db.scanFolderDao().insertFolder(folder)

        val album1 = AlbumEntity("album_1", "Old Album", "Artist 1", 2020, null, 1)
        val album2 = AlbumEntity("album_2", "New Album", "Artist 2", 2021, null, 1)
        val artist1 = ArtistEntity("artist_1", "Artist 1", 1)
        val artist2 = ArtistEntity("artist_2", "Artist 2", 1)

        db.albumDao().insertAlbums(listOf(album1, album2))
        db.artistDao().insertArtists(listOf(artist1, artist2))

        val song = SongEntity(
            id = "song_2",
            uri = "content://song_2",
            fileName = "song_2.mp3",
            relativePath = "path_2",
            scanFolderId = "folder_1",
            title = "Song 2",
            trackArtist = "Artist 2",
            albumName = "New Album",
            artistName = "Artist 2",
            artistId = "artist_2",
            albumId = "album_2",
            genreId = null,
            trackNumber = 1,
            discNumber = 1,
            year = 2021,
            durationMs = 180000,
            dateAdded = 1000L,
            lastModified = 1000L,
            fileSize = 5000000,
            artworkCachePath = null,
            mimeType = "audio/mpeg"
        )
        db.songDao().insertSongs(listOf(song))

        // Before cleanup, 2 albums and 2 artists exist
        assertEquals(2, db.albumDao().getAllAlbums().first().size)
        assertEquals(2, db.artistDao().getAllArtists().first().size)

        // Perform orphan cleanup
        db.albumDao().deleteOrphanedAlbums()
        db.artistDao().deleteOrphanedArtists()

        // After cleanup, only album_2 and artist_2 remain
        val albums = db.albumDao().getAllAlbums().first()
        val artists = db.artistDao().getAllArtists().first()

        assertEquals(1, albums.size)
        assertEquals("New Album", albums[0].name)

        assertEquals(1, artists.size)
        assertEquals("Artist 2", artists[0].name)
    }

    @Test
    fun updateTrackCountsCalculatesCorrectCount() = runBlocking {
        val folder = ScanFolderEntity(
            id = "folder_1",
            treeUri = "content://test_1",
            displayName = "Folder 1",
            dateAdded = 1000L,
            lastScanned = null,
            isActive = true
        )
        db.scanFolderDao().insertFolder(folder)

        val album = AlbumEntity("album_1", "Album 1", "Artist 1", 2020, null, 0)
        val artist = ArtistEntity("artist_1", "Artist 1", 0)

        db.albumDao().insertAlbums(listOf(album))
        db.artistDao().insertArtists(listOf(artist))

        val song1 = SongEntity(
            id = "song_1",
            uri = "content://song_1",
            fileName = "song_1.mp3",
            relativePath = "path_1",
            scanFolderId = "folder_1",
            title = "Song 1",
            trackArtist = "Artist 1",
            albumName = "Album 1",
            artistName = "Artist 1",
            artistId = "artist_1",
            albumId = "album_1",
            genreId = null,
            trackNumber = 1,
            discNumber = 1,
            year = 2020,
            durationMs = 180000,
            dateAdded = 1000L,
            lastModified = 1000L,
            fileSize = 5000000,
            artworkCachePath = null,
            mimeType = "audio/mpeg"
        )
        val song2 = song1.copy(id = "song_2", uri = "content://song_2", title = "Song 2")

        db.songDao().insertSongs(listOf(song1, song2))

        db.albumDao().updateTrackCounts()
        db.artistDao().updateTrackCounts()

        val albums = db.albumDao().getAllAlbums().first()
        val artists = db.artistDao().getAllArtists().first()

        assertEquals(2, albums[0].trackCount)
        assertEquals(2, artists[0].trackCount)
    }

    @Test
    fun searchSongs_returnsMatchingSongs() = runBlocking {
        val folder = ScanFolderEntity(
            id = "folder_1",
            treeUri = "content://test_1",
            displayName = "Folder 1",
            dateAdded = 1000L,
            lastScanned = null,
            isActive = true
        )
        db.scanFolderDao().insertFolder(folder)

        val song1 = SongEntity(
            id = "song_1",
            uri = "content://song_1",
            fileName = "song_1.mp3",
            relativePath = "path_1",
            scanFolderId = "folder_1",
            title = "Bohemian Rhapsody",
            trackArtist = "Queen",
            albumName = "A Night at the Opera",
            artistName = "Queen",
            artistId = "artist_1",
            albumId = "album_1",
            genreId = null,
            trackNumber = 1,
            discNumber = 1,
            year = 1975,
            durationMs = 354000,
            dateAdded = 1000L,
            lastModified = 1000L,
            fileSize = 5000000,
            artworkCachePath = null,
            mimeType = "audio/mpeg"
        )
        val song2 = song1.copy(
            id = "song_2",
            uri = "content://song_2",
            title = "Stairway to Heaven",
            trackArtist = "Led Zeppelin",
            albumName = "Led Zeppelin IV"
        )

        db.songDao().insertSongs(listOf(song1, song2))

        val ftsQuery = LocalMusicSource.buildFtsQuery("Bohemian")
        val results = db.songDao().searchSongs(ftsQuery = ftsQuery, rawQuery = "Bohemian").first()

        assertEquals(1, results.size)
        assertEquals("Bohemian Rhapsody", results[0].title)

        val artistResults = db.songDao().searchSongs(ftsQuery = LocalMusicSource.buildFtsQuery("Queen"), rawQuery = "Queen").first()
        assertEquals(0, artistResults.size)
    }
}
