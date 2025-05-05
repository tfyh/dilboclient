package org.dilbo.dilboclient.tfyh.data

import io.ktor.util.date.getTimeMillis
import kotlinx.datetime.LocalDate
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.LocalCache

/**
 * A table holds all rows and a name index. Each row is a Map of <fieldName, fieldValue>,
 * values are csv-encoded Strings.
 */
class Table internal constructor(private val recordItem: Item) {

    enum class Comparison {
        EQUAL, NOT_EQUAL, LOWER_THAN, GREATER_THAN, LOWER_OR_EQUAL, GREATER_OR_EQUAL, CONTAINS, IS_EMPTY, NONE
    }

    // Selector usage example
    val example: Selector = Selector(
        childrenAnd = true, children = mutableListOf(
            Selector(
                // A == 1 and
                fieldName = "A", compare = Comparison.EQUAL, filter = "1"
            ),
            Selector(
                // B < 100 or C >= 70
                childrenAnd = false, children = mutableListOf(
                    Selector(
                        fieldName = "B", compare = Comparison.LOWER_THAN, filter = "100"
                    ),
                    Selector(
                        fieldName = "C", compare = Comparison.GREATER_OR_EQUAL, filter = "70"
                    )
                )
            )
        )
    )

    /**
     * a hierarchical selection filter, see val example fur usage.
     */
    data class Selector(
        var fieldName: String = "",
        var compare: Comparison = Comparison.NONE,
        var filter: Any = "",
        val children: MutableList<Selector> = mutableListOf(),
        var childrenAnd: Boolean = true
    ) {
        fun isSelected(record: Record): Boolean {
            val value = record.value(fieldName)
            val parser = record.item.getChild(fieldName)?.type()?.parser() ?: ParserName.NONE
            val filterValue = Parser.parse(filter, parser, Language.CSV)
            var selected: Boolean
            var i = 0
            if (children.isNotEmpty()) {
                selected = children[i].isSelected(record)
                while (i < children.size - 1) {
                    i++
                    selected =
                        if (childrenAnd)
                            selected && children[i].isSelected(record)
                        else
                            selected || children[i].isSelected(record)
                }
            }
            else selected = when (compare) {
                Comparison.EQUAL -> Validator.equals(value, filterValue, parser)
                Comparison.NOT_EQUAL -> !Validator.equals(value, filterValue, parser)
                Comparison.LOWER_THAN -> Validator.lt(value, filterValue, parser)
                Comparison.GREATER_OR_EQUAL -> ! Validator.lt(value, filterValue, parser)
                Comparison.GREATER_THAN -> ! Validator.equals(value, filterValue, parser) && ! Validator.lt(value, filterValue, parser)
                Comparison.LOWER_OR_EQUAL -> Validator.equals(value, filterValue, parser) || Validator.lt(value, filterValue, parser)
                Comparison.CONTAINS -> Validator.contains(value, filterValue, parser)
                Comparison.IS_EMPTY -> ParserConstraints.isEmpty(value, parser)
                Comparison.NONE -> true
            }
            return selected
        }
    }

    /**
     * Build a selector to filter rows with matching filterRow values. If filterRow contains fields,
     * that do not belong to this table, they are ignored.
     */
    fun buildSelector(filterRow: MutableMap<String, String>): Selector {
        val conditions: MutableList<Selector> = mutableListOf()
        for (fieldName in filterRow.keys) {
            if (recordItem.hasChild(fieldName)) {
                val field = recordItem.getChild(fieldName)
                if (field != null) {
                    val filterValue = Parser.parse(filterRow[fieldName] ?: "", field.type().parser(), Language.CSV)
                    conditions.add(Selector(fieldName, Comparison.EQUAL, filterValue))
                }
            }
        }
        return Selector(children = conditions, childrenAnd = true)
    }

    /**
     * Notify UI provider on table data change.
     */
    interface Listener {
        /**
         * Implement the logic that shall be applied on a change of either a single record or
         * multiple records (uid = "")
         */
        fun changed(table: Table, uid: String)
    }

    companion object {

        /**
         * Return a sorting rule. Note that descending sort is not possible for Strings
         */
        private fun comparable(sortName: String, row: MutableMap<String, String>,
                               item: Item): Comparable<*> {
            val upDown = if (sortName.startsWith("-")) -1 else +1
            val fieldName = if (sortName.startsWith("-")) sortName.substring(1) else sortName
            val field = item.getChild(fieldName) ?: return 0
            val parser = field.type().parser()
            val value = Parser.parse(row[fieldName] ?: "", parser)
            return when (val comparable = Validator.comparable(value, parser)) {
                is Int -> comparable * upDown
                is Long -> comparable * upDown
                is Double -> comparable * upDown
                else -> comparable
            }
        }

        /**
         * sort the list by a sort string like id.-invalid_from or last_name.first_name. Note that
         * descending sort does not work with strings and sorting of lists will be according to
         * the list size only.
         */
        fun sortRows(sorting: String, rowsList: List<MutableMap<String, String>>,
                     item: Item): List<MutableMap<String, String>> {
            val sorts = sorting.split(".")
            return when (sorts.size) {
                1 -> rowsList.sortedWith(compareBy { comparable(sorts[0], it, item) })
                2 -> rowsList.sortedWith(compareBy({ comparable(sorts[0], it, item) },
                    { comparable(sorts[1], it, item) }))
                3 -> rowsList.sortedWith(compareBy({ comparable(sorts[0], it, item) },
                    { comparable(sorts[1], it, item) }, { comparable(sorts[2], it, item) }))
                else -> rowsList.toList()
            }
        }
    }

