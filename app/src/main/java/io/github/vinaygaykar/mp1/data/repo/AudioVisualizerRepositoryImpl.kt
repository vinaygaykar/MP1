package io.github.vinaygaykar.mp1.data.repo

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaygaykar.mp1.domain.repository.AudioVisualizerRepository
import io.github.vinaygaykar.mp1.utils.AudioEnergyExtractor
import io.github.vinaygaykar.mp1.utils.AudioFrequencyExtractor
import io.github.vinaygaykar.mp1.utils.FrequencyFrame
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioVisualizerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioVisualizerRepository {

    override fun getAudioEnergyFlow(songId: String, uri: Uri): Flow<FloatArray> {
        return AudioEnergyExtractor.extractEnergyFlow(context, songId, uri)
    }

    override fun getAudioFrequencyFlow(songId: String, uri: Uri): Flow<Array<FrequencyFrame>> {
        return AudioFrequencyExtractor.extractFrequencyFlow(context, songId, uri)
    }

    override fun getCachedEnergy(songId: String): FloatArray? {
        return AudioEnergyExtractor.getCachedEnergy(songId)
    }

    override fun getCachedFrequency(songId: String): Array<FrequencyFrame>? {
        return AudioFrequencyExtractor.getCachedFrequency(songId)
    }

    override fun clearCache() {
        AudioEnergyExtractor.clearCache()
        AudioFrequencyExtractor.clearCache()
    }
}
