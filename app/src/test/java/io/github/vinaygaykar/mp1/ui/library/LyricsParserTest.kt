package io.github.vinaygaykar.mp1.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

    @Test
    fun `parse synced lyrics with gaps inserts instrumental lines`() {
        val lrcContent = """
            [00:10.00] First lyric line
            [00:40.00] Second lyric line
        """.trimIndent()

        val lyrics = LyricsParser.parse(lrcContent, isSynced = true)

        assertTrue(lyrics.isSynced)
        // Expected lines:
        // 1. 00:00.00 -> Intro instrumental gap (because first line >= 5000ms)
        // 2. 00:10.00 -> "First lyric line"
        // 3. Gap after "First lyric line" -> Instrumental gap inserted
        // 4. 00:40.00 -> "Second lyric line"
        assertEquals(4, lyrics.lines.size)

        assertEquals(0L, lyrics.lines[0].timestampMs)
        assertTrue(lyrics.lines[0].isInstrumental())

        assertEquals(10000L, lyrics.lines[1].timestampMs)
        assertEquals("First lyric line", lyrics.lines[1].text)
        assertFalse(lyrics.lines[1].isInstrumental())

        assertTrue(lyrics.lines[2].timestampMs > 10000L)
        assertTrue(lyrics.lines[2].timestampMs < 40000L)
        assertTrue(lyrics.lines[2].isInstrumental())

        assertEquals(40000L, lyrics.lines[3].timestampMs)
        assertEquals("Second lyric line", lyrics.lines[3].text)
        assertFalse(lyrics.lines[3].isInstrumental())
    }

    @Test
    fun `parse synced lyrics with explicit empty lines marks them as instrumental`() {
        val lrcContent = """
            [00:02.00] Hello
            [00:05.00] 
            [00:20.00] World
        """.trimIndent()

        val lyrics = LyricsParser.parse(lrcContent, isSynced = true)

        assertTrue(lyrics.isSynced)
        assertEquals(3, lyrics.lines.size)

        assertEquals(2000L, lyrics.lines[0].timestampMs)
        assertFalse(lyrics.lines[0].isInstrumental())

        assertEquals(5000L, lyrics.lines[1].timestampMs)
        assertTrue(lyrics.lines[1].isInstrumental())

        assertEquals(20000L, lyrics.lines[2].timestampMs)
        assertFalse(lyrics.lines[2].isInstrumental())
    }
}
