package io.github.vinaygaykar.mp1.domain.repository

import android.net.Uri
import io.github.vinaygaykar.mp1.utils.FrequencyFrame
import kotlinx.coroutines.flow.Flow

interface AudioVisualizerRepository {
    fun getAudioEnergyFlow(songId: String, uri: Uri): Flow<FloatArray>
    fun getAudioFrequencyFlow(songId: String, uri: Uri): Flow<Array<FrequencyFrame>>
    fun getCachedEnergy(songId: String): FloatArray?
    fun getCachedFrequency(songId: String): Array<FrequencyFrame>?
    fun clearCache()
}
