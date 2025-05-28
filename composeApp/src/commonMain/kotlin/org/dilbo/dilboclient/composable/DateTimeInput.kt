package org.dilbo.dilboclient.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Parser
import org.dilbo.dilboclient.tfyh.data.ParserConstraints
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.util.FormField
import org.dilbo.dilboclient.tfyh.util.I18n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeInput(formField: FormField) {

    val lang = Config.getInstance().language()
    val optionsIcon = Icons.Default.ArrowDropDown

    // prepare date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val preset = Parser.parse(formField.preset, ParserName.DATETIME, lang) as LocalDateTime
    val initialSelectedDateMillis =
        if (ParserConstraints.isEmpty(preset, ParserName.DATETIME)) null
        else LocalDateTime(preset.date, LocalTime(12, 0))
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)
    var selectedDate by remember { mutableStateOf(Formatter.format(preset.date, ParserName.DATE, lang)) }

    // prepare time picker
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = preset.time.hour,
        initialMinute = preset.time.minute
    )
    var selectedTime by remember {
        mutableStateOf(Formatter.format(timePickerState.hour * 3600 + timePickerState.minute * 60,
            ParserName.TIME).substring(0, 5))
    }

    // build input UI as date input + time input
    Row (
        modifier = Modifier.wrapContentSize()
    ) {

        // Date input
        Box(
            modifier = formField.boxModifier { showDatePicker = true }
        ) {
            OutlinedTextField(
                colors = formField.viewModel.textFieldColors(),
                shape = OutlinedTextFieldDefaults.shape,
                textStyle = Theme.fonts.p,
                singleLine = true,
                value = selectedDate,
                onValueChange = {
                    showDatePicker = false
                    selectedDate = it
                },
                label = { Text(
                    text = formField.label,
                    style = Theme.fonts.p,
                    color = formField.viewModel.labelColor()) },
                trailingIcon = {
                    if (! formField.viewModel.isReadOnly())
                        IconButton(
                            onClick = { showDatePicker = !showDatePicker }
                        ) {
                            Icon(
                                imageVector = optionsIcon,
                                contentDescription = "Select date"
                            )
                        }
                },
                modifier = Modifier
                    .width(formField.width * 0.55F)
                    .padding(top = Theme.dimensions.smallSpacing),
            )

            if (showDatePicker && !formField.viewModel.isReadOnly()) {
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
                                formField.entered = "$selectedDate $selectedTime"
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

        // Time input
        Box(
            modifier = formField.boxModifier { showTimePicker = true }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .width(formField.width * 0.45F)
                    .padding(top = Theme.dimensions.smallSpacing),
                colors = formField.viewModel.textFieldColors(),
                shape = OutlinedTextFieldDefaults.shape,
                textStyle = Theme.fonts.p,
                singleLine = true,
                value = selectedTime,
                onValueChange = {
                    showTimePicker = false
                    selectedTime = it
                },
                label = {
                    Text(text = I18n.getInstance().t("ZFaJj0|time"),
                    style = Theme.fonts.p,
                    color = formField.viewModel.labelColor()) },
                trailingIcon = {
                    if (!formField.viewModel.isReadOnly())
                        IconButton(
                            onClick = { showTimePicker = !showTimePicker },
                            modifier = Modifier.background(Color(0x000000))
                        ) {
                            Icon(
                                imageVector = optionsIcon,
                                contentDescription = "Select time",
                            )
                        }
                },
            )

            if (showTimePicker && !formField.viewModel.isReadOnly()) {
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
                                formField.entered = "$selectedDate $selectedTime"
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
}
