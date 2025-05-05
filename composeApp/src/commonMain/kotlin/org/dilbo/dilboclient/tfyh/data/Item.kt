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

package org.dilbo.dilboclient.tfyh.data

import org.dilbo.dilboclient.tfyh.control.LoggerSeverity
import org.dilbo.dilboclient.tfyh.util.I18n
import org.dilbo.dilboclient.tfyh.util.Language
import kotlin.math.abs

class Item private constructor(parentItemSetter: Item?, definition: Map<String, String>) {

    private val nameSetter = definition["_name"] ?: "missing_name"
    private val _name =
        if (PropertyName.valueOfOrInvalid(nameSetter) !== PropertyName.INVALID) {
            val errorMessage = "Forbidden child name $nameSetter detected at " +
                    (parentItemSetter?.getPath() ?: "???") + ". Aborting.";
            Config.getInstance().logger.log(LoggerSeverity.ERROR, "Item.constructor", errorMessage)
            "forbidden_$nameSetter"
        } else if (discouragedNames.indexOf(nameSetter) >= 0) {
            val errorMessage = "Discouraged child name " + nameSetter + " detected at " +
                    (parentItemSetter?.getPath() ?: "???") + ". Changed to " + nameSetter + "!";
            Config.getInstance().logger.log(LoggerSeverity.ERROR, "Item.constructor", errorMessage)
            "$nameSetter!"
        } else
            nameSetter
    private val _parentItem: Item
    private val _type: Type
    private val properties: MutableMap<PropertyName, Any>
    internal val children: MutableList<Item>

    init {
        if (parentItemSetter == null)
            this._parentItem = this
        else {
            this._parentItem = parentItemSetter
            this._parentItem.children.add(this)
        }
        // set properties. Start with the immutable properties
        this.properties = mutableMapOf()
        this.properties[PropertyName._NAME] = this._name
        this.properties[PropertyName._PATH] = parentItemSetter?.getPath() ?: "#none"
        this._type = Type[definition["value_type"] ?: "none"]
        this.properties[PropertyName.VALUE_TYPE] = this._type.name()
        // set the children
        this.children = mutableListOf()
        if (definition["value_type"] == "template") {
            // if it is a template, copy the template
            val templatePath = definition["value_reference"] ?: "#invalid"
            val templateItem = Config.getInstance().getItem(templatePath)
            if (templateItem.isValid()) {
                for (templateChild in templateItem.children) {
                    val newChild = Item(this,
                        mapOf("_name" to templateChild.name(),
                            "value_type" to templateChild.valueType()))
                    newChild.mergeProperties(templateChild.properties)
                }
            }
        }
        // parse the definition as properties and children's actual values.
        this.parseDefinition(definition)
    }

    /**
     * Convenience function to simplify the validity check.
     */
    fun isValid() = (this != Config.getInstance().invalidItem)

    // generic search function
    // =======================
    fun find(lowerCaseAsciiFind: String, found: MutableMap<String, String>) {
        val ownPath = this.getPath()
        if (WordIndex.toLowerAscii(this.name()).indexOf(lowerCaseAsciiFind) >= 0)
            found[ownPath] = this.name()
        if (WordIndex.toLowerAscii(this.label()).indexOf(lowerCaseAsciiFind) >= 0)
            found["$ownPath.label"] = this.label()
        if (WordIndex.toLowerAscii(this.description()).indexOf(lowerCaseAsciiFind) >= 0)
            found["$ownPath.description"] = this.description()
        if (WordIndex.toLowerAscii(this.valueStr()).indexOf(lowerCaseAsciiFind) >= 0)
            found["$ownPath.value"] = this.valueStr()
        for (child in children)
            child.find(lowerCaseAsciiFind, found)
    }

