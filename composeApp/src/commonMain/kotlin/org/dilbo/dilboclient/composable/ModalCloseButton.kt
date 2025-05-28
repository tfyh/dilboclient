package org.dilbo.dilboclient.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.dilbo.dilboclient.app.UIEventHandler
import org.dilbo.dilboclient.design.Theme

@Composable
fun ModalCloseButton() {
    Box (
        modifier = Modifier
            .width(Theme.dimensions.textFieldHeight)
            .height(Theme.dimensions.textFieldHeight)
    ) {
        Text(
            text = "⨯",
            color = Theme.colors.color_text_h1_h3,
            style = Theme.fonts.h3,
            modifier = Modifier
                .padding(Theme.dimensions.regularSpacing)
                .clickable {
                    Stage.viewModel.setVisibleModal(false)
                    UIEventHandler.getInstance().handleButtonEvent("modalClose")
                }
        )
    }
}