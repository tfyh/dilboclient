package org.dilbo.dilboclient.tfyh.util

data class FormFieldListener (
    val onValueChange: (String) -> Unit = {},
    val onFocusLost: (String) -> Unit = {}
)