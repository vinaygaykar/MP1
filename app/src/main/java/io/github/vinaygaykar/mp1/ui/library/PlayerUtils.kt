package io.github.vinaygaykar.mp1.ui.library

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2

enum class DragValue {
    COLLAPSED,
    EXPANDED
}

fun launchActivityWithScaleUp(
    context: Context,
    intent: Intent,
    view: View? = null,
    startX: Int = 0,
    startY: Int = 0,
    width: Int = view?.width ?: 0,
    height: Int = view?.height ?: 0
) {
    if (view != null) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val options = ActivityOptions.makeScaleUpAnimation(
            view,
            startX,
            startY,
            safeWidth,
            safeHeight
        )
        context.startActivity(intent, options.toBundle())
    } else {
        context.startActivity(intent)
    }
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
