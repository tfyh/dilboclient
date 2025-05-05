package org.dilbo.dilboclient.tfyh.util

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.dilbo.dilboclient.composable.TextAutoCompleteInput
import org.dilbo.dilboclient.composable.DateInput
import org.dilbo.dilboclient.composable.DateTimeInput
import org.dilbo.dilboclient.composable.DilboLabel
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.composable.TextSelectInput
import org.dilbo.dilboclient.composable.TextAnyInput
import org.dilbo.dilboclient.composable.SubmitButton
import org.dilbo.dilboclient.composable.TimeInput
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Findings
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Indices
import org.dilbo.dilboclient.tfyh.data.Item
import org.dilbo.dilboclient.tfyh.data.Parser
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.data.Property
import org.dilbo.dilboclient.tfyh.data.PropertyName
import org.dilbo.dilboclient.tfyh.data.Type
import org.dilbo.dilboclient.tfyh.data.Validator

data class FormField (
    val parentItem: Item, val modifierChar: String, val nameDataField: String, val labelSetter: String,
    val rowTag: String, val index: Int, val listPosition : Int = 0, val onclick: () -> Unit = {},
    val columnsSpan: Int = 1
) {
    val name: String
    val item: Item
    val label: String
    val type: Type
    var width: Dp
    val options: MutableMap<String, Any> = mutableMapOf()
    var preset: String = ""
    var autoEntered: String = ""
    var entered: String = ""
    var parsed: Any = ""
    private var validated: Any = ""
    var changed: Boolean = false
    var findings: String = ""
    private val isProperty: Boolean
    val property: Property

    private val inputType: FormFieldType
    private val listeners: MutableList<FormFieldListener> = mutableListOf()
    private var isFocused: Boolean = false

    inner class FormFieldViewModel: ViewModel() {
        fun show() { _version.update { _version.value + 1 } }
        fun hide() { _version.update { 0 } }
        private val _version : MutableStateFlow<Int> = MutableStateFlow(1)
        val version : StateFlow<Int> = _version
        var entered by mutableStateOf("")
    }
    val viewModel = FormFieldViewModel()

    init {
        val propertyName = PropertyName.valueOfOrInvalid(nameDataField)
        isProperty = (propertyName !== PropertyName.INVALID)
        // the item to be modified is either a property, a child of the
        // config item of this form or a generic form field
        val recordItem = parentItem.getChild(nameDataField) ?: Config.getInstance().invalidItem
        val formFieldsItem = Config.getInstance().getItem(".framework.form_fields")

        item = if (isProperty) recordItem else {
            if (parentItem.hasChild(nameDataField))
                parentItem.getChild(nameDataField) ?: Config.getInstance().invalidItem
            else
                formFieldsItem.getChild(nameDataField) ?: Config.getInstance().invalidItem
        }
        name =
            if (((modifierChar == "~")) && ! isProperty && ! item.isValid())
                nameDataField + "_" + index // read only data can get always the same name. This makes then unique.
            else
                nameDataField
        label = labelSetter.ifEmpty { if (listPosition == 0) item.label() else item.label() + "#" + listPosition }

        readOptions()
        type = item.type()
        width = Stage.getFieldWidth(columnsSpan)
        property = Property.descriptor[propertyName] ?: Property.invalid

        // set the field dimensions for the form.
        inputType =
            if (item.isValid()) FormFieldType.valueOfOrText(item.inputType())
            else FormFieldType.NONE
    }

    /**
     * get the keyboard type for the form field (Text, numeric, Mail etc)
     */
    fun keyboardType(): KeyboardType {
        return if (type.parser() == ParserName.INT) KeyboardType.Number
        else if (type.name() == "email") KeyboardType.Email
        else if (type.name() == "password") KeyboardType.Password
        else if (type.parser() == ParserName.DOUBLE) KeyboardType.Decimal
        else KeyboardType.Text
    }

    /**
     * Read the options into the input field as they are provided by the respective value_reference property.
     */
    private fun readOptions() {

        val config = Config.getInstance()
        val valueReference = item.valueReference()
        if (valueReference.isEmpty())
            return

        options.clear()
        if (valueReference.startsWith("[")) {
            // a predefined configured list
            val parsedOptions = Parser.parse(valueReference, ParserName.STRING_LIST, Language.CSV)
            if (parsedOptions is List<*>)
            for (option in parsedOptions)  {
                if (option is String) {
                    if (option.contains("="))
                        options[option.split("=")[0]] = option.split("=")[1]
                    else
                        options[option] = option
                }
            }
        } else if (valueReference.startsWith(".")) {
            // an item catalog
            val headItem = config.getItem(valueReference)
            for (child in headItem.getChildren())
                options[child.name()] = child.label()
        } else {
            // a table
            val tableName = valueReference.split(".")[0]
            val indices = Indices.getInstance()
            indices.buildIndexOfNames(tableName)
            val names = indices.getNames(tableName)
            for (name in names.keys)
                options[name] = if (names[name]?.isEmpty() != false) "" else names[name]?.getOrNull(0) ?: ""
        }
    }

    /**
     * Filter all options for an auto-selection or autocompletion field. Not case sensitive.
     */
    fun filterOptions(filtered: MutableList<String>, filter: String, contained: Boolean, maxCount: Int) {
        filtered.clear()
        val filterToLower = filter.lowercase()
        var i = 0
        for (option in options.keys) {
            if ((i < maxCount) &&
                ((contained && option.lowercase().contains(filterToLower))
                        || option.lowercase().startsWith(filterToLower))) {
                filtered.add(option)
                i++
            }
        }
        filtered.sort()
    }

    /**
     * Returns true, if the fragment is contained in one of the options
     * or one of te options starts with it. Not case sensitive.
      */
    fun isMatchingFragment(fragment: String,  contained: Boolean) : Boolean {
        val fragmentToLower = fragment.lowercase()
        for (option in options.keys)
            if ((contained && option.lowercase().contains(fragmentToLower))
                || option.lowercase().startsWith(fragmentToLower))
                return true
        return false
    }

    /**
     * preset a the value with a formatted and resolved value. Only dates are reformatted from
     * the local format to the browser expected YYYY-MM-DD.
     */
    internal fun presetWithString (formattedValue: String) {
        // reformat Date and DateTime to iso compatible
        val parser = type.parser()
        val isDateOrDateTime = (parser === ParserName.DATE) || (parser === ParserName.DATETIME)
        val isMissingNotice = (inputType == FormFieldType.AUTOSELECT) &&
                (formattedValue == Indices.getInstance().missingNotice)
        val language = if (isDateOrDateTime) Language.CSV else Config.getInstance().language()
        preset =
            if (isDateOrDateTime)
                Formatter.format(Parser.parse(formattedValue, parser, language), parser, Language.CSV)
            else if (isMissingNotice) ""
            else formattedValue
    }


    /* ---------------------------------------------------------------------- */
    /* --------------- EVALUATION OF FORM ENTRIES --------------------------- */
    /* ---------------------------------------------------------------------- */

    /**
     * Resolve the entered value as name into an id as defined in the value reference. Returns the id on success and
     * the original value on failure.
     */
    private fun resolve(): String {
        if (!item.isValid())
            return entered
        val valueReference = item.valueReference()
        val toResolve = entered
        if ((valueReference.isNotEmpty()) && ! valueReference.startsWith(".")) {
            // a table
            val tableName = valueReference.split(".")[0]
            val indices = Indices.getInstance()
            val referenceField = valueReference.split(".")[1]
            // a table as reference
            var resolved = ""
            if (referenceField == "uuid") {
                // resolve a name to a uuid
                val values: List<String> =
                    if (type.parser() == ParserName.STRING_LIST)
                        @Suppress("UNCHECKED_CAST")
                        parsed as List<String>
                    else
                        listOf(toResolve)
                for (value in values) {
                    val uuid = indices.getUuid(tableName, value)
                    resolved += ", " + (uuid.ifEmpty { value })
                }
                if (resolved.length > 2)
                    resolved = resolved.substring(2)
            }
            return resolved
        }
        // for uuid_or_name type fields a reference may validly not resolvable
        return toResolve
    }

    /**
     * Validate entered against preset, constraints and rules.
     */
    internal fun validate(): Boolean {
        changed = false
        if (isProperty || (item != Config.getInstance().invalidItem)) {
            // only parse data for which a field exists
            findings = ""
            changed = (preset !== entered)
                    && (modifierChar !== "!") && (modifierChar !== "~")
            if (changed) {
                // only validate data if changed.
                if ((modifierChar === "*") && entered.isEmpty()) {
                    findings += I18n.getInstance().t("Please enter a value in %1", label) + ","
                } else {
                    // parse (syntactical check)
                    Findings.clearFindings()
                    parsed = Parser.parse(entered, type.parser())
                    // validate: limits and reference resolving
                    validated = if (item.valueReference().isNotEmpty())
                        resolve()
                    else
                        Validator.adjustToLimits(parsed, type, item.valueMin(),
                            item.valueMax(), item.valueSize())
                    // validate: rule check
                    Validator.checkAgainstRule(validated, item.validationRules())
                    findings = Findings.getFindings(false)
                }
            }
        }
        return changed
    }

    // before submitting a form any change can be used programmatically by implementing listeners
    fun addListener(listener: FormFieldListener) { listeners.add(listener) }
    fun onFocusChanged(isFocused: Boolean) {
        if (isFocused != this.isFocused) {
            this.isFocused = isFocused
            if (!isFocused) {
                val entered = this.entered.ifEmpty { this.preset }
                for (listener in listeners)
                    listener.onFocusLost(entered)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun boxModifier(onDirectionDown: () -> Unit): Modifier {
        val focusManager = LocalFocusManager.current
        return Modifier
            .wrapContentSize()
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp) {
                    if (it.key == Key.Enter)
                        focusManager.moveFocus(FocusDirection.Enter)
                    if (it.key == Key.Tab)
                        focusManager.moveFocus(FocusDirection.Next)
                    if (it.key == Key.DirectionDown)
                        onDirectionDown()
                }
                true
            }
    }
    @Composable
    fun compose() {
        val version: Int by viewModel.version.collectAsStateWithLifecycle()
        this.width = Stage.getFieldWidth(columnsSpan)
        if (version > 0) {
            when (inputType) {
                FormFieldType.TEXT -> {
                    when (type.name()) {
                        "date" -> DateInput(this)
                        "time" -> TimeInput(this)
                        "datetime" -> DateTimeInput(this)
                        else -> TextAnyInput(this)
                    }
                }
                FormFieldType.TEXTAREA -> { TextAnyInput(this, false) }
                FormFieldType.SELECT -> { TextSelectInput(this) }
                FormFieldType.AUTOSELECT -> { TextAutoCompleteInput(this) }
                FormFieldType.SUBMIT -> { SubmitButton(label, onclick) }
                FormFieldType.NONE -> {}
                else -> { DilboLabel(text = "TODO: $label")}
            }
        }
    }

}