    companion object {

        /**
         * Names of properties and functions of the javascript Array and object type. Using these
         * names may lead to issues.
         */
        private val discouragedNames = arrayOf(
            // javascript Array type
            "at","concat","copyWithin","entries","every",
            "fill","filter","find","findIndex","findLast","findLastIndex","flat","flatMap","forEach","from",
            "fromAsync","includes","indexOf","isArray","join","keys","lastIndexOf","length","map","of","pop","push",
            "reduce","reduceRight","reverse","shift","slice","some","sort","splice","toLocaleString",
            "toReversed","toSorted","toSpliced","toString","unshift","values","with",
            // javascript Object
            "__defineGetter__","__defineSetter__","__lookupGetter__",
            "__lookupSetter__","assign","create","defineProperties","defineProperty","entries","freeze",
            "fromEntries","getOwnPropertyDescriptor","getOwnPropertyDescriptors","getOwnPropertyNames",
            "getOwnPropertySymbols","getPrototypeOf","groupBy","hasOwn","hasOwnProperty","is","isExtensible",
            "isFrozen","isPrototypeOf","isSealed","keys","preventExtensions","propertyIsEnumerable","seal",
            "setPrototypeOf","toLocaleString","toString","valueOf","values")

        // Create a free floating item. To be used for the "invalid item" and the config root node.
        fun getFloating(definition: Map<String, String>) = Item(null, definition)

        /**
         * Sort all top level branches according to the canonical sequence.
         */
        fun sortTopLevel() {
            val config = Config.getInstance()
            val sortCache = mutableListOf<Item>()
            for (topBranchName in Config.allSettingsFiles)
                if (config.getItem(".$topBranchName") !== config.rootItem)
                    sortCache.add(config.getItem(".$topBranchName"))
            config.rootItem.children.clear()
            for (topBranch in sortCache)
                config.rootItem.children.add(topBranch)
        }
    }

    // setter functions for properties
    // ===============================
    fun parseProperty(key: String, value: String, language: Language) {
        val propertyName = PropertyName.valueOfOrInvalid(key)
        val property = Property.descriptor[propertyName] ?: Property.invalid
        val propertyParser = property.parser(this._type)
        // parse and take in, if not empty.
        if (propertyName !== PropertyName.INVALID) {
            val parsedProperty = Parser.parse(value, propertyParser, language)
            if (!ParserConstraints.isEmpty(parsedProperty, propertyParser))
                this.properties[propertyName] = parsedProperty
        }
    }
    /**
     * Parse a definition map into the items properties and its children's actual values. Overwrite, but keep existing
     * properties which are not in $definition. Immutable properties and unmatched fields are skipped.
     */
    fun parseDefinition(definition: Map<String, String>) {
        val newProperties = Property.parseProperties(definition, this._type)
        this.mergeProperties(newProperties)
        for (child in this.children) {
            val childValue = definition[child.name()]
            if (childValue != null)
                child.parseProperty("actual_value", childValue, Config.getInstance().language())
        }
    }
    /**
     * Copy all $sourceProperties values into $this->properties except the immutable ones.
     * Overwrite the existing, but keep those which are not part of the $sourceProperties set.
     */
    private fun mergeProperties(sourceProperties: Map<PropertyName, Any>) {
        for (propertyName in sourceProperties.keys)
        if (!Property.isImmutable(propertyName)) {
            val sourceProperty = sourceProperties[propertyName]
            if (sourceProperty != null)
                this.properties[propertyName] = Property.copyOfValue(sourceProperty)
        }
    }

    /**
     * Remove this item from its parent, clear children, properties and actual and do this with all items of its entire
     * branch recursively.
     */
    internal fun destroy() {
        // delete all information
        properties.clear()
        // then drill down
        for (child in children)
            child.destroy()
        // clear the own children after they have cleared their properties
        children.clear()
    }

    /**
     * get the parent item or, if there is none, this same item
     */
    fun name() = _name
    /**
     * Return the path property, which is different from teh getPath(), because it is the path of the parent. Cf. getPath()
     */
    fun path() = properties[PropertyName._PATH] as String? ?: ".invalid"
    fun type() = _type
    fun parent() = _parentItem

    // The defaultValue() is also used by the Record class, therefore it is not private
    fun defaultValue() = properties[PropertyName.DEFAULT_VALUE] ?: _type.defaultValue()
    private fun defaultLabel() = if (properties[PropertyName.DEFAULT_LABEL] != null)
        I18n.getInstance().t(properties[PropertyName.DEFAULT_LABEL] as String) else _type.defaultLabel()
    private fun defaultDescription() = if (properties[PropertyName.DEFAULT_DESCRIPTION] != null)
        I18n.getInstance().t(properties[PropertyName.DEFAULT_DESCRIPTION] as String) else _type.defaultDescription()