    private val rows: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    internal val record: Record = Record(recordItem)
    private val nameIndex: MutableMap<String, String> = mutableMapOf()
    private var lastChanged: Double = 0.0
    private var lastStored: Double = 0.0
    internal val listeners: MutableList<Listener> = mutableListOf()

    /**
     * Update the uuid index. it always points to the most recent version of an object for
     * versioned tables, that's why its complex.
     */
    private fun updateUuidIndex(row: MutableMap<String, String>,
                                uuidIndex: MutableMap<String, String>, isDelete: Boolean = false) {
        val uid = row["uid"] ?: return
        val uuid = row["uuid"] ?: return
        val invalidFrom = row["invalid_from"] ?: ""

        // this is a new uuid
        if (uuidIndex[uuid] == null) {
            if (!isDelete) uuidIndex[uuid] = uid
            // trying to delete a not existing row will not require any action at all
            return
        }

        // this is the valid version
        var currentVersionWasDeleted = false
        if (invalidFrom.isEmpty()) {
            if (!isDelete) {
                uuidIndex[uuid] = uid
                return
            }
            else {
                uuidIndex.remove(uuid)
                currentVersionWasDeleted = true
            }
        }

        // This is not a valid version, and a valid version exists
        val previousInvalidFrom = rows[uid]?.get("invalid_from") ?: ""
        if ((rows[uid] != null) && previousInvalidFrom.isEmpty()) {
            if (isDelete) uuidIndex.remove(uuid)
            // no index update required on removal of a non current version
            // no index update required on adding a non current version
            return
        }

        // Compare the previously index version with this one
        val invalidFromDate = Parser.parse(invalidFrom, ParserName. DATE, Language.CSV) as LocalDate
        val previousInvalidFromDate = Parser.parse(previousInvalidFrom, ParserName. DATE, Language.CSV) as LocalDate
        if (invalidFromDate > previousInvalidFromDate)
            // The previously registered version is less recent than the new one: replace it
            if (!isDelete) uuidIndex[uuid] = uid
            // for delete that shall not happen, because the indexed version is the most recent one
        else if (invalidFromDate == previousInvalidFromDate)
            // The previously registered version is the current version: ignore the new
            if (isDelete) {
                uuidIndex.remove(uuid)
                currentVersionWasDeleted = true
            }
        else
            // The new version is not the current version: ignore the new for add and delete
            if (isDelete) uuidIndex.remove(uuid)

        // a current version is removed. check for remaining versions of this uuid.
        if (currentVersionWasDeleted)
            for (remainingRowUid in rows.keys) {
                val remainingRow = rows[remainingRowUid]
                if (remainingRow != null)  {
                    val remaining = remainingRow["uuid"]
                    if (remaining == uuid)
                        updateUuidIndex(remainingRow, uuidIndex)
                }
            }
    }

    /**
     * Insert a row. row must have a field "uid" with a value that is not yet used.
     * The table row will have exactly the same values as row, no defaults are added or removed.
     * The name index is updated.
     */
    fun insert(row: MutableMap<String, String>, suppressNotification: Boolean = false) {
        val uid = row["uid"]
        // the uid must be set as unique identifier
        if ((uid == null) || (rows[uid] != null))
            return
        // create the new tableRow
        lastChanged = getTimeMillis() / 1000.0
        rows[uid] = mutableMapOf()
        // put the values
        update(row, suppressNotification)
    }

    /**
     * Update a row. row must have a field "uid" with a value that is used.
     * The row-values will overwrite the current values of the table row, extra fields
     * of the table row will be kept. The name index is updated.
     */
    fun update(row: MutableMap<String, String>, suppressNotification: Boolean = false) {
        val uid = row["uid"] ?: return
        // get the tableRow with the respective uid
        val tableRow = rows[uid] ?: return
        // get the current name
        lastChanged = getTimeMillis() / 1000.0
        record.parse(tableRow, Language.CSV)
        val currentName = record.recordToTemplate("name")
        // update the tableRow
        for (fieldName in row.keys) {
            if (record.item.hasChild(fieldName)) {
                val value = row[fieldName]
                if (value != null)
                    tableRow[fieldName] = value
            }
        }
        // get the new name
        record.parse(row, Language.CSV)
        val newName = record.recordToTemplate("name")
        // update the name index
        if (newName != currentName) {
            if (nameIndex[currentName] != null)
                nameIndex.remove(currentName)
            nameIndex[newName] = uid
        }
        // notify listeners
        if (!suppressNotification)
            for (listener in listeners)
                listener.changed(this, uid)
    }

