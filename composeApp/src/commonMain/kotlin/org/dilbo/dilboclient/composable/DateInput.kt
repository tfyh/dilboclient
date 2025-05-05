package org.dilbo.dilboclient.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Popup
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Parser
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.util.FormField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DateInput(formField: FormField) {
    val lang = Config.getInstance().language()
    val optionsIcon = Icons.Default.ArrowDropDown
    val preset = Parser.parse(formField.preset.substring(0, 10), ParserName.DATE, lang) as LocalDate
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDateTime(preset, LocalTime(12, 0))
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    )
    var selectedDate by remember { mutableStateOf(Formatter.format(preset, ParserName.DATE, lang)) }

    Box(
        modifier = formField.boxModifier { showDatePicker = true }
    ) {
        OutlinedTextField(
            colors = Theme.colors.textFieldColors(),
            shape = OutlinedTextFieldDefaults.shape,
            textStyle = Theme.fonts.p,
            singleLine = true,
            value = selectedDate,
            onValueChange = {
                showDatePicker = false
                selectedDate = it
            },
            label = { Text(text = formField.label, style = Theme.fonts.p, color = Theme.colors.color_text_h1_h3) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(
                        imageVector = optionsIcon,
                        contentDescription = "Select date"
                    )
                }
            },
            modifier = Modifier
                .width(formField.width)
                .padding(top = Theme.dimensions.smallSpacing)
                .onFocusChanged { formField.onFocusChanged(it.isFocused) },
        )

        if (showDatePicker) {
            Popup(
                onDismissRequest = { showDatePicker = false },
                alignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .offset(y = Theme.dimensions.textFieldHeight)
                        .shadow(elevation = Theme.dimensions.smallSpacing)
                        .background(Theme.colors.color_background_form_input)
                        .padding(Theme.dimensions.smallSpacing)
                ) {
                    Box (
                        modifier = Modifier.width(Theme.dimensions.pickerSize),
                        contentAlignment = Alignment.TopEnd
                    )  {
                        SubmitButton("Ok") {
                            selectedDate = Formatter.microTimeToString(
                                (datePickerState.selectedDateMillis ?: 0L) / 1000.0, lang)
                                .substring(0, 10)
                            showDatePicker = false
                        }
                    }
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier
                            .padding(Theme.dimensions.smallSpacing)
                            .width(Theme.dimensions.pickerSize),
                        colors = Theme.colors.datePickerColors(),
                        title = { DilboLabel(formField.label) },
                        showModeToggle = false
                    )
                }
            }
        }
    }
}
