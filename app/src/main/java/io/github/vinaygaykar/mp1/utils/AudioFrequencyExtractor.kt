package io.github.vinaygaykar.mp1.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Default fallback energy level for frequency bands. */
const val DEFAULT_BAND_ENERGY = 0.2f

@Immutable
data class FrequencyFrame(
    val low: Float = DEFAULT_BAND_ENERGY,  // Bass (20Hz - 250Hz)
    val mid: Float = DEFAULT_BAND_ENERGY,  // Mids (250Hz - 4000Hz)
    val high: Float = DEFAULT_BAND_ENERGY   // Treble (4000Hz - 20000Hz)
)

object AudioFrequencyExtractor {

    /** Value representing 16-bit PCM integer audio encoding (AudioFormat.ENCODING_PCM_16BIT). */
    private const val ENCODING_PCM_16BIT = 2

    /** Value representing 32-bit floating-point PCM audio encoding (AudioFormat.ENCODING_PCM_FLOAT). */
    private const val ENCODING_PCM_FLOAT = 4

    /** Duration in milliseconds for each frequency analysis window bucket (250 ms). */
    const val FREQUENCY_INTERVAL_MS = 250

    /** Default number of buckets accumulated before emitting a progressive Flow snapshot (4 buckets = 1 second). */
    const val DEFAULT_EMIT_BATCH_SIZE = 1000 / FREQUENCY_INTERVAL_MS

    /** Exponential power applied to normalized frequency band values to expand non-linear dynamic contrast. */
    const val DYNAMIC_CONTRAST_POWER = 2.2

    /** Fallback sample rate in Hz (CD quality) when not specified in media metadata format. */
    private const val DEFAULT_SAMPLE_RATE_HZ = 44100

    /** Fallback channel count (stereo) when not specified in media metadata format. */
    private const val DEFAULT_CHANNEL_COUNT = 2

    /** Upper cutoff frequency in Hz for the bass low-pass filter (250 Hz). */
    private const val BASS_CUTOFF_FREQ_HZ = 250f

    /** Center frequency in Hz for the mid-range band-pass filter (1000 Hz). */
    private const val MID_CENTER_FREQ_HZ = 1000f

    /** Bandwidth Q factor for the mid-range band-pass filter. */
    private const val MID_BANDPASS_Q = 1.0f

    /** Lower cutoff frequency in Hz for the treble high-pass filter (4000 Hz). */
    private const val TREBLE_CUTOFF_FREQ_HZ = 4000f

    /** Timeout in microseconds for MediaCodec dequeue output buffer operations. */
    private const val DEQUEUE_OUTPUT_TIMEOUT_US = 100L

    /** Default Q quality factor (1 / sqrt(2) ≈ 0.7071) producing a Butterworth filter response. */
    private const val DEFAULT_BUTTERWORTH_Q = 0.7071f

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

