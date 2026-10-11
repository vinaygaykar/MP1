package io.github.vinaygaykar.mp1.utils

import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.Normalizer

private val WINDOWS_1252: Charset = Charset.forName("windows-1252")

/**
 * Default character delimiters for splitting multi-artist tags.
 */
val DEFAULT_ARTIST_DELIMITERS = listOf(";", ",", "/", "&", "+")

/**
 * Default word-based delimiters matched case-insensitively with whitespace boundaries.
 */
val DEFAULT_WORD_DELIMITERS = listOf(
    "featuring", "feat.", "feat", "ft.", "ft",
    "vs.", "vs", "versus", "with", "prod.", "prod", "x"
)

private const val ESCAPE_SEQUENCE = "\\\\"
private const val ESCAPE_PLACEHOLDER = "\u0000ESCAPED\u0000"

/**
 * Attempts to fix misencoded metadata strings (e.g. Windows-1252 mistakenly read as UTF-8),
 * strips null bytes (\u0000), and normalizes text into Unicode NFC form.
 */
fun String?.normalizeMetadataText(): String? {
    if (this == null) return null
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return trimmed

    val suspiciousPatterns = listOf("Ã", "Å", "Ã¢")
    val needsFix = suspiciousPatterns.any { trimmed.contains(it) }

    val candidate = if (needsFix) {
        val reencoded = runCatching {
            val bytes = trimmed.toByteArray(WINDOWS_1252)
            val decoded = String(bytes, Charsets.UTF_8).trim()
            if (decoded.contains('\uFFFD') || (decoded.contains('?') && !trimmed.contains('?'))) {
                null
            } else {
                decoded
            }
        }.getOrNull()
        reencoded?.takeIf { it.isNotEmpty() } ?: trimmed
    } else {
        trimmed
    }

    // Clean out null bytes from binary/tag metadata
    val cleaned = candidate.replace("\u0000", "")

    return Normalizer.normalize(cleaned, Normalizer.Form.NFC)
}

/**
 * Splits an artist string by character delimiters and word delimiters,
 * preserving escaped delimiters (e.g. "AC\\DC").
 *
 * @param delimiters Character delimiters (e.g., [";", ",", "/", "&", "+"])
 * @param wordDelimiters Word delimiters (e.g., ["feat.", "ft.", "vs.", "x"])
 * @return List of individual artist names trimmed and deduplicated.
 */
fun String.splitArtistsByDelimiters(
    delimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS
): List<String> {
    if ((delimiters.isEmpty() && wordDelimiters.isEmpty()) || this.isBlank()) {
        return listOf(this.trim()).filter { it.isNotEmpty() }
    }

    val sortedDelimiters = delimiters.sortedByDescending { it.length }
    var working = this

    // Replace escaped delimiters with placeholders
    val escapedMappings = mutableMapOf<String, String>()
    sortedDelimiters.forEachIndexed { index, delimiter ->
        val escapedDelimiter = ESCAPE_SEQUENCE + delimiter
        val placeholder = "${ESCAPE_PLACEHOLDER}${index}${ESCAPE_PLACEHOLDER}"
        escapedMappings[placeholder] = delimiter
        working = working.replace(escapedDelimiter, placeholder)
    }

    val patternParts = mutableListOf<String>()

    // Sort word delimiters longer ones first to avoid partial matches
    val sortedWordDelimiters = wordDelimiters.sortedByDescending { it.length }
    for (wd in sortedWordDelimiters) {
        val escaped = Regex.escape(wd)
        if (wd.length == 1) {
            // Single-char word delimiters (e.g. "x") require spaces on both sides
            patternParts.add("\\s+$escaped\\s+")
        } else {
            patternParts.add("\\s+$escaped\\s+|\\s+$escaped$|^$escaped\\s+")
        }
    }

    if (sortedDelimiters.isNotEmpty()) {
        val charPattern = sortedDelimiters.joinToString("|") { Regex.escape(it) }
        patternParts.add(charPattern)
    }

    if (patternParts.isEmpty()) {
        return listOf(this.trim()).filter { it.isNotEmpty() }
    }

    val combinedPattern = patternParts.joinToString("|")
    val regex = Regex(combinedPattern, RegexOption.IGNORE_CASE)

    val parts = working.split(regex)

    return parts
        .map { part ->
            var restored = part
            escapedMappings.forEach { (placeholder, delimiter) ->
                restored = restored.replace(placeholder, delimiter)
            }
            restored.trim()
        }
        .filter { it.isNotEmpty() }
        .distinct()
        .ifEmpty { if (this.trim().isNotEmpty()) listOf(this.trim()) else emptyList() }
}

/**
 * Extracts featured artists from a song title (e.g. "Title (feat. Artist B)" or "Title ft. Artist B")
 * and returns the cleaned title and extracted artist list.
 */