    fun nodeHandling() = properties[PropertyName.NODE_HANDLING] as String? ?: _type.nodeHandling()
    fun nodeAddableType() = properties[PropertyName.NODE_ADDABLE_TYPE] as String? ?: _type.nodeAddableType()
    fun nodeWritePermissions() = properties[PropertyName.NODE_WRITE_PERMISSIONS] as String? ?: _type.nodeWritePermissions()
    fun nodeReadPermissions() = properties[PropertyName.NODE_READ_PERMISSIONS] as String? ?: _type.nodeReadPermissions()

    fun valueType() = _type.name()
    fun valueMin() = properties[PropertyName.VALUE_MIN] ?: _type.valueMin()
    fun valueMax() = properties[PropertyName.VALUE_MAX] ?: _type.valueMax()
    fun valueSize() = properties[PropertyName.VALUE_SIZE] as Int? ?: _type.valueSize()
    fun valueUnit() = properties[PropertyName.VALUE_UNIT] as String? ?: _type.valueUnit()
    fun valueReference() = properties[PropertyName.VALUE_REFERENCE] as String? ?: _type.valueReference()
    fun validationRules() = properties[PropertyName.VALIDATION_RULES] as String? ?: _type.validationRules()

    fun sqlType() = properties[PropertyName.SQL_TYPE] as String? ?: _type.sqlType()
    fun sqlNull() = properties[PropertyName.SQL_NULL] as Boolean? ?: _type.sqlNull()
    fun sqlIndexed() = properties[PropertyName.SQL_INDEXED] as String? ?: _type.sqlIndexed()

    fun inputType() = properties[PropertyName.INPUT_TYPE] as String? ?: _type.inputType()
    fun inputModifier() = properties[PropertyName.INPUT_MODIFIER] as String? ?: _type.inputModifier()
    fun recordEditForm() = properties[PropertyName.RECORD_EDIT_FORM] as String? ?: _type.recordEditForm()

    // getter for value, label and description
    // =======================================
    /**
     * Get the value. This will return the actual or the item default, if no actual was set. If the
     * item default is also not set, the type default is used.
     */
    fun value() = if (ParserConstraints.isEmpty(properties[PropertyName.ACTUAL_VALUE], _type.parser())) defaultValue()
        else properties[PropertyName.ACTUAL_VALUE] ?: defaultValue()
    // shorthand functions to get the value as String.
    fun valueCsv() = Formatter.formatCsv(this.value(), _type.parser())
    fun valueSql() = Formatter.format(this.value(), _type.parser(), Language.SQL)
    fun valueStr() = Formatter.format(this.value(), _type.parser())
    // localized, i.e. translated properties
    fun label() = if ((properties[PropertyName.ACTUAL_VALUE] as String? ?: "").isEmpty()) defaultLabel()
            else I18n.getInstance().t(properties[PropertyName.ACTUAL_VALUE] as String)
    fun description() = if ((properties[PropertyName.ACTUAL_DESCRIPTION] as String? ?: "").isEmpty()) defaultDescription()
            else I18n.getInstance().t(properties[PropertyName.ACTUAL_DESCRIPTION] as String)

    fun isOfAddableType(item: Item): Boolean {
        val itemTypeName = item.type().name()
        return ((itemTypeName == this.nodeAddableType()) ||
                ((itemTypeName == "template") && (item.defaultValue() === this.nodeAddableType())))
    }

    // Format the property value the "CSV language", but no csv encoding
    private fun propertyCsv(propertyName: PropertyName): String {
        val propertyValue = properties[propertyName] ?: return ""
        val property = Property.descriptor[propertyName] ?: Property.invalid
        return Formatter.format(propertyValue, property.parser(this._type), Language.CSV)
    }

    /**
     * Iterates through all children and returns true if the id was matched. If not,
     * false is returned.
     */
    fun hasChild(name: String) = (getChild(name) != null)

