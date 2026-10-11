package io.github.vinaygaykar.mp1.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DynamicThemeHelper {

    suspend fun extractSeedColor(context: Context, model: Any?): Color? {
        if (model == null) return null
        if (model is String && model.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(model)
                    .allowHardware(false)
                    .size(128, 128)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        extractSeedColorFromBitmap(bitmap)
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun extractSeedColorFromBitmap(bitmap: Bitmap): Color? {
        val palette = Palette.from(bitmap)
            .clearFilters()
            .maximumColorCount(24)
            .generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
        return swatch?.let { Color(it.rgb) }
    }

    fun createDynamicColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
        val argb = seedColor.toArgb()
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)

        val hue = hsl[0]
        val saturation = hsl[1]

        // Grayscale threshold: seeds with < 8% saturation are treated as pure grayscale.
        // Higher saturations retain their natural chroma smoothly.
        val finalSaturation = if (saturation < 0.08f) 0.0f else saturation.coerceIn(0f, 1f)

        return if (isDark) {
            createDarkScheme(hue, finalSaturation)
        } else {
            createLightScheme(hue, finalSaturation)
        }
    }

    private fun createLightScheme(hue: Float, saturation: Float): ColorScheme {
        val isGrayscale = saturation == 0.0f

        val secondarySat = if (isGrayscale) 0.0f else (saturation * 0.5f)
        val tertiarySat = if (isGrayscale) 0.0f else (saturation * 0.75f)
        val surfaceSat = if (isGrayscale) 0.0f else (saturation * 0.08f)

        val tertiaryHue = if (isGrayscale) hue else (hue + 60f) % 360f

        val primary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, 0.4f)))
        val surface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.98f)))

        // Fully Dynamic onPrimary, onSecondary, onTertiary with hue tinting
        val isBrightHue = isLightHue(hue)
        val onPrimary = Color(
            ColorUtils.HSLToColor(
                floatArrayOf(hue, saturation, if (isGrayscale || isBrightHue) 0.05f else 0.98f)
            )
        )
        val onSecondary = Color(
            ColorUtils.HSLToColor(
                floatArrayOf(hue, secondarySat, if (isGrayscale || isBrightHue) 0.05f else 0.98f)
            )
        )
        val onTertiary = Color(
            ColorUtils.HSLToColor(
                floatArrayOf(tertiaryHue, tertiarySat, if (isGrayscale || isLightHue(tertiaryHue)) 0.05f else 0.98f)
            )
        )

        return lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, 0.9f))),
            onPrimaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, 0.1f))),
            inversePrimary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, 0.8f))),

            secondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.4f))),
            onSecondary = onSecondary,
            secondaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.9f))),
            onSecondaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.1f))),

            tertiary = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.45f))),
            onTertiary = onTertiary,
            tertiaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.9f))),
            onTertiaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.1f))),

            background = surface,
            onBackground = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.1f))),

            surface = surface,
            onSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.1f))),
            surfaceDim = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.87f))),
            surfaceBright = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.98f))),
            surfaceContainerLowest = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 1.0f))),
            surfaceContainerLow = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.96f))),
            surfaceContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.92f))),
            surfaceContainerHigh = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.88f))),
            surfaceContainerHighest = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.84f))),

            surfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.9f))),
            onSurfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.3f))),
            inverseSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.2f))),
            inverseOnSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.95f))),

            outline = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.5f))),
            outlineVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.8f))),
            scrim = Color.Black,

            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }

    private fun createDarkScheme(hue: Float, saturation: Float): ColorScheme {
        val isGrayscale = saturation == 0.0f

        val darkSat = if (isGrayscale) 0.0f else (saturation * 0.8f).coerceIn(0f, 1f)
        val secondarySat = if (isGrayscale) 0.0f else darkSat * 0.5f
        val tertiarySat = if (isGrayscale) 0.0f else darkSat * 0.75f
        val surfaceSat = if (isGrayscale) 0.0f else darkSat * 0.1f

        val tertiaryHue = if (isGrayscale) hue else (hue + 60f) % 360f

        val primary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, darkSat, 0.8f)))
        val surface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.08f)))

        return darkColorScheme(
            primary = primary,
            onPrimary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, darkSat, 0.15f))),
            primaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, darkSat, 0.22f))),
            onPrimaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, darkSat, 0.95f))),
            inversePrimary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, darkSat, 0.4f))),

            secondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.8f))),
            onSecondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.15f))),
            secondaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.25f))),
            onSecondaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, secondarySat, 0.95f))),

            tertiary = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.8f))),
            onTertiary = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.15f))),
            tertiaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.25f))),
            onTertiaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(tertiaryHue, tertiarySat, 0.95f))),

            background = surface,
            onBackground = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.9f))),

            surface = surface,
            onSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.9f))),
            surfaceDim = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.06f))),
            surfaceBright = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.24f))),
            surfaceContainerLowest = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.04f))),
            surfaceContainerLow = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.08f))),
            surfaceContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.12f))),
            surfaceContainerHigh = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.16f))),
            surfaceContainerHighest = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.22f))),

            surfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.25f))),
            onSurfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.8f))),
            inverseSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.9f))),
            inverseOnSurface = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat, 0.1f))),

            outline = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.6f))),
            outlineVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, surfaceSat * 1.5f, 0.3f))),
            scrim = Color.Black,

            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    }

    private fun isLightHue(hue: Float): Boolean {
        // Yellows, limes, and cyans (45° to 190°) have higher perceived brightness
        return hue in 45f..190f
    }
}