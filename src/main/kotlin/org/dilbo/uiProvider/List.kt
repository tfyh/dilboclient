package org.dilbo.uiProvider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import dilboapp.shared.generated.resources.Res
import dilboapp.shared.generated.resources.none
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.dilbo.app.AssetInfos
import org.dilbo.app.UIProviderFactory
import org.tfyh.kmp.ui.AppLabel
import org.tfyh.kmp.ui.ListItem
import org.tfyh.kmp.ui.Stage
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.control.EventHandlerIF
import org.tfyh.control.LoggerSeverity
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import org.tfyh.data.Item
import org.tfyh.data.Table
import org.tfyh.kmp.design.Theme
import kotlin.collections.List
import kotlin.text.isNotEmpty
import kotlin.text.split
import kotlin.text.toInt

/**
 * Provide a list of data, e.g. the open trips.
 */
class List(val item: Item): UIProviderIF, EventHandlerIF {

    val viewModel = ListViewModel()

    private val optionsMap: MutableMap<String, String> = mutableMapOf()
    private val tableName = item.getChild("table")?.valueStr() ?: "_noTable_"

    init {
        val options = item.getChild("options")?.valueStr()?.split(";") ?: mutableListOf()
        for (option in options)
            if (option.isNotEmpty())
                optionsMap[option.split("=")[0]] = option.split("=")[1]
    }

    override fun toString() = "List " + item.name() + "@" + item.path()

    /**
     * Build a list according to the provider settings.
     */
    fun buildList() : List<ListItem> {

        val tableName = item.getChild("table")?.valueStr() ?: return emptyList()
        val template = item.getChild("template")?.valueStr() ?: return emptyList()

        // select the rows
        val config = Config.getInstance()
        val dbc = DataBase.getInstance()
        val selected = dbc.selectByMatch(tableName, optionsMap)

        // apply the asset status filter
        val statusFilter = optionsMap["asset_status"] ?: ""
        val filtered: MutableList<MutableMap<String, String>> =
            if (statusFilter.isNotEmpty()) {
                val buildIt = mutableListOf<MutableMap<String, String>>()
                for (row in selected) {
                    val uuid = if (tableName == "assets") row["uuid"] else row["asset_uuid"]
                    val assetInfo = AssetInfos.getInstance()[uuid]
                    if ((uuid != null) && (assetInfo.places >= 0)) {
                        row["places"] = assetInfo.places.toString()
                        when (statusFilter) {
                            "available" -> if (assetInfo.available) buildIt.add(row)
                            "damaged" -> if (assetInfo.damaged) buildIt.add(row)
                            "booked" -> if (assetInfo.booked) buildIt.add(row)
                            "onthewater" -> if (assetInfo.away) buildIt.add(row)
                        }
                    }
                }
                buildIt
            }
            else selected.toMutableList()

        // sort
        val sort = optionsMap["sort"] ?: ""
        val recordItem = config.getItem(".tables.$tableName")
        val sorted: List<MutableMap<String, String>> =
            if (sort.isNotEmpty()) {
                when (tableName) {
                    "assets" -> {
                        filtered.sortedWith(compareBy(
                            {
                                try { (it["places"]?.toInt() ?: 0) }
                                catch (_: Exception) { 0 }
                            },
                            { it["name"] ?: "no-name" }))
                    }
                    "logbook" -> Table.sortRows("start.number", filtered, recordItem)
                    else -> Table.sortRows("name.number", filtered, recordItem)
                }
            } else
                filtered

        // build new list
        val listElements = mutableListOf<ListItem>()
        val groupBy = optionsMap["groupBy"] ?: ""
        var lastGroup = ""
        for (element in sorted) {
            val elementUuid = if (tableName == "assets") element["uuid"] else element["asset_uuid"]
            val elementUid = element["uid"] ?: "?uid?"
            val group = element[groupBy] ?: ""
            if (group != lastGroup) {
                listElements.add(
                    ListItem("", Res.drawable.none, group, this.item, evenRow = true)
                )
                lastGroup = group
            }
            val icon = AssetInfos.getInstance()[elementUuid].iconResource
            listElements.add(
                ListItem(elementUid, icon, UIProviderFactory.fill(template, element), this.item)
            )
        }
        return listElements.toList()
    }

    /**
     * Compose the list
     */
    @Composable
    override fun ComposeContent() {
        val versionState by remember { mutableStateOf(viewModel.version) }
        val version = versionState.collectAsState().value
        if (version > 0)
            Column {
                Box (modifier = Modifier.fillMaxWidth()
                    .background(Theme.colors.color_background_body)
                    .padding(start = Theme.dimensions.smallSpacing * 2))
                {
                    AppLabel(item.label(), large = true,
                        color = Theme.colors.color_text_h1_h3,
                        textAlign = TextAlign.Center)
                }
                Column (
                    modifier = Modifier.verticalScroll(rememberScrollState())
                        .padding(start = Theme.dimensions.smallSpacing,
                            end = Theme.dimensions.smallSpacing)
                ) {
                    val list = buildList()
                    for (element in list)
                        element.Compose()
                }
            }
    }

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if (event.sourceItem.name() == tableName) {
            Stage.logger.log(LoggerSeverity.INFO, "List.changed()",
                "Notified $this.")
            viewModel.show()
        }
        return true
    }

    companion object {
        /**
         * A view model triggering update twice per second for API status display
         */
        class ListViewModel: ViewModel() {
            private val _version : MutableStateFlow<Int> = MutableStateFlow(1)
            val version : StateFlow<Int> = _version

            fun show() = _version.update { _version.value + 1 }
            fun hide() = _version.update { 0 }
        }
    }

}