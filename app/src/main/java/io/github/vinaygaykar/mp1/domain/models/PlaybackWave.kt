package io.github.vinaygaykar.mp1.domain.models

enum class PlaybackWave(val displayName: String) {
    SIMPLICITY("Simplicity"),
    ENERGY("Energy"),
    FREQUENCY("Frequency");

    companion object {
        fun fromName(name: String?): PlaybackWave {
            return entries.find {
                it.name.equals(name, ignoreCase = true) ||
                it.displayName.equals(name, ignoreCase = true)
            } ?: SIMPLICITY
        }
    }
}
