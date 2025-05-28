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
        private val serverTimes = mutableMapOf<String, Double>()

        /**
         * Return true if the configuration file is invalid. This may happen in case of server
         * error responses
         */
        fun invalidConfigurationFile(fileName: String, contents: String): Boolean {
            // check for correctness of read file. Internet retrieval has chances for hazard.
            // file must have at least a headline
            var fileIsInvalid = (contents.length < 5) // file must have at least a headline
            // ... a matching first word
            // _path for all "normal" settings files, _name fot the types file, name; for the descriptor file
            val firstWord = contents.trim().substring(0, 5).lowercase()
            fileIsInvalid = fileIsInvalid || ((firstWord != "_path") && (firstWord != "_name")
                    && (firstWord != "name;"))
            // ... and a headline with at least 2 entries
            val firstLine = contents.split("\n")[0]
            fileIsInvalid = fileIsInvalid || (firstLine.split(";").size < 2)
            // if it is invalid, remove it from the cache
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
        readServerModified(response)
        val apiHandler = ApiHandler.getInstance()
        for (settingsFile in serverTimes.keys) {
            // only non-basic configuration files are loaded from the server. The program versions
            // of client and server may differ and basic settings are linked to the version
            if (!settingsFile.startsWith("Config/basic")) {
                val localModified =
                    try {
                        LocalCache.getInstance().getItem("$settingsFile.modified").toDouble()
                    } catch (e: Exception) {
                        0.0
                    }
                val serverModified = serverTimes[settingsFile] ?: 0.0
                if (localModified < serverModified) {
                    apiHandler.addNewTxToPending(Transaction.TxType.LIST,
                        ".$settingsFile", emptyMap())
                    apiHandler.logger.log(LoggerSeverity.INFO, "SettingsLoader.onModifiedResponse()",
                        "updating $settingsFile")
                } else
                    apiHandler.logger.log(LoggerSeverity.INFO, "SettingsLoader.onModifiedResponse()",
                        "Skipped $settingsFile, already up-to-date")
            }
        }
        // as a last step get all actual values, labels, descriptions
        apiHandler.addNewTxToPending(Transaction.TxType.LIST,".actuals", emptyMap())
    }

    /**
     * update the local cache with the new settings received.
     */
    fun onSettingsResponse(tx: Transaction) {
        val settingsFile = tx.tableName.substring(1)
        if (invalidConfigurationFile(settingsFile, tx.resultMessage.trim()))
            return // logging was already done
        LocalCache.getInstance().setItem(settingsFile, tx.resultMessage.trim())
        LocalCache.getInstance().setItem("$settingsFile.modified",
            serverTimes[settingsFile].toString())
        val definitionsArray = Codec.csvToMap(tx.resultMessage)
        val topBranchName = settingsFile.substring(settingsFile.lastIndexOf("/") + 1)
        val topBranch = Config.getInstance().rootItem.getChild(topBranchName)
        topBranch?.readBranch(definitionsArray)
    }

    /**
     * update the local cache with the new settings received.
     */
    fun onActualsResponse(response: String) {
        LocalCache.getInstance().setItem("Config/basic/actuals", response)
        val settingsMap = Codec.csvToMap(response)
        Config.getInstance().rootItem.readBranch(settingsMap)
    }
}

