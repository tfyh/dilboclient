package org.dilbo.dilboclient.design

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DilboDimensions(
    val smallSpacing: Dp,
    val regularSpacing: Dp,
    val largeSpacing: Dp,
    val textFieldHeight: Dp = OutlinedTextFieldDefaults.MinHeight,
    val pickerSize: Dp = 400.dp
)

val dilboDimensions = DilboDimensions(
    smallSpacing = 3.dp,
    regularSpacing = 12.dp,
    largeSpacing = 20.dp
)

val dilboAppDimensions = staticCompositionLocalOf {
    DilboDimensions(
        smallSpacing = 0.dp,
        regularSpacing = 0.dp,
        largeSpacing = 0.dp
    )
}
