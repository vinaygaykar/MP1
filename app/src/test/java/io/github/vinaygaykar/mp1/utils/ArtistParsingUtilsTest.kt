package io.github.vinaygaykar.mp1.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistParsingUtilsTest {

    @Test
    fun `splitArtistsByDelimiters splits by semicolon, comma, slash, ampersand, and plus`() {
        val result = "Calvin Harris; Pharrell Williams, Katy Perry / Big Sean & Funk Wav + Snoop Dogg"
            .splitArtistsByDelimiters()

        assertEquals(
            listOf("Calvin Harris", "Pharrell Williams", "Katy Perry", "Big Sean", "Funk Wav", "Snoop Dogg"),
            result
        )
    }

    @Test
    fun `splitArtistsByDelimiters preserves escaped delimiters`() {
        val result = "AC\\\\/DC; W&W; Lost & Found; Black Country, New Road"
            .splitArtistsByDelimiters(delimiters = listOf(";", "/"))

        assertEquals(
            listOf("AC/DC", "W&W", "Lost & Found", "Black Country, New Road"),
            result
        )
    }

    @Test
    fun `splitArtistsByDelimiters handles word delimiters like feat, ft, vs, x`() {
        val result = "Marshmello x Bastille feat. Drake ft. Rihanna vs. David Guetta"
            .splitArtistsByDelimiters(delimiters = emptyList())

        assertEquals(
            listOf("Marshmello", "Bastille", "Drake", "Rihanna", "David Guetta"),
            result
        )
    }

    @Test
    fun `extractArtistsFromTitle extracts featured artists from track title`() {
        val (cleanedTitle, titleArtists) = "Feels (feat. Katy Perry & Big Sean)"
            .extractArtistsFromTitle()

        assertEquals("Feels", cleanedTitle)
        assertEquals(listOf("Katy Perry", "Big Sean"), titleArtists)
    }

    @Test
    fun `collectArtistNames merges title feature artists without duplicating existing artists`() {
        val result = collectArtistNames(
            rawArtistName = "Calvin Harris; Pharrell Williams",
            title = "Feels (feat. Katy Perry & Big Sean)"
        )

        assertEquals(
            listOf("Calvin Harris", "Pharrell Williams", "Katy Perry", "Big Sean"),
            result
        )
    }

    @Test
    fun `choosePreferredArtistName picks the source with more artists`() {
        val preferred = choosePreferredArtistName(
            localArtistName = "Calvin Harris",
            mediaStoreArtistName = "Calvin Harris; Pharrell Williams; Katy Perry"
        )

        assertEquals("Calvin Harris; Pharrell Williams; Katy Perry", preferred)
    }

    @Test
    fun `normalizeMetadataText strips null bytes and fixes encoding issues`() {
        val raw = "Artist\u0000 Name"
        assertEquals("Artist Name", raw.normalizeMetadataText())
    }

    @Test
    fun `normalizeMetadataText preserves non-English characters`() {
        val russian = "Песня"
        val spanish = "Señor"
        val japanese = "日本語"
        val german = "Müller"
        val french = "Café"

        assertEquals("Песня", russian.normalizeMetadataText())
        assertEquals("Señor", spanish.normalizeMetadataText())
        assertEquals("日本語", japanese.normalizeMetadataText())
        assertEquals("Müller", german.normalizeMetadataText())
        assertEquals("Café", french.normalizeMetadataText())
    }

    @Test
    fun `collectArtistNames extracts list of artists from raw artist string and title`() {
        val artists = collectArtistNames(
            rawArtistName = "Calvin Harris; Pharrell Williams",
            title = "Feels (feat. Katy Perry)"
        )

        assertEquals(
            listOf("Calvin Harris", "Pharrell Williams", "Katy Perry"),
            artists
        )
    }

    @Test
    fun `extractArtistsFromTitle handles unbracketed featured artists`() {
        val (cleanedTitle, titleArtists) = "SICKO MODE ft. Drake"
            .extractArtistsFromTitle()

        assertEquals("SICKO MODE", cleanedTitle)
        assertEquals(listOf("Drake"), titleArtists)
    }

    @Test
    fun `collectArtistNames considers unbracketed featured artists in title`() {
        val artists = collectArtistNames(
            rawArtistName = "Travis Scott",
            title = "SICKO MODE ft. Drake"
        )

        assertEquals(
            listOf("Travis Scott", "Drake"),
            artists
        )
    }
}
