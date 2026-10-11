package io.github.vinaygaykar.mp1.domain.models

import android.net.Uri

data class ScanFolder(
    val id: String,
    val treeUri: Uri,
    val displayName: String,
    val isActive: Boolean,
    val lastScanned: Long?
)
