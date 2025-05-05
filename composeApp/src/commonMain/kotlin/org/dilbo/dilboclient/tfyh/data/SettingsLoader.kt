package org.dilbo.dilboclient.tfyh.data

import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.api.Transaction
import org.dilbo.dilboclient.tfyh.control.LoggerSeverity
import org.dilbo.dilboclient.tfyh.util.LocalCache

class SettingsLoader {

    /**
     * The companion object holds the configuration modification times. It provides a function to
     * validate the configuration files in order to be able to distinguish them from server error
     * responses
     */
    companion object {
        // the path in the local cache must be identical to the settings files' path at the server side
        private const val CACHE_PATH = "Config"
        val settingsFiles = listOf( "modified", "descriptor", "types", "access", "framework",
            "templates", "tables", "app", "catalogs", "lists", "ui" )
        private val serverTimes = mutableMapOf<String, Double>()
        private val localTimes = mutableMapOf<String, Double>()

        /**
         * Return true if the configuration file is invalid. This may happen in case of server
         * error responses
         */
        fun invalidConfigurationFile(fileName: String, contents: String): Boolean {
            // check for correctness of read file. Internet retrieval has chances for hazard.
            if (contents.length < 5) // file must have at least a headline
                return true
            val firstWord = contents.trim().substring(0, 5).lowercase()
            var fileIsInvalid = ((firstWord != "_path") && (firstWord != "_name")
                    && (firstWord != "name;"))
            // _path for all "normal" settings files, _name fot the types file, name; for the descriptor file
            val firstLine = contents.split("\n")[0]
            fileIsInvalid = fileIsInvalid || (firstLine.split(";").size < 2) // ... a headline with at least 2 entries
            if (fileIsInvalid) {
                LocalCache.getInstance().removeItem(fileName)
                LocalCache.getInstance().removeItem("$fileName.modified")
                Config.getInstance().logger.log(LoggerSeverity.ERROR,
                    "SettingsLoader.invalidConfigurationFile()",
                    "Invalid configuration file " + fileName + ". Starts with '" +
                            contents.substring(0, 100) + " ...'." + "The file was removed.")
                return true
            }
            return false
        }
    }

    /**
     * Read the modified timestamps from local cache
     */
    private fun readLocalModified() {
        val lc = LocalCache.getInstance()
        for (settingsFile in settingsFiles) {
            try {
                localTimes["$CACHE_PATH/$settingsFile"] = lc.getItem("Config/$settingsFile.modified").toDouble()
            } catch (e: Exception) {
                localTimes["$CACHE_PATH/$settingsFile"] = 0.0
            }
        }
    }

    /**
     * Read the server modified timestamps using the transaction response
     */
    private fun readServerModified(response: String) {
        val times = response.split("\n")
        for (timeStr in times) {
            val name = timeStr.split("=")[0]
            val timeDbl = try {
                timeStr.split("=")[1].toDouble()
            } catch (e: Exception) {
                0.0
            }
            serverTimes[name] = timeDbl
        }
    }

    /**
     * Append an API transaction to read the configuration files' last modified timestamps
     */
    fun requestModified() {
        ApiHandler.getInstance().addNewTxToPending(Transaction.TxType.LIST, ".modified", emptyMap())
    }

    /**
     * Read the server response with the configuration files' last modified timestamp andTrigger a
     * configuration read request, if a server modification is more recent than the local time stamp.
     */
    fun onModifiedResponse(response: String) {
        readLocalModified()
        readServerModified(response)
        for (settingsFile in settingsFiles)
            if (settingsFile !== "modified") {
                val localModified = localTimes["$CACHE_PATH/$settingsFile"] ?: 0.0
                val serverModified = serverTimes["$CACHE_PATH/$settingsFile"] ?: 0.0
                if (localModified < serverModified)
                    ApiHandler.getInstance()
                        .addNewTxToPending(Transaction.TxType.LIST, ".$settingsFile", emptyMap())
            }
    }

    /**
     * update the local cache with the new settings received.
     */
    fun onSettingsResponse(tx: Transaction) {
        val settingsFile = tx.tableName.substring(1)
        if (invalidConfigurationFile(settingsFile, tx.resultMessage.trim()))
            return // logging was already done
        LocalCache.getInstance().setItem("$CACHE_PATH/$settingsFile", tx.resultMessage.trim())
        LocalCache.getInstance().setItem( "$CACHE_PATH/$settingsFile.modified",
            serverTimes["$CACHE_PATH/$settingsFile"].toString())
        val definitionsArray = Codec.csvToMap(tx.resultMessage)
        val topBranch = Config.getInstance().rootItem.getChild(settingsFile)
        topBranch?.readBranch(definitionsArray)
    }

}

