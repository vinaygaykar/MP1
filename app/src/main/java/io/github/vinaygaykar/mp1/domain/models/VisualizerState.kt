package io.github.vinaygaykar.mp1.domain.models

import androidx.compose.runtime.Immutable
import io.github.vinaygaykar.mp1.utils.FrequencyFrame

@Immutable
sealed interface VisualizerState {
    data object None : VisualizerState

    data class Energy(val curve: FloatArray) : VisualizerState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Energy
            return curve.contentEquals(other.curve)
        }

        override fun hashCode(): Int {
            return curve.contentHashCode()
        }
    }

    data class Frequency(val frames: Array<FrequencyFrame>) : VisualizerState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Frequency
            return frames.contentEquals(other.frames)
        }

        override fun hashCode(): Int {
            return frames.contentHashCode()
        }
    }
}
