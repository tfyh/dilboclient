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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Popup
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.util.FormField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TimeInput(formField: FormField) {
    val optionsIcon = Icons.Default.ArrowDropDown
    var showTimePicker by remember { mutableStateOf(false) }
    val dateTimeNow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val timePickerState = rememberTimePickerState(
        initialHour = dateTimeNow.hour,
        initialMinute = dateTimeNow.minute
    )
    var selectedTime by remember {
        mutableStateOf(Formatter.format(timePickerState.hour * 3600 + timePickerState.minute * 60,
            ParserName.TIME).substring(0, 5))
    }

    Box(
        modifier = formField.boxModifier { showTimePicker = true }
    ) {
        val lang = Config.getInstance().language()
        OutlinedTextField(
            modifier = Modifier
                .width(formField.width)
                .padding(top = Theme.dimensions.smallSpacing),
            colors = Theme.colors.textFieldColors(),
            shape = OutlinedTextFieldDefaults.shape,
            textStyle = Theme.fonts.p,
            singleLine = true,
            value = selectedTime,
            onValueChange = {
                showTimePicker = false
                selectedTime = it
            },
            label = { Text(text = formField.label, style = Theme.fonts.p, color = Theme.colors.color_text_h1_h3) },
            trailingIcon = {
                IconButton(onClick = { showTimePicker = !showTimePicker }) {
                    Icon(
                        imageVector = optionsIcon,
                        contentDescription = "Select time",
                    )
                }
            },
        )

        if (showTimePicker) {
            Popup(
                onDismissRequest = { showTimePicker = false },
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
                            selectedTime = Formatter.format(
                                (timePickerState.hour * 3600 + timePickerState.minute * 60),
                                ParserName.TIME, lang)
                                .substring(0, 5)
                            showTimePicker = false
                        }
                    }
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier
                            .padding(Theme.dimensions.smallSpacing)
                            .width(Theme.dimensions.pickerSize),
                        colors = Theme.colors.timePickerColors(),
                        layoutType = TimePickerLayoutType.Vertical
                    )
                }
            }
        }
    }
}
