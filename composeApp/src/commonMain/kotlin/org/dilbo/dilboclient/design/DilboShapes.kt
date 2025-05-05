package org.dilbo.dilboclient.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

data class DilboShapes(
    val container: Shape,
    val button: Shape,
    // val inputField: Shape
)

val dilboShapes = DilboShapes(
    container = RectangleShape,
    button = RectangleShape
)

val localAppShape = staticCompositionLocalOf {
    DilboShapes(
        container = RectangleShape,
        button = RectangleShape
    )
}

