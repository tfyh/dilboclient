package org.dilbo.dilboclient.tfyh.data

import io.ktor.util.date.getTimeMillis
import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.api.Transaction
import org.dilbo.dilboclient.app.DilboSettings
import org.dilbo.dilboclient.tfyh.data.Table.Listener
import org.dilbo.dilboclient.tfyh.data.Table.Selector
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.LocalCache
import kotlin.math.max

class DataBase private constructor() {

    companion object {
        private val instance = DataBase()
        internal const val CACHE_PATH = "Data"
        fun getInstance() = instance
    }

    // the list of tables: name => table
    private val tables: MutableMap<String, Table> = mutableMapOf()
    private var _tableOfFindBy: Table = Table(Config.getInstance().invalidItem)
    fun tableOfFindBy() = _tableOfFindBy.record.item.name()

    /**
     * Reload all tables from the local cache. This will delete all existing data. If a table is
     * not available in the local cache, a transaction for download is appended to the API queue.
     */
    fun load() {
        val localCache = LocalCache.getInstance()
        val recordItems = Config.getInstance().getItem(".tables").getChildren()
        Indices.getInstance().clearAll()
        for (recordItem in recordItems) {
            val timeStr = localCache.getItem("$CACHE_PATH/${recordItem.name()}.modified")
            val time = try { timeStr.toDouble() } catch (e: Exception) { 0.0 }
            val csv = localCache.getItem("$CACHE_PATH/${recordItem.name()}.csv")
            if (csv.isNotEmpty())
                merge(recordItem.name(), csv)
            // download list. First decide on the period to download
            val microTime = getTimeMillis() / 1000.0
            val gracePeriod = 120.0
            val ageInSeconds = microTime - time
            val modifiedAfter = max(microTime - (ageInSeconds * 1.5), 0.0)
            // then create the request transaction, if applicable
            if (ageInSeconds > gracePeriod)
                updateTable(recordItem.name(), modifiedAfter)
        }
    }

    /**
     * Create a "LIST" transaction for the tableName and records modified after modifiedAfter as
     * seconds after the epoch. Will only list the current logbook and workbook records.
     */
    fun updateTable(tableName: String, modifiedAfter: Double) {
        val apiHandler = ApiHandler.getInstance()
        if ((tableName == "logbook") || (tableName == "workbook")) {
            apiHandler.addNewTxToPending(Transaction.TxType.LIST, tableName,
                mapOf("set" to "client",
                    "modified_after" to modifiedAfter.toString(),
                    "sports_year_start" to
                            Formatter.format(DilboSettings.getInstance().sportsYearStart(),
                                ParserName.DATE, Language.CSV),
                    "logbookname" to DilboSettings.getInstance().currentLogbook()
                ))
        }
        else
            apiHandler.addNewTxToPending(Transaction.TxType.LIST, tableName,
                mapOf("set" to "client", "modified_after" to modifiedAfter.toString()))
    }

    /**
     * Add a listener to the table. The listener is appended. Set upFront to true to prepend it
     */
    fun addListener(tableName: String, listener: Listener, upFront: Boolean = false) {
        val table = getTableOrNull(tableName) ?: return
        if (table.listeners.indexOf(listener) < 0) {
            if (upFront) table.listeners.add(0, listener)
            else table.listeners.add(listener)
        }

    }
    fun removeListener(tableName: String, listener: Listener) {
        val table = getTableOrNull(tableName) ?: return
        table.listeners.remove(listener)
    }

    /**
     * Insert a row. row must have a field "uid" with a value that is not yet used.
     * The table row will have exactly the same values as row, no defaults are added or removed.
     */
    fun insert(tableName: String, row: MutableMap<String, String>) {
        val table = getTableOrNull(tableName) ?: return
        table.insert(row)
    }

    /**
     * Update a row. row must have a field "uid" with a value that is used.
     * The row-values will overwrite the current values of the table row, extra fields
     * of the table row will be kept.
     */
    fun update(tableName: String, row: MutableMap<String, String>) {
        val table = getTableOrNull(tableName) ?: return
        table.update(row)
    }

    /**
     * Update a row. row must have a field "uid" with a value that is used.
     */
    fun delete(tableName: String, uid: String) {
        val table = getTableOrNull(tableName) ?: return
        table.delete(uid)
    }

    /**
     * The value stored in the "modified" field indicates the the record was read from
     * the server.
     */
    private fun isServerStored(row: MutableMap<String,String>): Boolean {
        return (row["modified"]?.isNotEmpty() ?: false) // there is an entry
                && ((row["modified"]?.toDouble() ?: 0.0) > 946684800) // and it is after the 1.1.2000
    }

    /**
     * The value stored in the "modified" field indicates the the record was read from
     * the server.
     */
    fun isServerStored(tableName: String, uid: String): Boolean {
        val row = tables[tableName]?.selectByUid(uid)
        return ((row != null) && isServerStored(row))
    }

    /**
     * Return the existing table with the tableName. If it does not yet exist,
     * but a recordItem is within the configuration, create it. If no child with the tableName
     * exists for the ".tables" configuration item, return null.
     */
    private fun getTableOrNull(tableName: String): Table? {
        val recordItem = Config.getInstance().getItem(".tables.$tableName")
        if (!recordItem.isValid()) {
            return null
        }
        var table = tables[tableName]
        if (table != null)
            return table
        table = Table(recordItem)
        tables[tableName] = table
        return table
    }

    /**
     * Read the csv-table and update or insert ist rows in the table depending on whether
     * a row with the respective uid exists or not. If the table of name tableName does not
     * yet exist and tableName is a valid table name, this will import the table.
     */
    fun merge(tableName: String, csv: String) {
        val table = getTableOrNull(tableName) ?: return
        table.merge(csv)
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given uid or the uid is null.
     */
    internal fun findByUuid(uuid: String?): MutableMap<String, String> {
        _tableOfFindBy = tables[Indices.getInstance().getTableForUuid(uuid ?: "?uuid?")] ?: return mutableMapOf()
        return _tableOfFindBy.selectByUuid(uuid)
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given uid or the uid is null.
     */
    fun findByUid(uid: String?): MutableMap<String, String> {
        _tableOfFindBy = tables[Indices.getInstance().getTableForUid(uid ?: "?uid?")] ?: return mutableMapOf()
        return _tableOfFindBy.selectByUid(uid)
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given name.
     */
    fun selectByName(tableName: String, name: String): MutableMap<String, String> {
        val table = tables[tableName] ?: return mutableMapOf()
        return table.selectByName(name)
    }

    /**
     * Return a clone of the tableRow. Will be an empty map, if there is no table row
     * with the given name.
     */
    fun selectByMatch(tableName: String, row: MutableMap<String, String>): List<MutableMap<String, String>> {
        val table = tables[tableName] ?: return mutableListOf()
        return table.select(table.buildSelector(row))
    }

    /**
     * Select a set of tableRows applying the selector and sorting
     */
    fun select(tableName: String, where: Selector? = null, maxRows: Int = 0, sorting: String = ""):
            List<MutableMap<String, String>> {
        val table = tables[tableName] ?: return mutableListOf()
        val rowsList = table.select(where, maxRows, sorting)
        return if (sorting.isEmpty())
                    rowsList
                else
                    Table.sortRows(sorting, rowsList, table.record.item)
    }

    /**
     * store all changed tables as csv
     */
    fun store() {
        for (tableName in tables.keys)
            tables[tableName]?.store()
    }
}