    /**
     * Returns the child with the given name, if existing, else null.
     */
    fun getChild(name: String): Item? {
        for (child in children)
            if (child.name() == name)
                return child
        return null
    }

    /**
     * Return all children as mutable array. Be careful not to change those.
     */
    fun getChildren(): MutableList<Item> { return children }

    /**
     * Returns the full path of the Item. The Item.path() will return the path property, which is the parent item's
     * path. For top level items getPath() will return ".topLevelName" and path() ""; for root and invalid getPath()
     * will return "" and path() "#none".
     */
    fun getPath(): String {
        if (this == Config.getInstance().rootItem)
            return "."
        var path = _name
        var current = this
        val passed = mutableListOf(this.path()) // path is a unique immutable String property of the item, not the "getPath" dynamic result.
        while (current.parent() != current) {
            current = current.parent()
            if (passed.contains(current.path())) {
                val recursionPath = current.name() + "(#recursion#)." + path
                Config.getInstance().logger.log(LoggerSeverity.ERROR, "Item.getPath()",
                    "Recursion detected in configuration. Please correct: " + recursionPath)
                return recursionPath
            }
            passed.add(current.path())
            path = if (current == Config.getInstance().rootItem) ".$path" else "${current.name()}.$path"
        }
        return path
    }

    /**
     * Copy the sourceItem's children to this item. Used by the Record class to propagate common record fields. No
     * drill down.
     */
    internal fun copyChildren(sourceItem: Item) {
        for (sourceChild in sourceItem.children)
            if (!hasChild(sourceChild.name())) {
                val ownChild = Item(this,
                    mapOf("_name" to sourceChild.name(), "value_type" to sourceChild._type.name()))
                ownChild.mergeProperties(sourceChild.properties)
            }
    }

    /**
     * Reads the definition into a child item. This will create a new child, if the child with the
     * name that is given in the definition does not exist. It will merge the properties and the
     * children's actual values, if the child exists. Returns false, only if the name or - for
     * a not yet existing child - the value type are missing in the definition, or if the provided
     * value type for a new child is invalid.
     */
    private fun putChild(definition: Map<String, String>): Boolean {
        // a name and valid type must be provided in the definition
        val childName = definition["_name"] ?: return false
        // check whether the child already exists
        val child = this.getChild(childName)
        if (child != null) {
            // the child exists replace the properties, but not the children
            child.parseDefinition(definition)
            return true
        }
        // for new children valid type must be provided in the definition
        val childTypeString = definition["value_type"] ?: return false
        val childType = Type[childTypeString]
        if (childType == Type.invalid)
            return false
        Item(this, definition)
        return true
    }

    /**
     * Remove the child item from this item's children array.
     */
    fun removeChild(child: Item) {
        this.children.remove(child)
    }

    /**
     * Validate value against this item's constraints and validation rules. Returns an updated value,
     * e.g. when adjusted by the limit checks. If value is left out or set null, the items actual
     * value will be validated, updated, and returned. See the Findings class to get errors and warnings.
     */
    fun validate(value: Any? = null): Any {
        var validated: Any
        if (value == null)
            validated = properties[PropertyName.ACTUAL_VALUE] ?: ParserConstraints.empty(type().parser())
        else {
            // empty values are always syntactically compliant
            if (ParserConstraints.isEmpty(value, _type.parser()))
                return value
            validated = value
        }
        // limit conformance
        validated = Validator.adjustToLimits(validated, _type, valueMin(), valueMax(), valueSize())
        // validation rules conformance
        Validator.checkAgainstRule(validated, this.validationRules())
        return validated
    }

    // get a readable String for debugging purposes
    override fun toString(): String {
        if (valueCsv().isEmpty())
            return "$_name (${_type.name()}) => ${parent().getPath()}"
        return "$_name=${valueCsv()} (${_type.name()}) => ${parent().getPath()}"
    }

    /**
     * Read a full branch from its definitions array
     */
    fun readBranch(definitionsArray: List<Map<String, String>>): String {
        val config = Config.getInstance()
        for (definition in definitionsArray) {
            // read the relative path
            val path = definition["_path"]
            val name = definition["_name"]
            if ((path != null) && (name != null)) {
                val parent = config.getItem(path)
                if (!parent.isValid())
                    // an invalid parent means that the path could not be resolved
                    return "Failed to find parent '$path' for child '$name'"
                else {
                    val success = parent.putChild(definition)
                    if (!success)
                    // adding can fail, if child names are duplicate
                        return "Failed to add child '$name' at $path"
                }
            }
        }
        return ""
    }

