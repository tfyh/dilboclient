package org.dilbo.dilboclient.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.util.FormField

@Composable
fun TextAnyInput(
    formField: FormField,
    singleLine: Boolean = true
) {
    val hideText = formField.type.name() == "password"
    Box(
        modifier = formField.boxModifier {  }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .width(formField.width)
                .padding(top = Theme.dimensions.smallSpacing),
            colors =
                if (formField.modifierChar == "!") Theme.colors.textDisplayColors()
                else Theme.colors.textFieldColors(),
            enabled = (formField.modifierChar != "!"),
            shape = OutlinedTextFieldDefaults.shape,
            textStyle = Theme.fonts.p,
            value = formField.viewModel.entered.ifEmpty { formField.preset },
            visualTransformation = if (hideText) PasswordVisualTransformation() else VisualTransformation.None,
            onValueChange = {
                formField.viewModel.entered = it
                formField.entered = it
            },
            singleLine = singleLine,
            label = { Text(text = formField.label, style = Theme.fonts.p, color = Theme.colors.color_text_h1_h3) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                // keyboardType = formField.keyboardType(),
            )
        )
    }
}



