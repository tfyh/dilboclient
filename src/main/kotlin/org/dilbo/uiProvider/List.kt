package org.dilbo.uiProvider

import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.dilbo.app.AssetInfos
import org.dilbo.app.ResProvider
import org.dilbo.app.UIProviderFactory
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import org.tfyh.data.Item
import org.tfyh.data.Table
import org.tfyh.jfx.ui.ListItem
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.appLabel
import java.io.ByteArrayInputStream
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
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
class List(val item: Item): UIProviderIF, EventHandlerIF {

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
        val resProvider = ResProvider.getSingle()
        val noneIcon =
            Image(ByteArrayInputStream(resProvider.getDrawable("none")))

        for (element in sorted) {
            val elementUuid = if (tableName == "assets") element["uuid"] else element["asset_uuid"]
            val elementUid = element["uid"] ?: "?uid?"
            val group = element[groupBy] ?: ""
            if (group != lastGroup) {
                listElements.add(
                    ListItem("", noneIcon, group, this.item, evenRow = true)
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

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if (event.sourceItem.name() == tableName) {
            //TODO
        }
        return true
    }

    override fun build(): Region {
        val listItems = mutableListOf<Region>()
        val list = buildList()
        for (element in list)
            listItems.add(element.build())
        val header = appLabel(item.label(), "h3")
        val listView = VBox(header, ScrollPane(VBox(*listItems.toTypedArray())))
        return listView
    }

}