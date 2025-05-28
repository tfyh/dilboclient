package org.dilbo.dilboclient.tfyh.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.api.Transaction
import org.dilbo.dilboclient.app.UIEventHandler
import org.dilbo.dilboclient.composable.DilboLabel
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Codec
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Ids
import org.dilbo.dilboclient.tfyh.data.Item
import org.dilbo.dilboclient.tfyh.data.Parser
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.data.Record

/**
 * tools-for-your-hobby
 * https://www.tfyh.org
 * Copyright  2023-2025  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

class Form(
    private val _recordItem: Item,
    private val formDefinition: String = "",
    private val onSubmit: () -> Unit
) {

    internal var formErrors: String = "" // validation errors as String
    internal var formHeadline: String = "" // headline for the form, leave empty to use the record item label
    var inputFields: MutableMap<String, FormField> = mutableMapOf()   // used by the FormHandler class
    internal var previousErrors: String = "" // for display of entry errors on top of the form
    private var fsId = Ids.generateUid(4)
    private var formColumnsCount: Int = 0
    private var formFieldFocused: FormField? = null

    inner class FormViewModel: ViewModel() {
        fun setVisible(visible: Boolean) {
            _show.update { if (visible) _show.value + 1 else -1 }
        }
        private val _show : MutableStateFlow<Int> = MutableStateFlow(0)
        val show : StateFlow<Int> = _show
        override fun toString() = "v" + _show.value
    }
    val viewModel = FormViewModel()

    /* ---------------------------------------------------------------------- */
    /* --------------------- INITIALIZATION --------------------------------- */
    /* ---------------------------------------------------------------------- */
    /**
     * Initialize the form. Separate function to keep aligned with the
     * javascript twin code, in which external initialization of a form is used.
     */
    fun init() {
        fsId = Ids.generateUid(4)
        // hierarchy of form definitions: 1. explicitly provided, 2.
        // part of the record's properties, 3. auto-generated from the record's properties.
        val recordToEdit = Record(this._recordItem)
        var formDefinitionToUse =
            this.formDefinition.ifEmpty {
                this._recordItem.recordEditForm().ifEmpty {
                    recordToEdit.defaultEditForm()
                }
            }

        if (!formDefinitionToUse.startsWith("rowTag;names;labels"))
            formDefinitionToUse = "rowTag;names;labels\n$formDefinitionToUse"
        val definitionRows = Codec.csvToMap(formDefinitionToUse)
        this.inputFields = mutableMapOf()
        val config = Config.getInstance()

        // identify form columns count
        for (definitionRow in definitionRows) {
            val namesStr = definitionRow["names"]
            if (namesStr != null) {
                val namesPlus = Parser.parse(namesStr, ParserName.STRING_LIST, Language.CSV) as List<*>
                if (namesPlus.size > formColumnsCount) formColumnsCount = namesPlus.size
            }
        }

        // * = required, . = hidden, ! = read-only, ~ = display value like bold label
        val modifierChars = listOf("*", ".", "!", "~", ">", "<")
        var i = 0
        val formFieldsItem = config.getItem(".framework.form_fields")
        for (definitionRow in definitionRows) {
            val rowTag = definitionRow["rowTag"]
            val namesStr = definitionRow["names"]
            val labelsStr = definitionRow["labels"]
            if ((namesStr != null) && (labelsStr != null)) {
                val namesPlus = Parser.parse(namesStr, ParserName.STRING_LIST, Language.CSV)
                val labels = Parser.parse(labelsStr, ParserName.STRING_LIST, Language.CSV)
                var c = 0
                if ((namesPlus is List<*>) && (labels is List<*>)) {
                    for (namePlus in namesPlus) {
                        var inputFieldName = namePlus as String
                        val useRowTag = if (c == 0) (rowTag ?: "") else ""
                        val label = (labels.getOrNull(c) as String?) ?: ""
                        val modifierChar =
                            if (inputFieldName.isNotEmpty() && (modifierChars.indexOf(inputFieldName.substring(0, 1)) >= 0))
                                inputFieldName.substring(0, 1)
                            else ""
                        // the last form field in a row will always fill the remainder of the row.
                        val formColumnsSpan =
                            if (c == (namesPlus.size - 1)) formColumnsCount - c
                            else 1
                        inputFieldName = inputFieldName.substring(modifierChar.length)
                        var listPosition = 0
                        var dataFieldName = inputFieldName
                        if (inputFieldName.contains("#")) {
                            dataFieldName = inputFieldName.substring(0, inputFieldName.indexOf("#"))
                            // inputName = namePlusStr.replace("#", "_")
                            try  {
                                listPosition = inputFieldName.substring(inputFieldName.indexOf("#") + 1).toInt()
                            } catch (_: Exception) {}
                        }
                        if (inputFieldName.isNotEmpty())
                            this.inputFields[inputFieldName] =
                                if (_recordItem.hasChild(dataFieldName))
                                    FormField(this, _recordItem, modifierChar, dataFieldName, label, useRowTag,
                                        i, listPosition, onSubmit, formColumnsSpan)
                                else
                                    FormField(this, formFieldsItem, modifierChar, inputFieldName, label, useRowTag,
                                        i, 0, onSubmit, formColumnsSpan)
                        else {
                            val virtualName = "_$i"
                            this.inputFields[virtualName] =
                                FormField(this, config.invalidItem, modifierChar, virtualName, virtualName,
                                    useRowTag, i, 0, { }, formColumnsSpan)
                        }
                        if ((listPosition > 0) && (_recordItem.hasChild(dataFieldName))
                                && (this.inputFields[dataFieldName] == null))
                            this.inputFields[dataFieldName] = FormField(this, _recordItem, "",
                                dataFieldName, label, "", i, -1, onSubmit, formColumnsSpan)

                        c++
                        i++
                    }
                }
            }
        }
    }

    /**
     * Convenience shortcut to check whether the form was initialized with a valid configuration
     */
    fun isValid() = this._recordItem.isValid()

    /**
     * Hide an input field
     */
    fun setVisible(fieldName: String, visible: Boolean) {
        val inputField = inputFields[fieldName]
        if (inputField != null) {
            if (visible) inputField.viewModel.show()
            else inputField.viewModel.hide()
            viewModel.show
        }
    }
    /**
     * the form input field gathered focus. Notify the previous one about its focus losing.
     */
    fun moveFocus(toFormField: FormField) {
        this.formFieldFocused?.notifyOnFocusLost()
        this.formFieldFocused = toFormField
    }
    /**
     * Preset all values of the form with those of the provided row. Strings are shown in the form as is,
     * they must be formatted and ids resolved. Only dates are reformatted from the local format to the
     * browser expected YYYY-MM-DD.
     */
    fun presetWithStrings(row: Map<String, String>, asEntered: Boolean = false) {
        for (fieldName in this.inputFields.keys) {
            val f = this.inputFields[fieldName]
            val fieldValue = row[fieldName]
            if ((f != null) && (fieldValue != null))
                f.presetWithString(fieldValue, asEntered)
        }
    }

    fun responsiveFormColumnsCount() =
        if (Stage.getWidthDp() < 800.dp) 1 else formColumnsCount

    /**
     * compose the form based on the field set.
     */
    @Composable
    fun compose () {
        val show = viewModel.show.collectAsStateWithLifecycle()
        val inputNames = inputFields.keys.toList()
        var index = 0
        val version = show.value
        val rsp = Theme.dimensions.regularSpacing
        val headline = formHeadline.ifEmpty { _recordItem.label() }
        if (version >= 0)
            // the fsId is used to hide the
            Column (
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .wrapContentSize()
            ) {
                val cCnt = responsiveFormColumnsCount()
                Spacer(modifier = Modifier.height(rsp))
                Row (modifier = Modifier.width(Stage.getFieldWidth(cCnt))) {
                    if (Stage.isPortrait())
                        Spacer(modifier = Modifier.width(rsp))
                    DilboLabel(headline, true)
                }
                if (previousErrors.isNotEmpty())
                    Row (modifier = Modifier.width(Stage.getFieldWidth(cCnt))) {
                        Text(
                            text = previousErrors,
                            style = Theme.fonts.p,
                            color = Theme.colors.color_all_form_input_invalid
                        )
                    }
                while (index < inputNames.size) {
                    val inputField = inputFields[inputNames[index]]
                    if (inputField != null) {
                        // the "listPosition == 0" fields contain the summary of list fields
                        // and are not displayed, only fields for the list members
                        if (inputField.rowTag == "R")
                            Spacer(modifier = Modifier.height(rsp))
                        Row {
                            if (Stage.isPortrait())
                                Spacer(modifier = Modifier.width(rsp))
                            if (inputField.listPosition >= 0)
                                inputField.compose()
                            index ++
                            var c = 1
                            while ((index < inputNames.size) && (c < cCnt)
                                && (inputFields[inputNames[index]]?.rowTag == "")) {
                                val ff = inputFields[inputNames[index]]
                                if ((ff?.listPosition ?: -1) >= 0) {
                                    Spacer(modifier = Modifier.width(rsp))
                                    ff?.compose()
                                    c++
                                }
                                index ++
                            }
                            Spacer(modifier = Modifier.width(rsp))
                        }
                    } else
                        index ++
                }
                Spacer(modifier = Modifier.height(rsp))
            }
    }

    /**
     * read all values entered.
     */
    fun validate (): Boolean {
        this.formErrors = ""
        var anyChange = false
        for (fieldName in inputFields.keys) {
            val f = inputFields[fieldName]
            if (f != null) {
                anyChange = f.validate() || anyChange
                formErrors += f.findings
                // the change is propagated to the listField. List element fields shall never
                // record a change at all.
                if (f.listPosition > 0) f.changed = false
            }
        }
        return anyChange
    }

    /**
     * Submit the form to the API. This will insert the record. To update the record, set update
     * true. In that case set uid to ensure that the correct record is updated. For update == flase
     * a new uid is generated.
     */
    fun submit(update: Boolean, uid: String = ""): Transaction {
        val record: MutableMap<String, String> = mutableMapOf()
        for (inputFieldName in inputFields.keys) {
            val inputField = inputFields[inputFieldName]
            if (inputField != null &&
                (inputField.listPosition <= 0) &&
                // -1 = list field which was split into single fields, 0 = no list field, >0 = list element field
                (inputField.changed || inputField.preset.isNotEmpty()))
                record[inputFieldName] = Formatter.format(inputField.validated, inputField.type.parser(),
                    Language.CSV)
        }
        if (! update)
            record["uid"] = Ids.generateUid(6)
        else
            record["uid"] = uid
        val apiHandler = ApiHandler.getInstance()
        return apiHandler.addNewTxToPending(
            txType = if (update) Transaction.TxType.UPDATE else Transaction.TxType.INSERT,
            tableName = _recordItem.name(),
            record = record
        )
    }

}