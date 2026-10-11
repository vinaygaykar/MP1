package io.github.vinaygaykar.mp1.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtworkUriUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun parseArtworkUri_returnsNullForNullOrBlank() {
        assertNull(parseArtworkUri(null))
        assertNull(parseArtworkUri(""))
        assertNull(parseArtworkUri("   "))
    }

    @Test
    fun parseArtworkUri_returnsNullForNonExistentLocalFile() {
        val nonExistentPath = "/data/user/0/io.github.vinaygaykar.mp1/cache/artwork/non_existent_123.jpg"
        assertNull(parseArtworkUri(nonExistentPath))
    }

    @Test
    fun parseArtworkUri_returnsFileUriForExistingLocalFile() {
        val sampleFile = tempFolder.newFile("album_art.jpg")
        sampleFile.writeText("sample image content")

        val resultUri = parseArtworkUri(sampleFile.absolutePath)
        assertNotNull(resultUri)
        assertEquals("file", resultUri?.scheme)
        assertEquals(sampleFile.absolutePath, resultUri?.path)
    }

    @Test
    fun parseArtworkUri_handlesFileSchemePrefixForExistingFile() {
        val sampleFile = tempFolder.newFile("album_art_2.jpg")
        sampleFile.writeText("sample image content")

        val fileUriString = "file://" + sampleFile.absolutePath
        val resultUri = parseArtworkUri(fileUriString)
        assertNotNull(resultUri)
        assertEquals("file", resultUri?.scheme)
        assertEquals(sampleFile.absolutePath, resultUri?.path)
    }

    @Test
    fun parseArtworkUri_returnsContentUriDirectlyForContentScheme() {
        val contentUriString = "content://media/external/audio/albumart/123"
        val resultUri = parseArtworkUri(contentUriString)

        assertNotNull(resultUri)
        assertEquals("content", resultUri?.scheme)
        assertEquals(contentUriString, resultUri.toString())
    }
}
