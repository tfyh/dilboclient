/**
 *
 *       dilbo - digital logbook for Rowing and Canoeing
 *       -----------------------------------------------
 *       https://www.dilbo.org
 *
 * Copyright  2023-2024  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dilbo.app

import javafx.scene.image.Image
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import org.tfyh.data.Parser
import org.tfyh.data.ParserName
import org.tfyh.data.Table
import org.tfyh.util.Language
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.NoZoneDateTime
import java.io.ByteArrayInputStream

class AssetInfos {


    companion object {

        val assetInfoTables = listOf("assets", "logbook", "damages", "reservations")

        private val instance = AssetInfos()
        fun getInstance() = instance
    }

    data class AssetInfo(
        var baseStatus: String = "AVAILABLE",
        var hidden: Boolean = false, // reflects baseStatus == "HIDE"
        var unavailable: Boolean = false, // reflects baseStatus == "NOTAVAILABLE"
        var away: Boolean = false, // is set true, if an open trip exists with this asset
        var booked: Boolean = false, // is set true, if a reservation exists with this asset for now
        var bookedLater: Boolean = false, // is set true, if a future reservation exists with this asset
        var damaged: Boolean = false, // is set true, if unfixed damages of severity "NOTUSEABLE" exists with this asset
        var damagedUsable: Boolean = false, // is set true, if unfixed damages of any other severity exists with this asset
        var available: Boolean = false, // reflects baseStatus == "AVAILABLE" && !away && !booked && !damaged
        var iconResource: Image = Image(ByteArrayInputStream(ResProvider.getSingle().getDrawable("none"))),
        var places: Int = -1 // collected from the first boat variant
    )

    private val assetInfoList: MutableMap<String, AssetInfo> = mutableMapOf()

    private fun getAssetInfo(uuid: String?): AssetInfo {
        if (uuid == null)
            return AssetInfo() // this will create an AssetInfo which is not attached to the
        // assetInfoList, i.e. which will be ignored.
        var assetInfo = AssetInfo()
        if (assetInfoList[uuid] == null)
            assetInfoList[uuid] = assetInfo
        else
            assetInfo = assetInfoList[uuid] ?: AssetInfo()
        return assetInfo
    }

    /**
     * Collect the asset status for all assets by scanning the logbook, damages and reservations,
     * or update some based on the changed table
     */
     fun build(changedTable: Table? = null) {

        val changedTableName = changedTable?.record?.item?.name() ?: ""
        val singleTable = changedTableName.isNotEmpty()
        if (!singleTable)
            assetInfoList.clear()

        var selector: Table.Selector
        // damaged
        if (!singleTable || (changedTableName == "damages")) {
            selector = Table.Selector(
                children = mutableListOf(
                    Table.Selector("fixed", Table.Comparison.EQUAL, false),
                    Table.Selector("severity", Table.Comparison.EQUAL, "NOTUSEABLE")
                ), childrenAnd = true
            )
            val damaged = DataBase.getInstance().select("damages", selector)
            for (assetInfoUuid in assetInfoList.keys)
                assetInfoList[assetInfoUuid]?.damaged = false
            for (row in damaged)
                getAssetInfo(row["asset_uuid"]).damaged = true
        }

        // damaged, but still usable
        if (!singleTable || (changedTableName == "damages")) {
            selector = Table.Selector(
                children = mutableListOf(
                    Table.Selector("fixed", Table.Comparison.EQUAL, false),
                    Table.Selector("severity", Table.Comparison.NOT_EQUAL, "NOTUSEABLE")
                ), childrenAnd = true
            )
            val usable = DataBase.getInstance().select("damages", selector)
            for (assetInfoUuid in assetInfoList.keys)
                assetInfoList[assetInfoUuid]?.damagedUsable = false
            for (row in usable)
                getAssetInfo(row["asset_uuid"]).damagedUsable = true
        }

        // away on the water
        if (!singleTable || (changedTableName == "logbook")) {
            selector = Table.Selector("end", Table.Comparison.IS_EMPTY, "")
            val away = DataBase.getInstance().select("logbook", selector)
            for (assetInfoUuid in assetInfoList.keys)
                assetInfoList[assetInfoUuid]?.away = false
            for (row in away)
                getAssetInfo(row["asset_uuid"]).away = true
        }

        // currently booked (always rebuild due to the change of "now")
        val now: NoZoneDateTime = NoZoneDateTime.now()
        selector = Table.Selector(
            children = mutableListOf(
                Table.Selector("start", Table.Comparison.LOWER_OR_EQUAL, now),
                Table.Selector("end", Table.Comparison.GREATER_OR_EQUAL, now)
            ), childrenAnd = true
        )
        val booked = DataBase.getInstance().select("reservations", selector)
        for (assetInfoUuid in assetInfoList.keys)
            assetInfoList[assetInfoUuid]?.booked = false
        for (row in booked)
            getAssetInfo(row["asset_uuid"]).booked = true

        // with a future reservation (always rebuild due to the change of "now")
        selector = Table.Selector("start", Table.Comparison.GREATER_THAN, now)
        val bookedLater = DataBase.getInstance().select("reservations", selector)
        for (assetInfoUuid in assetInfoList.keys)
            assetInfoList[assetInfoUuid]?.bookedLater = false
        for (row in bookedLater)
            getAssetInfo(row["asset_uuid"]).bookedLater = true

        // add all other assets, the icon, places and base status, remove invalid boat uuids
        val assets = DataBase.getInstance().select(
            "assets",
            Table.Selector("invalid_from", Table.Comparison.IS_EMPTY, "")
        )
        val boatVariants = Config.getInstance().getItem(".catalogs.boat_variants")
        for (asset in assets) {
            val uuid = asset["uuid"]
            if (uuid != null) {
                if (assetInfoList[uuid] == null)
                    assetInfoList[uuid] = AssetInfo()
                val assetInfo = assetInfoList[uuid]
                if (assetInfo != null) {
                    // index on uuid and shortUuid used.
                    val shortUuid = uuid.substring(0, 11)
                    if (assetInfoList[shortUuid] == null)
                        assetInfoList[shortUuid] = assetInfo
                    assetInfo.baseStatus = asset["base_status"] ?: ""
                    if (assetInfo.baseStatus.isEmpty())
                        assetInfo.baseStatus = "AVAILABLE"
                    assetInfo.unavailable = (assetInfo.baseStatus == "NOTAVAILABLE")
                    assetInfo.hidden = (assetInfo.baseStatus == "HIDE")
                    assetInfo.available =
                        (assetInfo.baseStatus == "AVAILABLE") && !assetInfo.away &&
                                !assetInfo.booked && !assetInfo.damaged
                    assetInfo.iconResource =
                        if (assetInfo.damaged)
                            Image(ByteArrayInputStream(ResProvider.getSingle().getDrawable("boat_damaged")))
                        else if (assetInfo.booked)
                            Image(ByteArrayInputStream(ResProvider.getSingle().getDrawable("boat_booked")))
                        else if (assetInfo.away)
                            Image(ByteArrayInputStream(ResProvider.getSingle().getDrawable("boat_onthewater")))
                        else
                            Image(ByteArrayInputStream(ResProvider.getSingle().getDrawable("boat_available")))
                    val variantOptions = Parser.parse(
                        asset["variant_options"] ?: "",
                        ParserName.STRING_LIST, Language.CSV
                    ) as List<*>
                    val firstVariantName = (variantOptions.getOrNull(0) as String?) ?: ""
                    assetInfo.places =
                        (boatVariants.getChild(firstVariantName)?.getChild("places")
                            ?.value() as Int?) ?: 1
                }
            }
        }
    }

    /**
     * Get the asset information for a single asset. If the uuid has no match, an empty
     * asset information is returned. It can be identified by the value of places = -1
     */
    operator fun get(uuid: String?): AssetInfo {
        return assetInfoList[uuid] ?: AssetInfo()
    }

    fun handleEvent(event: EventHandlerIF.Event): Boolean {
        val tableName = event.sourceItem.name()
        val table = DataBase.getInstance().getTableOrNull(tableName) ?: return false
        if (assetInfoTables.indexOf(table.record.item.name()) >= 0)
                build(table)
        return true
    }

}
