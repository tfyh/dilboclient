package org.dilbo.dilboclient.design

import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePickerColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class DilboColorScheme (
    // body & modal
    val color_text_body: Color,
    val color_background_body: Color,
    val color_background_modal: Color,
    val color_background_bar: Color,
    val color_text_h1_h3: Color,
    val color_text_p_h4_h6_etc: Color,
    val color_text_link: Color,
    val color_text_link_hover_visited: Color,
    // menu
    val color_background_menubar: Color,
    val color_text_menuitem: Color,
    val color_background_menuitem: Color,
    val color_text_menuitem_hover: Color,
    val color_background_menuitem_hover: Color,
    // formButton
    val color_text_form_button: Color,
    val color_background_form_button: Color,
    val color_text_form_button_hover: Color,
    val color_background_form_button_hover: Color,
    // formInput
    val color_text_form_input: Color,
    val color_background_form_input: Color,
    val color_border_form_input: Color,
    val color_border_autocomplete_items: Color,
    val color_text_form_input_hover: Color,
    val color_background_form_input_hover: Color,
    val color_background_checkbox_radio_checked: Color,
    // table
    val color_text_table_header: Color,
    val color_background_table_header: Color,
    val color_background_table_even_rows: Color,
    val color_background_table_row_hover: Color,
    val color_border_table : Color) {

    @Composable
    fun textFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = color_text_form_input,
        unfocusedTextColor = color_text_form_input,
        disabledTextColor = color_text_body,
        focusedContainerColor = color_background_form_input_hover,
        unfocusedContainerColor = color_background_form_input,
        focusedLabelColor = color_text_form_input,
        unfocusedLabelColor = color_text_form_input,
        focusedBorderColor = color_border_form_input,
        unfocusedBorderColor = color_border_form_input
    )

    @Composable
    fun textDisplayColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = color_text_form_input,
        unfocusedTextColor = color_text_form_input,
        disabledTextColor = color_text_form_input,
        focusedContainerColor = Color(0x00000000),
        unfocusedContainerColor = Color(0x00000000),
        focusedLabelColor = Color(0x00000000),
        unfocusedLabelColor = Color(0x00000000),
        focusedBorderColor = Color(0x00000000),
        unfocusedBorderColor = Color(0x00000000),
    )


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun datePickerColors() = DatePickerColors(
        containerColor = color_background_form_input,
        titleContentColor = color_border_form_input,
        headlineContentColor = color_border_form_input,
        weekdayContentColor = color_text_h1_h3,
        subheadContentColor = color_text_h1_h3,
        navigationContentColor = color_text_body,
        yearContentColor = color_text_body,
        disabledYearContentColor = color_text_p_h4_h6_etc,
        currentYearContentColor = color_text_body,
        selectedYearContentColor = color_text_form_button,
        disabledSelectedYearContentColor = color_text_p_h4_h6_etc,
        selectedYearContainerColor = color_background_form_button,
        disabledSelectedYearContainerColor = color_background_form_input,
        dayContentColor = color_text_body,
        disabledDayContentColor = color_text_p_h4_h6_etc,
        selectedDayContentColor = color_text_form_button,
        disabledSelectedDayContentColor = color_text_p_h4_h6_etc,
        selectedDayContainerColor = color_background_form_button,
        disabledSelectedDayContainerColor = color_background_form_input,
        todayContentColor = color_text_body,
        todayDateBorderColor = color_border_table,
        dayInSelectionRangeContainerColor = color_background_form_input,
        dayInSelectionRangeContentColor = color_text_p_h4_h6_etc,
        dividerColor = color_border_form_input,
        dateTextFieldColors = textFieldColors()
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun timePickerColors() = TimePickerColors(
        clockDialColor = color_background_form_input,
        selectorColor = color_background_form_button,
        containerColor = color_background_modal,
        periodSelectorBorderColor = color_border_table,
        clockDialSelectedContentColor = color_text_h1_h3,
        clockDialUnselectedContentColor = color_text_p_h4_h6_etc,
        periodSelectorSelectedContainerColor = color_background_form_input,
        periodSelectorUnselectedContainerColor = color_background_modal,
        periodSelectorSelectedContentColor = color_text_h1_h3,
        periodSelectorUnselectedContentColor = color_text_p_h4_h6_etc,
        timeSelectorSelectedContainerColor = color_background_form_input,
        timeSelectorUnselectedContainerColor = color_background_modal,
        timeSelectorSelectedContentColor = color_text_h1_h3,
        timeSelectorUnselectedContentColor = color_text_p_h4_h6_etc,
    )
}

