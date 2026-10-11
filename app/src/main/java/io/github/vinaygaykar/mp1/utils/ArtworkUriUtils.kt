package io.github.vinaygaykar.mp1.utils

import android.net.Uri
import java.io.File

/**
 * Converts an artwork path or URI string into an [Uri].
 *
 * If [pathOrUri] is a raw file path (e.g., starting with `/`), it uses [Uri.fromFile]
 * to produce a proper `file://` URI so image loaders like Coil can handle it correctly.
 * If it already has a scheme (e.g., `content://`, `file://`, `http://`), [Uri.parse] is used.
 */
fun parseArtworkUri(pathOrUri: String?): Uri? {
    if (pathOrUri.isNullOrBlank()) return null
    if (pathOrUri.startsWith("content://") ||
        pathOrUri.startsWith("http://") ||
        pathOrUri.startsWith("https://")
    ) {
        return Uri.parse(pathOrUri)
    }

    val filePath = if (pathOrUri.startsWith("file://")) {
        pathOrUri.substring(7)
    } else {
        pathOrUri
    }

    val file = File(filePath)
    return if (file.exists() && file.length() > 0) {
        Uri.fromFile(file)
    } else {
        null
    }
}
