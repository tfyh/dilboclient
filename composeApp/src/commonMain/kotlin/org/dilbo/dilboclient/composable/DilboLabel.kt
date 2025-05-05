package org.dilbo.dilboclient.composable

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import org.dilbo.dilboclient.design.Theme

@Composable
fun DilboLabel(
    text: String,
    large: Boolean = false,
    color: Color = Theme.colors.color_text_body,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier.wrapContentSize()

) {
    Text(
        StyledText.toAnnotatedString(text),
        color = color,
        style = if (large) Theme.fonts.h4 else Theme.fonts.h5,
        textAlign = textAlign,
        modifier = modifier
    )
}