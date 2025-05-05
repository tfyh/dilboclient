package org.dilbo.dilboclient.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.util.FormField

// from https://gist.github.com/snicmakino/297d34e429c078624fde6771064ed6d2
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextSelectInput(formField: FormField) {

    val expanded = remember { mutableStateOf(false) }
    val isFocused = remember { mutableStateOf(false) }
    val optionsIcon = Icons.Default.ArrowDropDown

    val selectedOption = remember { mutableStateOf(
        if (formField.options.isEmpty()) "???"
        else formField.options.keys.toList().getOrNull(0))
    }
    val language = Config.getInstance().language()

    Column(
        modifier = formField.boxModifier { expanded.value = true }
    ) {
        Spacer(modifier = Modifier.padding(top = Theme.fonts.p.fontSize.value.dp / 2 + 3.dp))
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .width(formField.width)
                .height(Theme.dimensions.textFieldHeight)
                .clip(RoundedCornerShape(4.dp))
                .border(if (isFocused.value)
                            BorderStroke(2.dp, Theme.colors.color_border_form_input)
                        else
                            BorderStroke(1.dp, Theme.colors.color_border_form_input),
                    RoundedCornerShape(Theme.dimensions.smallSpacing))
                .onFocusChanged {
                    isFocused.value = it.isFocused
                }
                .clickable { expanded.value = !expanded.value }
                .background(if (isFocused.value)
                                Theme.colors.color_background_form_input_hover
                            else
                                Theme.colors.color_background_form_input
                    )
                .padding(OutlinedTextFieldDefaults.contentPadding()),
        ) {
            val selectedOptionText = formField.options[selectedOption.value]
            if (selectedOptionText != null)
                Text(
                    text = Formatter.format(selectedOptionText, formField.type.parser(), language),
                    style = Theme.fonts.p
                )
            Icon(
                optionsIcon, "dropDown",
                Modifier.align(Alignment.CenterEnd)
            )
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                formField.options.keys.toList().forEach { selectOption ->
                    DropdownMenuItem(
                        modifier = Modifier.height(Theme.dimensions.textFieldHeight / 2),
                        onClick = {
                            selectedOption.value = selectOption
                            expanded.value = false
                        }
                    ) {
                        val selectOptionText = formField.options[selectOption]
                        if (selectOptionText != null)
                            Text(
                                text = Formatter.format(selectOptionText, formField.type.parser(), language),
                                style = Theme.fonts.p
                            )
                    }
                }
            }
        }
    }
}