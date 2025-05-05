package org.dilbo.dilboclient.composable

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.dilbo.dilboclient.design.Theme

@Composable
fun SubmitButton(
    text: String = "dilbo button",
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Theme.colors.color_background_form_button,
            contentColor = Theme.colors.color_text_form_button,
            disabledContainerColor = Theme.colors.color_background_form_button_hover,
            disabledContentColor = Theme.colors.color_text_form_button_hover
        ),
        shape = OutlinedTextFieldDefaults.shape
    ) {
        Text(
            text = text,
            style = Theme.fonts.p,
        )
    }
}