    /**
     * Extracts frequency frames progressively as a Flow, emitting updated arrays
     * of buckets as soon as they become available during decoding.
     *
     * @param emitBatchSize How many buckets to accumulate before emitting a progressive snapshot.
     */
    fun extractFrequencyFlow(
        context: Context,
        songId: String,
        uri: Uri,
        intervalMs: Int = FREQUENCY_INTERVAL_MS,
        emitBatchSize: Int = DEFAULT_EMIT_BATCH_SIZE
    ): Flow<Array<FrequencyFrame>> = flow {
        if (activeSongId == songId && activeFrequencyCurve != null) {
            emit(activeFrequencyCurve!!)
            return@flow
        }

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null

        try {
            val ext = MediaExtractor()
            extractor = ext
            ext.setDataSource(context, uri, null)

            val trackIndex = findAudioTrack(ext)
            if (trackIndex == -1) {
                error("No audio track found in media file")
            }
            ext.selectTrack(trackIndex)
            val format = ext.getTrackFormat(trackIndex)

            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else DEFAULT_SAMPLE_RATE_HZ

            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else DEFAULT_CHANNEL_COUNT

            // Mono downmixing means sampleRate samples per second across time
            val samplesPerBucket = max(1, (sampleRate * (intervalMs / 1000f)).toInt())

            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("MIME type not found")
            val dec = MediaCodec.createDecoderByType(mime)
            decoder = dec
            dec.configure(format, null, null, 0)
            dec.start()

            val lowFilter = BiquadFilter.lowPass(sampleRate.toFloat(), BASS_CUTOFF_FREQ_HZ)
            val midFilter = BiquadFilter.bandPass(sampleRate.toFloat(), MID_CENTER_FREQ_HZ, MID_BANDPASS_Q)
            val highFilter = BiquadFilter.highPass(sampleRate.toFloat(), TREBLE_CUTOFF_FREQ_HZ)

            val rawFrames = mutableListOf<Triple<Float, Float, Float>>()
            val bufferInfo = MediaCodec.BufferInfo()

            var sumLowSq = 0f
            var sumMidSq = 0f
            var sumHighSq = 0f
            var sampleCount = 0

            val pcmEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else ENCODING_PCM_16BIT

            var isExtractorEOS = false
            var isDecoderEOS = false

            val invChannelCount = 1f / channelCount
            val invShortMax = 1f / Short.MAX_VALUE

            while (!isDecoderEOS) {
                currentCoroutineContext().ensureActive()

                if (!isExtractorEOS) {
                    val inputBufferIndex = dec.dequeueInputBuffer(0L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = dec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer == null) {
                            dec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isExtractorEOS = true
                        } else {
                            val sampleSize = ext.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                dec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isExtractorEOS = true
                            } else {
                                dec.queueInputBuffer(inputBufferIndex, 0, sampleSize, ext.sampleTime, 0)
                                ext.advance()
                            }
                        }
                    }
                }

                var outputBufferIndex = dec.dequeueOutputBuffer(bufferInfo, DEQUEUE_OUTPUT_TIMEOUT_US)
                while (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isDecoderEOS = true
                    }
                    val outputBuffer = dec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val dup = outputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                        val startPos = bufferInfo.offset.coerceAtLeast(0)
                        val endPos = (bufferInfo.offset + bufferInfo.size).coerceAtMost(outputBuffer.capacity())

                        if (startPos < endPos) {
                            dup.position(startPos)
                            dup.limit(endPos)

                            when (pcmEncoding) {
                                ENCODING_PCM_FLOAT -> {
                                    val floatBuffer = dup.asFloatBuffer()
                                    val frameCount = floatBuffer.remaining() / channelCount
                                    for (f in 0 until frameCount) {
                                        var sum = 0f
                                        for (c in 0 until channelCount) {
                                            sum += floatBuffer.get()
                                        }
                                        val sample = sum * invChannelCount

                                        val lowSample = lowFilter.process(sample)
                                        val midSample = midFilter.process(sample)
                                        val highSample = highFilter.process(sample)

                                        sumLowSq += lowSample * lowSample
                                        sumMidSq += midSample * midSample
                                        sumHighSq += highSample * highSample
                                        sampleCount++

                                        if (sampleCount >= samplesPerBucket) {
                                            val rmsLow = sqrt(sumLowSq / sampleCount)
                                            val rmsMid = sqrt(sumMidSq / sampleCount)
                                            val rmsHigh = sqrt(sumHighSq / sampleCount)
                                            rawFrames.add(Triple(rmsLow, rmsMid, rmsHigh))

                                            sumLowSq = 0f
                                            sumMidSq = 0f
                                            sumHighSq = 0f
                                            sampleCount = 0

                                            if (rawFrames.size == 1 || rawFrames.size % emitBatchSize == 0) {
                                                emit(processRawFramesToFrequencyFrames(rawFrames))
                                            }
                                        }
                                    }
                                }
                                else -> { // ENCODING_PCM_16BIT
                                    val shortBuffer = dup.asShortBuffer()
                                    val frameCount = shortBuffer.remaining() / channelCount
                                    for (f in 0 until frameCount) {
                                        var sum = 0f
                                        for (c in 0 until channelCount) {
                                            sum += shortBuffer.get()
                                        }
                                        val sample = sum * invChannelCount * invShortMax

                                        val lowSample = lowFilter.process(sample)
                                        val midSample = midFilter.process(sample)
                                        val highSample = highFilter.process(sample)

                                        sumLowSq += lowSample * lowSample
                                        sumMidSq += midSample * midSample
                                        sumHighSq += highSample * highSample
                                        sampleCount++

                                        if (sampleCount >= samplesPerBucket) {
                                            val rmsLow = sqrt(sumLowSq / sampleCount)
                                            val rmsMid = sqrt(sumMidSq / sampleCount)
                                            val rmsHigh = sqrt(sumHighSq / sampleCount)
                                            rawFrames.add(Triple(rmsLow, rmsMid, rmsHigh))

                                            sumLowSq = 0f
                                            sumMidSq = 0f
                                            sumHighSq = 0f
                                            sampleCount = 0

                                            if (rawFrames.size == 1 || rawFrames.size % emitBatchSize == 0) {
                                                emit(processRawFramesToFrequencyFrames(rawFrames))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    dec.releaseOutputBuffer(outputBufferIndex, false)
                    if (!isDecoderEOS) {
                        outputBufferIndex = dec.dequeueOutputBuffer(bufferInfo, 0L)
                    } else {
                        outputBufferIndex = MediaCodec.INFO_TRY_AGAIN_LATER
                    }
                }
            }

            if (sampleCount > 0) {
                val rmsLow = sqrt(sumLowSq / sampleCount)
                val rmsMid = sqrt(sumMidSq / sampleCount)
                val rmsHigh = sqrt(sumHighSq / sampleCount)
                rawFrames.add(Triple(rmsLow, rmsMid, rmsHigh))
            }

            val finalFrames = processRawFramesToFrequencyFrames(rawFrames)
            activeSongId = songId
            activeFrequencyCurve = finalFrames
            emit(finalFrames)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun extractFrequency(
        context: Context,
        songId: String,
        uri: Uri,
        intervalMs: Int = FREQUENCY_INTERVAL_MS
    ): Result<Array<FrequencyFrame>> = runCatching {
        extractFrequencyFlow(context, songId, uri, intervalMs).last()
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

    private fun processRawFramesToFrequencyFrames(
        rawFrames: List<Triple<Float, Float, Float>>
    ): Array<FrequencyFrame> {
        if (rawFrames.isEmpty()) {
            return arrayOf(FrequencyFrame(DEFAULT_BAND_ENERGY, DEFAULT_BAND_ENERGY, DEFAULT_BAND_ENERGY))
        }

        var maxLow = 0.0001f; var minLow = Float.MAX_VALUE
        var maxMid = 0.0001f; var minMid = Float.MAX_VALUE
        var maxHigh = 0.0001f; var minHigh = Float.MAX_VALUE

        for (i in rawFrames.indices) {
            val (l, m, h) = rawFrames[i]
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
        return Array(normalized.size) { i ->
            val prev = normalized.getOrElse(i - 1) { normalized[i] }
            val curr = normalized[i]
            val next = normalized.getOrElse(i + 1) { normalized[i] }

            FrequencyFrame(
                low = (prev.low + 2f * curr.low + next.low) / 4f,
                mid = (prev.mid + 2f * curr.mid + next.mid) / 4f,
                high = (prev.high + 2f * curr.high + next.high) / 4f
            )
        }
    }

    // Biquad Filter Helper using Float operations
    private class BiquadFilter(
        private val b0: Float,
        private val b1: Float,
        private val b2: Float,
        private val a1: Float,
        private val a2: Float
    ) {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        companion object {
            fun lowPass(sampleRate: Float, cutoff: Float, q: Float = DEFAULT_BUTTERWORTH_Q): BiquadFilter {
                val w0 = (2.0 * Math.PI * cutoff / sampleRate).toFloat()
                val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
                val cosW0 = cos(w0.toDouble()).toFloat()

                val b0 = (1f - cosW0) / 2f
                val b1 = 1f - cosW0
                val b2 = (1f - cosW0) / 2f
                val a0 = 1f + alpha
                val a1 = -2f * cosW0
                val a2 = 1f - alpha

                return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }

            fun highPass(sampleRate: Float, cutoff: Float, q: Float = DEFAULT_BUTTERWORTH_Q): BiquadFilter {
                val w0 = (2.0 * Math.PI * cutoff / sampleRate).toFloat()
                val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
                val cosW0 = cos(w0.toDouble()).toFloat()

                val b0 = (1f + cosW0) / 2f
                val b1 = -(1f + cosW0)
                val b2 = (1f + cosW0) / 2f
                val a0 = 1f + alpha
                val a1 = -2f * cosW0
                val a2 = 1f - alpha

                return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }

            fun bandPass(sampleRate: Float, centerFreq: Float, q: Float = 1.0f): BiquadFilter {
                val w0 = (2.0 * Math.PI * centerFreq / sampleRate).toFloat()
                val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
                val cosW0 = cos(w0.toDouble()).toFloat()

                val b0 = alpha
                val b1 = 0f
                val b2 = -alpha
                val a0 = 1f + alpha
                val a1 = -2f * cosW0
                val a2 = 1f - alpha

                return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }
        }
    }
}
