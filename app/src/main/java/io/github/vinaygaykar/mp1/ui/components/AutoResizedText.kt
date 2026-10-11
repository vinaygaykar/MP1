package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import kotlin.random.Random

val DefaultMultilineWeights = listOf(
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
)

val DefaultSinglelineWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
    FontWeight.Black,
)

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun AutoResizedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    multilineWeights: List<FontWeight> = DefaultMultilineWeights,
    singlelineWeights: List<FontWeight> = DefaultSinglelineWeights,
    textAlign: TextAlign? = null,
    maxLines: Int = 2,
    minFontSize: TextUnit = 18.sp,
    maxFontSize: TextUnit = 40.sp,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val maxBoundFontSize = if (!maxFontSize.isUnspecified && maxFontSize.isSp) maxFontSize else 40.sp
    val styleFontSize = if (!style.fontSize.isUnspecified && style.fontSize.isSp) style.fontSize else maxBoundFontSize
    val targetFontSize = if (maxBoundFontSize.value < styleFontSize.value) maxBoundFontSize else styleFontSize

    // Deterministically style text per track title using seeded Random so style stays constant while song plays
    val (isItalic, randomMultiLineWeight, randomSingleLineWeight) = remember(text, multilineWeights, singlelineWeights) {
        val rng = Random(text.hashCode())
        val multiLineWeight = if (multilineWeights.isNotEmpty()) {
            multilineWeights[rng.nextInt(multilineWeights.size)]
        } else {
            fontWeight ?: FontWeight.Normal
        }
        val singleLineWeight = if (singlelineWeights.isNotEmpty()) {
            singlelineWeights[rng.nextInt(singlelineWeights.size)]
        } else {
            fontWeight ?: FontWeight.Normal
        }
        Triple(false, multiLineWeight, singleLineWeight)
    }

    val fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
    val letterSpacing = (-0.03).em

    fun calculateLineHeight(candidateSp: Float, currentFontSizeSp: Float): TextUnit {
        return if (!style.lineHeight.isUnspecified && style.lineHeight.isSp && (currentFontSizeSp > 0f)) {
            (candidateSp * (style.lineHeight.value / currentFontSizeSp)).sp
        } else {
            (candidateSp * 1.15f).sp
        }
    }

    var resizedFontSize by remember(text, style, targetFontSize, minFontSize) { mutableStateOf(minFontSize) }
    var resolvedWeight by remember(text, style, targetFontSize, minFontSize, fontWeight) {
        mutableStateOf(fontWeight ?: randomSingleLineWeight)
    }
    var readyToDraw by remember(text, style, targetFontSize, minFontSize) { mutableStateOf(value = false) }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val maxHeightPx = with(density) { maxHeight.toPx() }.toInt()

        // Quantize width to 32px buckets to prevent continuous recalculations during drag/animations
        val stableWidthPx = if (maxWidthPx > 0) ((maxWidthPx + 15) / 32) * 32 else 0
        val hasHeight = maxHeightPx > 0

        LaunchedEffect(
            text, style, minFontSize, targetFontSize, maxLines,
            stableWidthPx, hasHeight, fontWeight,
            randomMultiLineWeight, randomSingleLineWeight,
        ) {
            if ((stableWidthPx <= 0) || (!hasHeight)) return@LaunchedEffect

            readyToDraw = false

            var lowerBoundSp = minFontSize.value
            val upperBoundCap = if (targetFontSize.value >= minFontSize.value) targetFontSize.value else minFontSize.value
            var upperBoundSp = upperBoundCap

            if (lowerBoundSp > (upperBoundSp + 0.01f)) {
                resizedFontSize = minFontSize
                resolvedWeight = fontWeight ?: randomSingleLineWeight
                readyToDraw = true
                return@LaunchedEffect
            }

            fun testFit(candidateSp: Float, isSingleLineWeight: Boolean): Boolean {
                val candidateFontSize = candidateSp.sp
                val candidateWeight = fontWeight ?: if (isSingleLineWeight) randomSingleLineWeight else randomMultiLineWeight
                val candidateLineHeight = calculateLineHeight(candidateSp, styleFontSize.value)

                val testStyle = style.copy(
                    fontSize = candidateFontSize,
                    lineHeight = candidateLineHeight,
                    letterSpacing = letterSpacing,
                    fontWeight = candidateWeight,
                    fontStyle = fontStyle,
                )

                val layoutResult = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = testStyle,
                    overflow = TextOverflow.Clip,
                    softWrap = maxLines > 1,
                    maxLines = maxLines,
                    constraints = Constraints(
                        maxWidth = stableWidthPx,
                        maxHeight = Constraints.Infinity,
                    ),
                )

                val fitsLineCount = layoutResult.lineCount <= maxLines
                val fitsOverflow = !layoutResult.hasVisualOverflow && !layoutResult.didOverflowWidth
                return fitsLineCount && fitsOverflow
            }

            var bestFitSp = lowerBoundSp

            // Check minFontSize fit first
            if (!testFit(lowerBoundSp, isSingleLineWeight = false)) {
                resizedFontSize = minFontSize
                resolvedWeight = fontWeight ?: randomMultiLineWeight
                readyToDraw = true
                return@LaunchedEffect
            }

            // Binary search for maximum fitting font size
            repeat(15) {
                if ((upperBoundSp - lowerBoundSp) < 0.1f) return@repeat

                val midSp = (lowerBoundSp + upperBoundSp) / 2f

                if (testFit(midSp, isSingleLineWeight = true) || testFit(midSp, isSingleLineWeight = false)) {
                    bestFitSp = midSp
                    lowerBoundSp = midSp + 0.01f
                } else {
                    upperBoundSp = midSp - 0.01f
                }
            }

            val finalFontSize = bestFitSp.sp
            val finalLineHeight = calculateLineHeight(bestFitSp, styleFontSize.value)

            // Determine line count with single-line weight at final size
            val singleLineTestStyle = style.copy(
                fontSize = finalFontSize,
                lineHeight = finalLineHeight,
                letterSpacing = letterSpacing,
                fontWeight = fontWeight ?: randomSingleLineWeight,
                fontStyle = fontStyle,
            )
            val finalSingleLineResult = textMeasurer.measure(
                text = AnnotatedString(text),
                style = singleLineTestStyle,
                overflow = TextOverflow.Clip,
                softWrap = maxLines > 1,
                maxLines = maxLines,
                constraints = Constraints(
                    maxWidth = stableWidthPx,
                    maxHeight = Constraints.Infinity,
                ),
            )

            val isSingleLine = (finalSingleLineResult.lineCount == 1) && (!finalSingleLineResult.hasVisualOverflow)

            resizedFontSize = finalFontSize
            resolvedWeight = fontWeight ?: if (isSingleLine) randomSingleLineWeight else randomMultiLineWeight
            readyToDraw = true
        }

        if (readyToDraw) {
            val finalLineHeight = calculateLineHeight(resizedFontSize.value, styleFontSize.value)
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                fontWeight = resolvedWeight,
                fontStyle = fontStyle,
                textAlign = textAlign,
                maxLines = maxLines,
                softWrap = maxLines > 1,
                overflow = overflow,
                style = style.copy(
                    fontSize = resizedFontSize,
                    lineHeight = finalLineHeight,
                    letterSpacing = letterSpacing,
                ),
            )
        }
    }
}