val dilboAppColorScheme = staticCompositionLocalOf {
    DilboColorScheme(
        // body & modal
        color_text_body = Color.Unspecified,
        color_background_body = Color.Unspecified,
        color_background_modal = Color.Unspecified,
        color_background_bar = Color.Unspecified,
        color_text_h1_h3 = Color.Unspecified,
        color_text_p_h4_h6_etc = Color.Unspecified,
        color_text_link = Color.Unspecified,
        color_text_link_hover_visited = Color.Unspecified,
        // menu
        color_background_menubar = Color.Unspecified,
        color_text_menuitem = Color.Unspecified,
        color_background_menuitem = Color.Unspecified,
        color_text_menuitem_hover = Color.Unspecified,
        color_background_menuitem_hover = Color.Unspecified,
        // formButton
        color_text_form_button = Color.Unspecified,
        color_background_form_button = Color.Unspecified,
        color_text_form_button_hover = Color.Unspecified,
        color_background_form_button_hover = Color.Unspecified,
        // formInput
        color_text_form_input = Color.Unspecified,
        color_background_form_input = Color.Unspecified,
        color_border_form_input = Color.Unspecified,
        color_border_autocomplete_items = Color.Unspecified,
        color_text_form_input_hover = Color.Unspecified,
        color_background_form_input_hover = Color.Unspecified,
        color_background_checkbox_radio_checked = Color.Unspecified,
        // table
        color_text_table_header = Color.Unspecified,
        color_background_table_header = Color.Unspecified,
        color_background_table_even_rows = Color.Unspecified,
        color_background_table_row_hover = Color.Unspecified,
        color_border_table = Color.Unspecified
    )
}

// dilbo color set, agnostic of usage.
val textColor = Color(0xff1c1d1f)
val bodyBackground = Color(0xfff0f1f4)
val evenRowsBackground  = Color(0xfff6f6f8)
val barBackground = Color(0xfff0f2f9)
val textBlue = Color(0xff033161)
val accentBlue = Color(0xff2d7ebe)
val darkBlue = Color(0xff0063a0)
val darkGray = Color(0xff3a3b3f)
val lightBlue = Color(0xffcce0f1)

// This is currently the very same color scheme than for the light mode.
val dilboDarkColorScheme = DilboColorScheme(
// body & modal
    color_text_body = textColor,
    color_background_body = bodyBackground,
    color_background_modal = evenRowsBackground,
    color_background_bar = barBackground,
    color_text_h1_h3 = textBlue,
    color_text_p_h4_h6_etc = textColor,
    color_text_link = textBlue,
    color_text_link_hover_visited = darkBlue,
// menu
    color_background_menubar = Color.White,
    color_text_menuitem = textBlue,
    color_background_menuitem = Color.White,
    color_text_menuitem_hover = Color.White,
    color_background_menuitem_hover = darkGray,
// formbutton
    color_text_form_button = bodyBackground,
    color_background_form_button = textBlue,
    color_text_form_button_hover = bodyBackground,
    color_background_form_button_hover = darkGray,
// formInput
    color_text_form_input = textColor,
    color_background_form_input = Color.White,
    color_border_form_input = accentBlue,
    color_border_autocomplete_items = accentBlue,
    color_text_form_input_hover = textColor,
    color_background_form_input_hover = lightBlue,
    color_background_checkbox_radio_checked = textBlue,
// table
    color_text_table_header = bodyBackground,
    color_background_table_header = textBlue,
    color_background_table_even_rows = evenRowsBackground,
    color_background_table_row_hover = Color.White,
    color_border_table = lightBlue
)

val dilboLightColorScheme = DilboColorScheme(
// body & modal
    color_text_body = textColor,
    color_background_body = bodyBackground,
    color_background_modal = evenRowsBackground,
    color_background_bar = barBackground,
    color_text_h1_h3 = textBlue,
    color_text_p_h4_h6_etc = textColor,
    color_text_link = textBlue,
    color_text_link_hover_visited = darkBlue,
// menu
    color_background_menubar = Color.White,
    color_text_menuitem = textBlue,
    color_background_menuitem = Color.White,
    color_text_menuitem_hover = Color.White,
    color_background_menuitem_hover = darkGray,
// formbutton
    color_text_form_button = bodyBackground,
    color_background_form_button = textBlue,
    color_text_form_button_hover = bodyBackground,
    color_background_form_button_hover = darkGray,
// formInput
    color_text_form_input = textColor,
    color_background_form_input = Color.White,
    color_border_form_input = accentBlue,
    color_border_autocomplete_items = accentBlue,
    color_text_form_input_hover = textColor,
    color_background_form_input_hover = lightBlue,
    color_background_checkbox_radio_checked = textBlue,
// table
    color_text_table_header = bodyBackground,
    color_background_table_header = textBlue,
    color_background_table_even_rows = evenRowsBackground,
    color_background_table_row_hover = Color.White,
    color_border_table = lightBlue
)



