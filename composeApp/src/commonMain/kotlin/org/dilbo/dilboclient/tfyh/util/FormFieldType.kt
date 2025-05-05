package org.dilbo.dilboclient.tfyh.util

enum class FormFieldType {
    CHECKBOX,
    TEXT,
    DATE,
    TIME,
    DATETIME,
    SELECT,
    RADIO,
    AUTOSELECT,
    TEXTAREA,
    SUBMIT,
    NONE;

    companion object {
        fun valueOfOrText(name: String): FormFieldType {
            var fft: FormFieldType = TEXT
            try {
                fft = FormFieldType.valueOf(name.uppercase())
            } catch (_: Exception) {
            }
            return fft
        }
    }
}