    private fun collectItems(items: MutableList<Item>, fieldNames: MutableList<String>,
                             drillDown: Int, level: Int = 0) {
        for (child in children) {
            if (child !== this) {
                // avoid endless drill down loops. Misconfiguration can cause such situations
                items.add(child)
                for (propertyName in child.properties.keys)
                    if ((!fieldNames.contains(propertyName.name.lowercase())))
                        fieldNames.add(propertyName.name.lowercase())
                if (level < drillDown)
                    child.collectItems(items, fieldNames, drillDown, level + 1)
            }
        }
    }

    /**
     * Sort all children of this item in alphabetical order of their names. No drill down.
     */
    fun sortChildrenByName() {
        this.children.sortWith(compareBy { it.name() });
    }

    /**
     * Sort all children to get all branches first or last, but do not change the inner sequence of
     * branches and leaves.
     */
    private fun sortChildren(drillDown: Int, branchesFirst: Boolean) {
        // split children into branches and leafs
        val branchItems = mutableListOf<Item>()
        val leafItems = mutableListOf<Item>()
        for (child in this.children) {
            if ((child.children.size > 0) || (child.nodeAddableType().isNotEmpty()))
                branchItems.add(child)
            else
                leafItems.add(child)
        }

        // now rearrange the children according to the rearranged names.
        this.children.clear()
        if (branchesFirst) {
            this.children.addAll(branchItems)
            this.children.addAll(leafItems)
        } else {
            this.children.addAll(leafItems)
            this.children.addAll(branchItems)
        }

        // go for further levels, if required.
        if (drillDown > 0)
            for (child in this.children)
                if (child !== this) 
                    // avoid endless drill down loops. Misconfiguration can cause such situations
                    child.sortChildren(drillDown - 1, branchesFirst)
    }

    /**
     * Get the entire branch as csv table.
     */
    fun branchToCsv(drillDown: Int): String {
        val items = mutableListOf<Item>( this )
        var fieldNames = mutableListOf<String>()
        for (propertyName in this.properties.keys)
            fieldNames.add(propertyName.name.lowercase())
        sortChildren(drillDown, false)
        collectItems(items, fieldNames, drillDown)
        fieldNames = Property.sortProperties(fieldNames)
        var header = ""
        for (fieldName in fieldNames)
            header += ";$fieldName"
        var csv = header.substring(1) + "\n"
        for (item in items) {
            var rowCsv = ""
            for (fieldName in fieldNames)
                rowCsv += ";" + Codec.encodeCsvEntry(item.propertyCsv(PropertyName.valueOfOrInvalid(fieldName)))
            csv += rowCsv.substring(1) + "\n"
        }
        return csv
    }

    fun getLevel(): Int {
        if ((this === Config.getInstance().rootItem) || !this.isValid())
            return 0
        return this.getPath().split(".").size - 1
    }

    /**
     * Move a child branch within the children sequence. The sequence is the one
     * created by adding the items. (See:
     * https://stackoverflow.com/questions/5525795/does-javascript-guarantee-object-property-order)
     */
    fun moveChild (item: Item, by: Int): Boolean
    {
        if (by == 0) // nothing to move
            return true

        // identify the current and new item position
        val parent = item.parent()
        val fromPosition = parent.children.indexOf(item)
        val toPosition = fromPosition + by
        // do not move, if target position is beyond the ends
        if ((toPosition >= parent.children.size) || (toPosition < 0))
            return false

        // now move the items in between fromPosition and toPosition
        // this will duplicate the name at the $to_position
        val end = abs(by)
        val fwd = by / end
        for (i in 1 .. end)
            parent.children[fromPosition + ((i - 1) * fwd)] = parent.children[fromPosition + (i * fwd)]
        // replace the name at the toPosition by the cached name
        parent.children[toPosition] = item
        return true
    }

}