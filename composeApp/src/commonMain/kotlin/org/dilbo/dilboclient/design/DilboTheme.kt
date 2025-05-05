package org.dilbo.dilboclient.design
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// see https://www.youtube.com/watch?v=eTVVT9cX4Bw
// The structure has been changed in the way, that the design system is split into its parts.

@Composable
fun DilboTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val typography = dilboTypography()
    val colorScheme = if (isDark) dilboDarkColorScheme else dilboLightColorScheme
    val shapes = dilboShapes
    val dimensions = dilboDimensions
    // val localIndication = rememberRipple() this may be something not relevant in Compose Multiplatform
    CompositionLocalProvider(
        dilboAppColorScheme provides colorScheme,
        localAppTypography provides typography,
        localAppShape provides shapes,
        dilboAppDimensions provides dimensions,
        /// localIndication provides rippleIndication (see above)
        content = content
    )
}

object Theme {
    val colors: DilboColorScheme
        @Composable get() = dilboAppColorScheme.current
    val fonts: DilboTypography
        @Composable get() = localAppTypography.current
    val shapes: DilboShapes
        @Composable get() = localAppShape.current
    val dimensions: DilboDimensions
        @Composable get() = dilboAppDimensions.current
}