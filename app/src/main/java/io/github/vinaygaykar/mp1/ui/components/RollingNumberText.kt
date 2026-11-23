package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private data class RollingDigitState(
    val char: Char,
    val totalCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RollingDigitState) return false
        return char == other.char
    }

    override fun hashCode(): Int {
        return char.hashCode()
    }
}

/**
 * Reusable component that displays an integer count with rolling digit animations
 * (odometer-style per place value: units, tens, hundreds, etc.).
 * Incrementing count slides digits up; decrementing slides digits down.
 */
@Composable
fun RollingNumberText(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified
) {
    val countString = count.toString()

    Row(
        modifier = modifier.animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        countString.forEachIndexed { index, char ->
            val digitPlaceValue = countString.length - 1 - index

            key(digitPlaceValue) {
                AnimatedContent(
                    targetState = RollingDigitState(char, count),
                    transitionSpec = {
                        val isIncrement = targetState.totalCount >= initialState.totalCount
                        if (isIncrement) {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                        }
                    },
                    label = "rolling_digit_$digitPlaceValue"
                ) { digitState ->
                    Text(
                        text = digitState.char.toString(),
                        style = style,
                        fontWeight = fontWeight,
                        color = color
                    )
                }
            }
        }
    }
}
