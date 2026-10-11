package io.github.vinaygaykar.mp1.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMusicSourceTest {

    @Test
    fun `buildFtsQuery generates valid FTS match queries for simple search terms`() {
        val query = "hello"
        val fts = LocalMusicSource.buildFtsQuery(query)
        assertEquals("title:hello*", fts)
    }

    @Test
    fun `buildFtsQuery connects multiple tokens with AND operator`() {
        val query = "hello world"
        val fts = LocalMusicSource.buildFtsQuery(query)
        assertEquals("title:hello* AND title:world*", fts)
    }

    @Test
    fun `buildFtsQuery strips quotes and special punctuation that break FTS syntax`() {
        val query = "\"hello\" - world!"
        val fts = LocalMusicSource.buildFtsQuery(query)
        assertEquals("title:hello* AND title:world*", fts)
    }

    @Test
    fun `buildFtsQuery returns empty string for non-alphanumeric or empty input`() {
        assertEquals("", LocalMusicSource.buildFtsQuery(""))
        assertEquals("", LocalMusicSource.buildFtsQuery("   "))
        assertEquals("", LocalMusicSource.buildFtsQuery("@#$%^&*()"))
    }
}
