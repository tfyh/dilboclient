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

import javafx.application.Platform
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.dilbo.core.AssetInfos
import org.dilbo.core.EventHandler
import org.dilbo.core.UIProviderFactory
import org.dilbo.storage.ResProvider
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import org.tfyh.data.Item
import org.tfyh.data.Table
import org.tfyh.jfx.design.AppColours
import org.tfyh.jfx.design.AppDimensions
import org.tfyh.jfx.design.AppFonts
import org.tfyh.jfx.ui.ListItem
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.jfx.ui.appLabel
import java.io.ByteArrayInputStream
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.removeAll
import kotlin.collections.set
import kotlin.collections.sortedWith
import kotlin.collections.toList
import kotlin.collections.toMutableList
import kotlin.collections.toTypedArray
import kotlin.text.isNotEmpty
import kotlin.text.split
import kotlin.text.toInt

/**
 * Provide a list of data, e.g. the open trips.
 */
class List(val item: Item, val dimensions: ViewBuilder.Dimensions): UIProviderIF, EventHandlerIF {

    private val optionsMap: MutableMap<String, String> = mutableMapOf()
    private val tableName = item.getChild("table")?.valueStr() ?: "_noTable_"
    private val header = appLabel(item.label(), item, AppFonts.h2)
    private val collapsed = mutableListOf<String>()

    private val listPane = VBox().apply {
        border = AppColours.borderNone()
        background = AppColours.backgroundTransparent()
    }
    private val scrollPane = ScrollPane(listPane).apply {
        border = AppColours.borderNone()
        background = AppColours.backgroundTransparent()
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        isPannable = true
    }
    private val listView = VBox(header, scrollPane)


    init {
        val options = item.getChild("options")?.valueStr()?.split(";") ?: mutableListOf()
        for (option in options)
            if (option.isNotEmpty())
                optionsMap[option.split("=")[0]] = option.split("=")[1]
        ViewBuilder.getInstance().registerHandler(this, EventHandlerIF.EventType.DATA_EVENT)
        ViewBuilder.getInstance().registerHandler(this, EventHandlerIF.EventType.UI_EVENT)
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
        val statusFilter = optionsMap["asset_status"] ?: // for the available/damaged boats list
                    (if (optionsMap["end"] == "") "onthewater" else "") // for the open trips list
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
        val resProvider = ResProvider.getSingleton()
        val noneIcon =
            Image(ByteArrayInputStream(resProvider.getDrawable("none.png")))

        var even = false
        var collapsedGroup = false
        for (element in sorted) {
            val elementUuid = if (tableName == "assets") element["uuid"] else element["asset_uuid"]
            val elementUid = element["uid"] ?: "?uid?"
            val group = element[groupBy] ?: ""
            if (group != lastGroup) {
                listElements.add(
                    ListItem(group, noneIcon, group, item, EventHandler.getSingleton(),
                        headerRow = true, evenRow = false)
                )
                lastGroup = group
                even = false
                collapsedGroup = collapsed.contains(group)
            }
            val icon = AssetInfos.getInstance()[elementUuid].iconResource
            if (!collapsedGroup)
                listElements.add(
                    ListItem(elementUid, icon, UIProviderFactory.fill(template, element), this.item,
                        EventHandler.getSingleton(), headerRow = false, evenRow = even)
            )
            even = !even
        }
        return listElements.toList()
    }

    override var isDisplayedOn: Pane? = null
        get() = field // Access the backing field
        set(value) { field = value } // Modify the backing field

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if (event.type == EventHandlerIF.EventType.DATA_EVENT) {
            if ((event.sourceItem.name() == tableName) || (event.name == "assetInfosRebuilt")) {
                Platform.runLater { buildPane() }
                return true
            }
        } else if (event.type == EventHandlerIF.EventType.UI_EVENT) {
            if ((event.sourceItem == item) && (event.name == "collapse")) {
                val group = event.uid
                if (collapsed.indexOf(group) != -1)
                    collapsed.remove(group)
                else
                    collapsed.add(group)
                build()
                return true
            }
        }
        return false
    }

    fun buildPane() {
        val listItems = mutableListOf<Pane>()
        val list = buildList()
        for (element in list)
            listItems.add(element.build().apply {
                prefWidth = dimensions.widthPx - 2 * AppDimensions.REGULAR_SPACING
            })
        listPane.children.removeAll { true }
        listPane.children.addAll(*listItems.toTypedArray())
    }

    override fun build(): Pane {
        buildPane()
        isDisplayedOn = listPane
        return listView.apply {
            prefWidth = dimensions.widthPx.toDouble()
            border = AppColours.borderNone()
            background = AppColours.backgroundTransparent()
        }
    }

}