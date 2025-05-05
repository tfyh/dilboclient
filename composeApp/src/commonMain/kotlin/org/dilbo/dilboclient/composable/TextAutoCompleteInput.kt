package org.dilbo.dilboclient.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.util.FormField

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextAutoCompleteInput(formField: FormField) {
    // see https://stackoverflow.com/questions/67111020/exposed-drop-down-menu-for-jetpack-compose/67111599#67111599
    var expanded by remember { mutableStateOf(false) }
    var typedTextLength by remember { mutableStateOf(0) }
    var dropDownWidth by remember { mutableStateOf(0) }
    val options: MutableList<String> = formField.options.keys.toMutableList()
    val icon = if (formField.viewModel.entered.isEmpty())
            Icons.Default.ArrowDropDown
        else if (options.contains(formField.viewModel.entered.trim()))
            Icons.Filled.CheckCircle
        else if (formField.isMatchingFragment(formField.entered, false))
            Icons.Default.ArrowDropDown
        else Icons.Outlined.Warning

    Box (
        modifier = formField.boxModifier { expanded = true }
    ) {
        OutlinedTextField(
            colors = Theme.colors.textFieldColors(),
            shape = OutlinedTextFieldDefaults.shape,
            textStyle = Theme.fonts.p,
            singleLine = true,
            onValueChange = {
                typedTextLength++
                formField.filterOptions(options, it, false, 2)
                formField.entered =
                    if (options.size == 1)
                        options.first()
                    else if (it.length >= typedTextLength)
                        it.substring(0, typedTextLength)
                    else it
                formField.viewModel.entered = formField.entered
            },
            value = formField.viewModel.entered.ifEmpty { formField.preset },
            modifier = Modifier
                .width(formField.width)
                .onSizeChanged { dropDownWidth = it.width }
                .padding(top = Theme.dimensions.smallSpacing)
                .onFocusChanged {
                    formField.onFocusChanged(it.isFocused) },
            label = { Text(text = formField.label, style = Theme.fonts.p, color = Theme.colors.color_text_h1_h3) },
            trailingIcon = {
                if (formField.viewModel.entered.isNotEmpty())
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Show selections",
                        )
                }
            },
        )

        formField.filterOptions(options, formField.entered, false, 12)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current){ dropDownWidth.toDp() })
                .height(Theme.dimensions.textFieldHeight * 2)
                .background(Theme.colors.color_background_form_input)
        ) {
            options.forEach { label ->
                DropdownMenuItem(
                    modifier = Modifier.height(Theme.dimensions.textFieldHeight / 2),
                    onClick = {
                        formField.entered = label.trim()
                        formField.viewModel.entered = formField.entered
                        expanded = false
                    },
                    colors = MenuItemColors(
                        textColor = Theme.colors.color_text_form_input,
                        leadingIconColor = Theme.colors.color_text_form_input,
                        trailingIconColor = Theme.colors.color_text_form_input,
                        disabledTextColor = Theme.colors.color_text_form_input_hover,
                        disabledLeadingIconColor = Theme.colors.color_text_form_input_hover,
                        disabledTrailingIconColor = Theme.colors.color_text_form_input_hover,
                    ),
                    // text assignment is not tested, different than in the stackoverflow suggestion.
                    text = { DilboLabel(label.trim()) }
                )
            }
        }
    }
}
