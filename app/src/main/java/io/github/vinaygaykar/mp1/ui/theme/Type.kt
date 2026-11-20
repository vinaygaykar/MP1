package io.github.vinaygaykar.mp1.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.vinaygaykar.mp1.R
import kotlinx.coroutines.flow.MutableStateFlow

// Define the default FontFamily using the local resource
val googleSansFlexFontFamily = FontFamily(
    Font(R.font.googlesans_flex)
)

enum class AppFont(val displayName: String, val fontFamily: FontFamily) {
    GOOGLE_SANS("Google Sans Flex", googleSansFlexFontFamily),
    ANTON("Anton", FontFamily(Font(R.font.anton_regular))),
    BAGEL_FAT_ONE("Bagel Fat One", FontFamily(Font(R.font.bagel_fat_one_regular))),
    BBH_BARTLE("BBH Bartle", FontFamily(Font(R.font.bbh_bartle_regular))),
    BBH_HEGARTY("BBH Hegarty", FontFamily(Font(R.font.bbh_hegarty_regular))),
    BOLDONSE("Boldonse", FontFamily(Font(R.font.boldonse_regular))),
    BOWLBY_ONE_SC("Bowlby One SC", FontFamily(Font(R.font.bowlby_one_sc_regular))),
    CAL_SANS("Cal Sans", FontFamily(Font(R.font.calsans_regular))),
    CLIMATE_CRISIS("Climate Crisis", FontFamily(Font(R.font.climate_crisis_regular))),
    DELA_GOTHIC_ONE("Dela Gothic One", FontFamily(Font(R.font.dela_gothic_one_regular))),
    DM_SERIF("DM Serif Text", FontFamily(Font(R.font.dmseriftext_regular))),
    GASOEK_ONE("Gasoek One", FontFamily(Font(R.font.gasoek_one_regular))),
    KANIT(
        "Kanit",
        FontFamily(
            Font(R.font.kanit_regular, FontWeight.Normal),
            Font(R.font.kanit_medium, FontWeight.Medium),
            Font(R.font.kanit_bold, FontWeight.Bold)
        )
    ),
    PASSION_ONE("Passion One", FontFamily(Font(R.font.passion_one_regular))),
    RADIO_CANADA_BIG("Radio Canada Big", FontFamily(Font(R.font.radio_canada_big_variable))),
    READEX_PRO("Readex Pro", FontFamily(Font(R.font.readex_pro_variable))),
    SPECIAL_GOTHIC("Special Gothic Expanded", FontFamily(Font(R.font.special_gothic_expanded_one_regular)))
}

data class TypographyFontConfig(
    val displayFont: AppFont = AppFont.GOOGLE_SANS,
    val headlineFont: AppFont = AppFont.GOOGLE_SANS,
    val titleFont: AppFont = AppFont.GOOGLE_SANS,
    val bodyFont: AppFont = AppFont.GOOGLE_SANS
)

// Global state flow for developer previewing font changes at runtime across 4 typography categories
val CurrentTypographyFonts = MutableStateFlow(TypographyFontConfig())

fun getTypographyForFonts(
    displayFont: FontFamily,
    headlineFont: FontFamily,
    titleFont: FontFamily,
    bodyFont: FontFamily
): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = titleFont,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = titleFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

fun getTypographyForFont(fontFamily: FontFamily): Typography {
    return getTypographyForFonts(fontFamily, fontFamily, fontFamily, fontFamily)
}

// Fallback typography for Preview mode to avoid font loading errors in Layoutlib
val PreviewTypography = getTypographyForFont(FontFamily.Default)

// Set up Material 3 typography styles using Google Sans Flex by default
val Typography = getTypographyForFont(googleSansFlexFontFamily)

