package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2

enum class DragValue {
    MINI,
    COLLAPSED,
    EXPANDED
}

fun calculateAngle(offset: Offset, center: Offset): Float {
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val radians = atan2(dy.toDouble(), dx.toDouble())
    return (radians * 180 / PI).toFloat()
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
