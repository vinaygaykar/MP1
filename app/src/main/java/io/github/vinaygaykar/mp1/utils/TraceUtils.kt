package io.github.vinaygaykar.mp1.utils

import android.os.SystemClock
import android.os.Trace
import android.util.Log

/**
 * Utility inline function to wrap code blocks with [Trace.beginSection] and [Trace.endSection]
 * for Perfetto/System Trace analysis, as well as logging execution duration to logcat.
 */
@Suppress("unused")
inline fun <T> traceSection(sectionName: String, block: () -> T): T {
    Trace.beginSection(sectionName)
    val startTime = SystemClock.elapsedRealtimeNanos()
    return try {
        block()
    } finally {
        val durationMs = (SystemClock.elapsedRealtimeNanos() - startTime) / 1_000_000.0
        Log.d("PERF_BASELINE", "[$sectionName] took ${"%.2f".format(durationMs)} ms")
        Trace.endSection()
    }
}