    /**
     * Update a row. row must have a field "uid" with a value that is used.
     * The name index is updated.
     */
    fun delete(uid: String) {
        // get the tableRow with the respective uid
        val tableRow = rows[uid] ?: return
        lastChanged = getTimeMillis() / 1000.0
        // get the current name
        record.parse(tableRow, Language.CSV)

        // update the indices
        val currentName = record.recordToTemplate("name")
        if (nameIndex[currentName] != null)
            nameIndex.remove(currentName)

        // delete the tableRow
        rows.remove(uid)
    }

     /**
     * Read the csv-table and update or insert ist rows in the table depending on whether
     * a row with the respective uid exists or not.
     * The name index is updated. Listeners are notified once.
     */
    fun merge(csv: String) {
        // read the rows
        val tableRows = Codec.csvToMap(csv)
        // insert or update the rows one by one
         for (row in tableRows) {
             val uid = row["uid"]
             if (uid != null) {
                 val tableRow = rows[uid]
                 if (tableRow == null)
                     insert(row, true)
                 else
                     update(row, true)
             }
         }
         Indices.getInstance().addTable(record.item, true)
         for (listener in listeners)
             listener.changed(this, "")
    }

    /**
     * returns true, if the uid corresponds to the uid of the most recent record with the given uuid
     */
    fun isCurrentVersion(uid: String, uuid: String): Boolean {
        if (rows.isNotEmpty() && (rows[rows.keys.first()]?.get("invalid_from") == null))
            return true    // non versioned table. The record is always the current one
        val row = selectByUuid(uuid)  // this returns the most recent one
        return (row["uid"] ?: "?uid?") == uid
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given uid or the uid is null.
     */
    fun selectByUid(uid: String?): MutableMap<String, String> {
        val clone = mutableMapOf<String, String>()
        val row = rows[uid] ?: return clone
        for (fieldName in row.keys) {
            val value = row[fieldName]
            if (value != null)
                clone[fieldName] = value
        }
        return clone
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given uid or the uid is null.
     */
    fun selectByUuid(uuid: String?): MutableMap<String, String> {
        val empty = mutableMapOf<String, String>()
        if (uuid == null)
            return empty
        val selector = Selector("uuid", Comparison.EQUAL, uuid)
        val selected = select(selector)
        if (selected.isEmpty())
            return empty
        val sorted = sortRows("invalid_from", selected, record.item)
        return if (sorted.first()["invalid_from"] == "") sorted.first()
               else sorted.last()
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given name.
     */
    fun selectByName(name: String): MutableMap<String, String> {
        return selectByUid(nameIndex[name])
    }

    /**
     * Select a set of tableRows where (where AND andWhere) OR orWhere,
     */
    fun select(where: Selector?, maxRows: Int = 0): List<MutableMap<String, String>> {
        val selected: MutableList<MutableMap<String, String>> = mutableListOf()
        val uids = rows.keys.toList() // avoid a co-modification exception when loading the very first time
        for (uid in uids) {
            val row = rows[uid]
            if (row != null) {
                record.parse(row.toMap(), Language.CSV)
                if ((where?.isSelected(record) != false) && ((maxRows <= 0) || (selected.size < maxRows)))
                    selected.add(row)
            }
        }
        return selected.toList()
    }

    /**
     * store the table as csv, if changed
     */
    fun store() {
        if ((lastChanged > lastStored) || (lastStored == 0.0)) {
            val rowsList = mutableListOf<MutableMap<String, String>>()
            val allKeys: MutableList<String> = mutableListOf()
            // Strip the uid off the rows and collect the columns filled
            for (uid in rows.keys) {
                val row = rows[uid]
                if (row != null) {
                    for (key in row.keys)
                        if (allKeys.indexOf(key) < 0)
                            allKeys.add(key)
                    rowsList.add(row)
                }
            }
            // set all available columns for the first row
            // because multiple lists can provide data for a table, the row size may differ
            if (rowsList.size > 0) {
                val firstRow = rowsList.getOrNull(0)
                if (firstRow != null)
                    for (key in allKeys)
                        if (firstRow[key] == null)
                            firstRow[key] = ""
            }
            val csv = Codec.encodeCsvTable(rowsList)
            LocalCache.getInstance().setItem("${DataBase.CACHE_PATH}/${record.item.name()}.csv",
                csv)
        }
        lastStored = getTimeMillis() / 1000.0
    }

    override fun toString(): String {
        return record.item.name() +  "[" + rows.size + "]"
    }
}