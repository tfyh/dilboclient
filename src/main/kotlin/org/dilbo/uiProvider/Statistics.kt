/**
 * dilbo - digital logbook for Rowing and Canoeing
 * https://www.dilbo.org
 * Copyright:  2023-2025  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.dilbo.uiProvider

import javafx.scene.layout.Pane
import org.tfyh.api.ApiHandler
import org.tfyh.api.Transaction
import org.tfyh.data.Item
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder

class Statistics(val item: Item, val dimensions: ViewBuilder.Dimensions): UIProviderIF {

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
        viewModel.setStatistics(resultMessage)
    }

    override fun build(): Pane {
        TODO("Not yet implemented")
    }

    companion object {

        /**
         * A view model triggering update twice per second for API status display
         */
        class Model {
            fun setStatistics(newCsv: String) {
                // TODO
            }
        }
    }

}