fun String.extractArtistsFromTitle(
    delimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS
): Pair<String, List<String>> {
    if (this.isBlank()) return this to emptyList()

    val sortedKeywords = wordDelimiters
        .filter { it.isNotBlank() }
        .sortedByDescending { it.length }
        .map { Regex.escape(it) }

    if (sortedKeywords.isEmpty()) return this to emptyList()

    val bracketPattern = Regex(
        """[(\[]\s*(?:${sortedKeywords.joinToString("|")})\s+(.+?)\s*[)\]]""",
        RegexOption.IGNORE_CASE
    )

    // For unbracketed features (e.g. "SICKO MODE ft. Drake"), exclude prepositions like "with"/"vs"
    val unbracketedExcluded = setOf("with", "vs", "vs.", "versus")
    val unbracketedKeywords = wordDelimiters
        .filter { it.length > 1 && it.lowercase() !in unbracketedExcluded }
        .sortedByDescending { it.length }
        .map { Regex.escape(it) }

    val unbracketedPattern = if (unbracketedKeywords.isNotEmpty()) {
        Regex(
            """(?:\s+-\s*|\s+)(?:${unbracketedKeywords.joinToString("|")})\s+(.+)""",
            RegexOption.IGNORE_CASE
        )
    } else null

    val extractedArtists = mutableListOf<String>()
    var cleanedTitle = this

    // 1. Match bracketed features e.g. "Title (ft. Artist)" or "Title [feat. Artist]"
    bracketPattern.findAll(cleanedTitle).forEach { match ->
        val rawArtistString = match.groupValues[1]
        val cleanedArtistString = rawArtistString.replace(Regex("""^(?:by\s+)+""", RegexOption.IGNORE_CASE), "").trim()
        val artists = cleanedArtistString.splitArtistsByDelimiters(delimiters, wordDelimiters)
        extractedArtists.addAll(artists)
        cleanedTitle = cleanedTitle.replace(match.value, "")
    }

    // 2. Match unbracketed features e.g. "Title ft. Artist" or "Title - feat. Artist"
    unbracketedPattern?.findAll(cleanedTitle)?.forEach { match ->
        val rawArtistString = match.groupValues[1]
        val cleanedArtistString = rawArtistString.replace(Regex("""^(?:by\s+)+""", RegexOption.IGNORE_CASE), "").trim()
        val artists = cleanedArtistString.splitArtistsByDelimiters(delimiters, wordDelimiters)
        extractedArtists.addAll(artists)
        cleanedTitle = cleanedTitle.replace(match.value, "")
    }

    return cleanedTitle.trim() to extractedArtists.distinct()
}

/**
 * Collects all artist names from a track's raw artist metadata and title.
 */
fun collectArtistNames(
    rawArtistName: String,
    title: String,
    artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
    extractFromTitle: Boolean = true
): List<String> {
    val splitFromArtist = rawArtistName.splitArtistsByDelimiters(artistDelimiters, wordDelimiters)
    if (!extractFromTitle) {
        return splitFromArtist
    }

    val (_, titleArtists) = title.extractArtistsFromTitle(artistDelimiters, wordDelimiters)
    if (titleArtists.isEmpty()) {
        return splitFromArtist
    }

    val combined = splitFromArtist.toMutableList()
    titleArtists.forEach { titleArtist ->
        if (combined.none { it.equals(titleArtist, ignoreCase = true) }) {
            combined.add(titleArtist)
        }
    }
    return combined
}

/**
 * Compares two artist strings (e.g. from local file vs MediaStore) and returns
 * the one that yields more individual artists after delimiter splitting.
 */
fun choosePreferredArtistName(
    localArtistName: String,
    mediaStoreArtistName: String,
    artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS
): String {
    val localTrimmed = localArtistName.trim()
    val mediaTrimmed = mediaStoreArtistName.trim()

    if (localTrimmed.isBlank()) return mediaStoreArtistName
    if (mediaTrimmed.isBlank()) return localArtistName

    val localArtists = localTrimmed.splitArtistsByDelimiters(artistDelimiters, wordDelimiters)
    val mediaArtists = mediaTrimmed.splitArtistsByDelimiters(artistDelimiters, wordDelimiters)

    return when {
        mediaArtists.size > localArtists.size -> mediaStoreArtistName
        localArtists.size > mediaArtists.size -> localArtistName
        mediaTrimmed.length > localTrimmed.length -> mediaStoreArtistName
        localTrimmed.length > mediaTrimmed.length -> localArtistName
        else -> mediaStoreArtistName
    }
}

/**
 * Utility to format a list of artist strings or objects into a display string (e.g. "Artist 1, Artist 2").
 */
fun List<String>.joinArtistsForDisplay(separator: String = ", "): String {
    return this.joinToString(separator)
}

/**
 * Generates the deterministic artist ID hash for an artist name.
 */
fun String.toArtistId(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
