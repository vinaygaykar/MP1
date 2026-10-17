package io.github.vinaygaykar.mp1.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
data class FrequencyFrame(
    val low: Float = 0.5f,  // Bass (20Hz - 250Hz)
    val mid: Float = 0.5f,  // Mids (250Hz - 4000Hz)
    val high: Float = 0.5f  // Treble (4000Hz - 20000Hz)
)

object AudioFrequencyExtractor {

    private const val TIMEOUT_US = 1000L
    private const val ENCODING_PCM_16BIT = 2
    private const val ENCODING_PCM_FLOAT = 4

    const val FREQUENCY_INTERVAL_MS = 250
    const val DYNAMIC_CONTRAST_POWER = 2.2

    @Volatile
    private var activeSongId: String? = null

    @Volatile
    private var activeFrequencyCurve: Array<FrequencyFrame>? = null

    fun getCachedFrequency(songId: String): Array<FrequencyFrame>? {
        return if (activeSongId == songId) activeFrequencyCurve else null
    }

    fun clearCache() {
        activeSongId = null
        activeFrequencyCurve = null
    }

    suspend fun extractFrequency(
        context: Context,
        songId: String,
        uri: Uri,
        intervalMs: Int = FREQUENCY_INTERVAL_MS
    ): Result<Array<FrequencyFrame>> = withContext(Dispatchers.IO) {
        if (activeSongId == songId && activeFrequencyCurve != null) {
            return@withContext Result.success(activeFrequencyCurve!!)
        }

        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            val trackIndex = findAudioTrack(extractor)
            if (trackIndex == -1) {
                extractor.release()
                error("No audio track found in media file")
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val samplesPerBucket = max(1, (sampleRate * channelCount * (intervalMs / 1000f)).toInt())

            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("MIME type not found")
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            // Initialize Biquad Filters for Low, Mid, High bands
            val lowFilter = BiquadFilter.lowPass(sampleRate.toDouble(), 250.0)
            val midFilter = BiquadFilter.bandPass(sampleRate.toDouble(), 1000.0, 1.0)
            val highFilter = BiquadFilter.highPass(sampleRate.toDouble(), 4000.0)

            val rawFrames = mutableListOf<Triple<Float, Float, Float>>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEndOfStream = false

            var sumLowSq = 0.0
            var sumMidSq = 0.0
            var sumHighSq = 0.0
            var sampleCount = 0

            while (!isEndOfStream) {
                val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    if (inputBuffer == null) {
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEndOfStream = true
                    } else {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEndOfStream = true
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val samples = byteBufferToFloats(outputBuffer, bufferInfo, format)
                        for (sample in samples) {
                            val lowSample = lowFilter.process(sample.toDouble())
                            val midSample = midFilter.process(sample.toDouble())
                            val highSample = highFilter.process(sample.toDouble())

                            sumLowSq += lowSample * lowSample
                            sumMidSq += midSample * midSample
                            sumHighSq += highSample * highSample
                            sampleCount++

                            if (sampleCount >= samplesPerBucket) {
                                val rmsLow = sqrt(sumLowSq / sampleCount).toFloat()
                                val rmsMid = sqrt(sumMidSq / sampleCount).toFloat()
                                val rmsHigh = sqrt(sumHighSq / sampleCount).toFloat()

                                rawFrames.add(Triple(rmsLow, rmsMid, rmsHigh))

                                sumLowSq = 0.0
                                sumMidSq = 0.0
                                sumHighSq = 0.0
                                sampleCount = 0
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                }
            }

            if (sampleCount > 0) {
                val rmsLow = sqrt(sumLowSq / sampleCount).toFloat()
                val rmsMid = sqrt(sumMidSq / sampleCount).toFloat()
                val rmsHigh = sqrt(sumHighSq / sampleCount).toFloat()
                rawFrames.add(Triple(rmsLow, rmsMid, rmsHigh))
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            if (rawFrames.isEmpty()) {
                val defaultArray = arrayOf(FrequencyFrame(0.5f, 0.5f, 0.5f))
                return@runCatching defaultArray
            }

            // Normalization
            var maxLow = 0.0001f; var minLow = Float.MAX_VALUE
            var maxMid = 0.0001f; var minMid = Float.MAX_VALUE
            var maxHigh = 0.0001f; var minHigh = Float.MAX_VALUE

            for ((l, m, h) in rawFrames) {
                if (l > maxLow) maxLow = l; if (l < minLow) minLow = l
                if (m > maxMid) maxMid = m; if (m < minMid) minMid = m
                if (h > maxHigh) maxHigh = h; if (h < minHigh) minHigh = h
            }

            val rangeLow = max(0.0001f, maxLow - minLow)
            val rangeMid = max(0.0001f, maxMid - minMid)
            val rangeHigh = max(0.0001f, maxHigh - minHigh)

            val normalized = Array(rawFrames.size) { i ->
                val (l, m, h) = rawFrames[i]
                val normL = (((l - minLow) / rangeLow).coerceIn(0f, 1f).toDouble().pow(DYNAMIC_CONTRAST_POWER).toFloat()).coerceIn(0f, 1f)
                val normM = (((m - minMid) / rangeMid).coerceIn(0f, 1f).toDouble().pow(DYNAMIC_CONTRAST_POWER).toFloat()).coerceIn(0f, 1f)
                val normH = (((h - minHigh) / rangeHigh).coerceIn(0f, 1f).toDouble().pow(DYNAMIC_CONTRAST_POWER).toFloat()).coerceIn(0f, 1f)
                FrequencyFrame(normL, normM, normH)
            }

            // 3-point weighted moving average smoothing
            val smoothed = Array(normalized.size) { i ->
                val prev = normalized.getOrElse(i - 1) { normalized[i] }
                val curr = normalized[i]
                val next = normalized.getOrElse(i + 1) { normalized[i] }

                FrequencyFrame(
                    low = (prev.low + 2f * curr.low + next.low) / 4f,
                    mid = (prev.mid + 2f * curr.mid + next.mid) / 4f,
                    high = (prev.high + 2f * curr.high + next.high) / 4f
                )
            }

            activeSongId = songId
            activeFrequencyCurve = smoothed
            smoothed
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    private fun byteBufferToFloats(
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        format: MediaFormat
    ): FloatArray {
        val pcmEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else ENCODING_PCM_16BIT

        val dup = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val startPos = bufferInfo.offset.coerceAtLeast(0)
        val endPos = (bufferInfo.offset + bufferInfo.size).coerceAtMost(buffer.capacity())
        if (startPos >= endPos) return FloatArray(0)

        dup.position(startPos)
        dup.limit(endPos)

        return when (pcmEncoding) {
            ENCODING_PCM_16BIT -> {
                val shortBuffer = dup.asShortBuffer()
                FloatArray(shortBuffer.remaining()) {
                    shortBuffer.get().toFloat() / Short.MAX_VALUE
                }
            }
            ENCODING_PCM_FLOAT -> {
                val floatBuffer = dup.asFloatBuffer()
                FloatArray(floatBuffer.remaining()) { floatBuffer.get() }
            }
            else -> {
                val shortBuffer = dup.asShortBuffer()
                FloatArray(shortBuffer.remaining()) {
                    shortBuffer.get().toFloat() / Short.MAX_VALUE
                }
            }
        }
    }

    // Biquad Filter Helper
    private class BiquadFilter(
        private val b0: Double,
        private val b1: Double,
        private val b2: Double,
        private val a1: Double,
        private val a2: Double
    ) {
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        fun process(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        companion object {
            fun lowPass(sampleRate: Double, cutoff: Double, q: Double = 0.707): BiquadFilter {
                val w0 = 2.0 * Math.PI * cutoff / sampleRate
                val alpha = sin(w0) / (2.0 * q)
                val cosW0 = cos(w0)

                val b0 = (1.0 - cosW0) / 2.0
                val b1 = 1.0 - cosW0
                val b2 = (1.0 - cosW0) / 2.0
                val a0 = 1.0 + alpha
                val a1 = -2.0 * cosW0
                val a2 = 1.0 - alpha

                return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }

            fun highPass(sampleRate: Double, cutoff: Double, q: Double = 0.707): BiquadFilter {
                val w0 = 2.0 * Math.PI * cutoff / sampleRate
                val alpha = sin(w0) / (2.0 * q)
                val cosW0 = cos(w0)

                val b0 = (1.0 + cosW0) / 2.0
                val b1 = -(1.0 + cosW0)
                val b2 = (1.0 + cosW0) / 2.0
                val a0 = 1.0 + alpha
                val a1 = -2.0 * cosW0
                val a2 = 1.0 - alpha

                return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }

            fun bandPass(sampleRate: Double, centerFreq: Double, q: Double = 1.0): BiquadFilter {
                val w0 = 2.0 * Math.PI * centerFreq / sampleRate
                val alpha = sin(w0) / (2.0 * q)
                val cosW0 = cos(w0)

                val b0 = alpha
                val b1 = 0.0
                val b2 = -alpha
                val a0 = 1.0 + alpha
                val a1 = -2.0 * cosW0
                val a2 = 1.0 - alpha

                return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }
        }
    }
}
