package org.dilbo.uiProvider

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tfyh.api.ApiHandler
import org.tfyh.api.Transaction
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.data.Item

class Statistics(val item: Item): UIProviderIF {

    val viewModel = StatisticsViewModel()
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

    @Composable
    override fun ComposeContent() {
        TODO("Not yet implemented")
    }
    companion object {

        /**
         * A view model triggering update twice per second for API status display
         */
        class StatisticsViewModel : ViewModel() {
            private val _type = MutableStateFlow("") // private mutable state flow
            val type = _type.asStateFlow() // publicly exposed as read-only state flow
            private val _csv = MutableStateFlow("") // private mutable state flow
            val csv = _csv.asStateFlow() // publicly exposed as read-only state flow

            fun setStatistics(newCsv: String) {
                _csv.update { newCsv }
            }
        }
    }

}