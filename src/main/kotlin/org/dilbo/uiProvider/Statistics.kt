package org.dilbo.uiProvider

import javafx.scene.layout.Region
import org.tfyh.api.ApiHandler
import org.tfyh.api.Transaction
import org.tfyh.data.Item
import org.tfyh.jfx.ui.UIProviderIF

class Statistics(val item: Item): UIProviderIF {

    val viewModel = Model()
    private val optionsMap: MutableMap<String, String> = mutableMapOf()
    // a cache for statistics tables retrieved from the server.

    init {
        val options = item.getChild("options")?.valueStr()?.split(";") ?: mutableListOf()
        for (option in options)
            if (option.isNotEmpty())
                optionsMap[option.split("=")[0]] = option.split("=")[1]
    }
    /**
     * get the statistics from the server for a uiProvider
     */
    @Suppress("Unused")
    internal fun getRankingFromServer(ranking: String) {
        ApiHandler.getInstance().addNewTxToPending(Transaction.TxType.LIST, "statistics",
            mutableMapOf("providerName" to ranking))
    }

    /**
     * read the returned statistics result into the statistics cache
     */
    @Suppress("Unused")
    fun buildStatistics(resultMessage: String) {
        viewModel.setStatistics( resultMessage)
    }

    override fun build(): Region {
        TODO("Not yet implemented")
    }

    companion object {

        /**
         * A view model triggering update twice per second for API status display
         */
        class Model() {
            fun setStatistics(newCsv: String) {
                // TODO
            }
        }
    }

}