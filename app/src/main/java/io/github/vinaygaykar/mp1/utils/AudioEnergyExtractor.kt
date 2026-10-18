package io.github.vinaygaykar.mp1.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object AudioEnergyExtractor {

    private const val TIMEOUT_US = 1000L
    private const val ENCODING_PCM_16BIT = 2
    private const val ENCODING_PCM_FLOAT = 4

    // Window set to 250ms (0.25 seconds)
    const val ENERGY_INTERVAL_MS = 250

    @Volatile
    private var activeSongId: String? = null

    @Volatile
    private var activeEnergyCurve: FloatArray? = null

    fun getCachedEnergy(songId: String): FloatArray? {
        return if (activeSongId == songId) activeEnergyCurve else null
    }

    fun clearCache() {
        activeSongId = null
        activeEnergyCurve = null
    }

    /**
     * Extracts audio energy curve progressively as a Flow, emitting updated FloatArrays
     * of buckets as soon as they become available during decoding.
     *
     * @param emitBatchSize How many buckets to accumulate before emitting a progressive snapshot.
     */
    fun extractEnergyFlow(
        context: Context,
        songId: String,
        uri: Uri,
        intervalMs: Int = ENERGY_INTERVAL_MS,
        emitBatchSize: Int = 4
    ): Flow<FloatArray> = flow {
        if (activeSongId == songId && activeEnergyCurve != null) {
            emit(activeEnergyCurve!!)
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
            } else 44100

            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val samplesPerBucket = max(1, (sampleRate * channelCount * (intervalMs / 1000f)).toInt())

            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("MIME type not found")
            val dec = MediaCodec.createDecoderByType(mime)
            decoder = dec
            dec.configure(format, null, null, 0)
            dec.start()

            val rawRmsList = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isDecoderEOS = false
            var isExtractorEOS = false

            var currentSquareSum = 0.0
            var currentSampleCount = 0

            while (!isDecoderEOS) {
                currentCoroutineContext().ensureActive()

                if (!isExtractorEOS) {
                    val inputBufferIndex = dec.dequeueInputBuffer(TIMEOUT_US)
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

                var outputBufferIndex = dec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                while (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isDecoderEOS = true
                    }
                    val outputBuffer = dec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val samples = byteBufferToFloats(outputBuffer, bufferInfo, format)
                        for (sample in samples) {
                            currentSquareSum += (sample * sample)
                            currentSampleCount++

                            if (currentSampleCount >= samplesPerBucket) {
                                val rms = sqrt(currentSquareSum / currentSampleCount).toFloat()
                                rawRmsList.add(rms)
                                currentSquareSum = 0.0
                                currentSampleCount = 0

                                if (rawRmsList.size == 1 || rawRmsList.size % emitBatchSize == 0) {
                                    emit(processRawRmsToEnergyCurve(rawRmsList))
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

            if (currentSampleCount > 0) {
                val rms = sqrt(currentSquareSum / currentSampleCount).toFloat()
                rawRmsList.add(rms)
            }

            val finalCurve = processRawRmsToEnergyCurve(rawRmsList)
            activeSongId = songId
            activeEnergyCurve = finalCurve
            emit(finalCurve)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun extractEnergy(
        context: Context,
        songId: String,
        uri: Uri,
        intervalMs: Int = ENERGY_INTERVAL_MS
    ): Result<FloatArray> = runCatching {
        extractEnergyFlow(context, songId, uri, intervalMs).last()
    }

    private fun processRawRmsToEnergyCurve(rawRmsList: List<Float>): FloatArray {
        if (rawRmsList.isEmpty()) {
            return FloatArray(1) { 0.5f }
        }

        var maxRms = 0.0001f
        var minRms = Float.MAX_VALUE
        for (rms in rawRmsList) {
            if (rms > maxRms) maxRms = rms
            if (rms < minRms) minRms = rms
        }

        val range = max(0.0001f, maxRms - minRms)
        val normalized = FloatArray(rawRmsList.size) { i ->
            val linearNorm = ((rawRmsList[i] - minRms) / range).coerceIn(0f, 1f)
            linearNorm.toDouble().pow(1.8).toFloat().coerceIn(0f, 1f)
        }

        // 3-point weighted moving average filter to eliminate frame jitter
        val smoothed = FloatArray(normalized.size)
        for (i in normalized.indices) {
            val prev = normalized.getOrElse(i - 1) { normalized[i] }
            val curr = normalized[i]
            val next = normalized.getOrElse(i + 1) { normalized[i] }
            smoothed[i] = (prev + 2f * curr + next) / 4f
        }

        return smoothed
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
}
