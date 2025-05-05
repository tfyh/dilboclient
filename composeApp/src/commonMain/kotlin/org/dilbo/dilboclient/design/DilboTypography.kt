package org.dilbo.dilboclient.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import dilboclient.composeapp.generated.resources.Res
import dilboclient.composeapp.generated.resources.prompt_italic
import dilboclient.composeapp.generated.resources.prompt_medium
import org.jetbrains.compose.resources.Font

data class DilboTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h5: TextStyle,
    val p: TextStyle,
)

// The provided typography must be wrapped into a
// composable function to be able to load the resources
@Composable
fun dilboTypography(): DilboTypography {
    val fontFamily = FontFamily (
        Font(Res.font.prompt_medium, weight = FontWeight.Normal),
        Font(Res.font.prompt_italic, weight = FontWeight.Normal)
    )
    // use the types as in the CSS of the web application.
    val rem = 16.sp
    return DilboTypography(
        h1 = TextStyle(
            fontSize = 2.25 * rem,
            fontWeight = FontWeight.Normal,
            fontFamily = fontFamily,
            color = Theme.colors.color_text_h1_h3,
        ),
        h2 = TextStyle(
            fontSize = 1.875 * rem,
            fontWeight = FontWeight.Normal,
            fontFamily = fontFamily,
            color = Theme.colors.color_text_h1_h3,
        ),
        h3 = TextStyle(
            fontSize = 1.5 * rem,
            fontWeight = FontWeight.Normal,
            fontFamily = fontFamily,
            color = Theme.colors.color_text_h1_h3,
        ),
        h4 = TextStyle(
            fontSize = 1.25 * rem,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            color = Theme.colors.color_text_p_h4_h6_etc,
        ),
        h5 = TextStyle(
            fontSize = 1.125 * rem,
            fontWeight = FontWeight.Normal,
            fontFamily = fontFamily,
            color = Theme.colors.color_text_p_h4_h6_etc,
        ),
        p = TextStyle(
            fontSize = rem,
            fontWeight = FontWeight.Thin,
            fontFamily = fontFamily,
            color = Theme.colors.color_text_p_h4_h6_etc,
        ),
    )
}

val localAppTypography = staticCompositionLocalOf {
    DilboTypography(
        h1 = TextStyle.Default,
        h2 = TextStyle.Default,
        h3 = TextStyle.Default,
        h4 = TextStyle.Default,
        h5 = TextStyle.Default,
        p = TextStyle.Default
    )
}
