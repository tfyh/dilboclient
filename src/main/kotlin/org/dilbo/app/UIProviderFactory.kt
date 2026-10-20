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

import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.dilbo.uiProvider.*
import org.dilbo.uiProvider.List
import org.tfyh.api.ApiHandler
import org.tfyh.data.*
import org.tfyh.jfx.ui.ViewBuilder.Dimensions
import org.tfyh.jfx.ui.UIProviderFactoryIF
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.jfx.ui.appLabel
import org.tfyh.util.I18n
import org.tfyh.util.Language
import org.tfyh.util.User
import io.ktor.client.HttpClient

class UIProviderFactory: UIProviderFactoryIF {


    companion object {

        private var httpClient = HttpClient()
        fun setHttpClient(httpClient: HttpClient) { this.httpClient = httpClient }

        // STATIC FUNCTIONS FOR TEXT FORMATTING
        // ====================================
        /**
         * Fill a template string with available values. Use the uid to get the row's definition.
         */
        fun fill(template: String, row: Map<String, String>): String {

            val config = Config.getInstance()
            var filled = template.replace("~\\", "\n")

            // check special text
            filled = filled.replace("{welcomeMessage}", ApiHandler.getInstance().welcomeMessage)

            // check for configuration path elements, whether a replacement is requested
            var c1 = filled.indexOf ("{.")
            var c2 = filled.indexOf ("}", c1)
            while ((c1 >= 0) && (c2 >= 0)) {
                val configPath = filled.substring (c1 + 1, c2)
                val configItem = config.getItem(configPath)
                if (configItem.isValid()) {
                    filled = filled.substring(0, c1) + configItem.valueStr() + filled.substring(c2 + 1)
                    c2 = 0
                }
                c1 = filled.indexOf("{.", c2)
                c2 = filled.indexOf("}", c1)
            }

            // check for row fields next, whether a replacement is requested
            if (row.isEmpty())
                return filled
            val uid = row["uid"] ?: return filled
            val tableName = Indices.getInstance().getTableForUid(uid)
            val recordItem = config.getItem(".tables.$tableName")
            if (!recordItem.isValid())
                return filled
            val record = Record(recordItem)
            record.parse(row, language = Language.CSV)

             for (key in row.keys) {
                val search = "{$key}"
                if (filled.indexOf(search) >= 0) {
                    val columnItem = recordItem.getChild(key)
                    val toDisplay =
                        if (columnItem != null)
                            record.valueToDisplay(columnItem, config.language())
                        else
                            ""
                    filled = filled.replace(search, toDisplay)
                }
            }
            return filled
        }

        // DIALOG DISPLAY
        // ==============
        /**
         * Display a dialogue with a notice
         */
        fun displayDialog(headline: String, text: String) {
            ViewBuilder.getInstance().showDialogue("~*$headline*~~\\$text")
        }

        // DETAILS DISPLAY
        // ===============
        /**
         * Display the details for a record in the modal. This does not depend on trigger source.
         */
        fun displayDetails() {

            val uid = EventHandler.getInstance().getLastUid()
            val db = DataBase.getInstance()
            val indices = Indices.getInstance()
            val tableName = indices.getTableForUid(uid)
            val row = db.select(tableName,
                Table.Selector(fieldName = "uid", compare = Table.Comparison.EQUAL, filter = uid),
                1).getOrNull(0) ?: mutableMapOf()

            val viewBuilder = ViewBuilder.getInstance()
            val modalWidth = viewBuilder.getModalDimensions().widthPx
            val config = Config.getInstance()
            val recordItem = config.getItem(".tables.$tableName")
            if (recordItem.isValid()) {

                val record = Record(recordItem)
                record.parse(row, Language.CSV)

                val heading = appLabel(recordItem.label() + ": " + record.recordToTemplate("name"),
                    "h2")
                val details = mutableListOf<HBox>()
                for ((i, field) in recordItem.getChildren().withIndex()) {
                    if (!ParserConstraints.isEmpty(record.value(field.name()),
                            field.type().parser())) {
                        val style = if ((i % 2) == 1) ".evenRow" else ".oddRow"
                        val toShow =
                            if ((field.name() != "uuid") && (field.name() != "uid"))
                                record.valueToDisplay(field, config.language())
                            else
                                record.formatValue(field, config.language())
                        val fieldLabel = appLabel("~*" + field.label() + "*~", "p")
                        fieldLabel.prefWidth = modalWidth * 0.4
                        val valueLabel = appLabel(toShow, "p")
                        HBox.setHgrow(valueLabel, Priority.ALWAYS)
                        val detail = HBox(fieldLabel, valueLabel)
                        detail.styleClass += style
                        details.add(detail)
                    }
                }

                val detailsPane = ScrollPane(VBox(*details.toTypedArray()))
                val content = VBox(heading, detailsPane)
                viewBuilder.showModal(content)

            }
        }

        /**
         * Get a summary of the current dilbo installation for display in the dialogue
         */
        fun dilboAbout(): String {
            val config = Config.getInstance()
            val appURL = config.appUrl
            val appVersion = config.appVersion
            val language = config.language()
            val localFilePath = config.localFilePath
            return "the digital logbook for rowing and canoeing~\\" +
                    "\u00a9 $appURL~\\" +
                    "version: $appVersion~\\" +
                    "language: $language~\\" +
                    "local file path: $localFilePath~\\" +
                    "server URL: " +  ApiHandler.getInstance().getUrl() + "~\\" +
                    "server: " + ApiHandler.getInstance().welcomeMessage.replace("//", "~\\")
        }

    }


    /**
     * get the provider instance for a sector using the given configuration item
     */
    override fun getProvider(item: Item, dimensions: Dimensions): UIProviderIF? {

        if (item == Config.getInstance().invalidItem)
            return Warning("-???-")

        val providerName = item.value()
        val providerItem = Config.getInstance().getItem(".ui.providers.$providerName")
        val type: String = providerItem.getChild("type")?.valueCsv() ?: "noProvider"
        val permissionsString: String = item.getChild("permissions")?.valueStr() ?: "anonymous"
        val isDisplayAllowed = User.getInstance().isAllowedItem(permissionsString)

        return if (!isDisplayAllowed) {
            val message = if (User.getInstance().userId() > 0)
                I18n.getInstance().t("aJAUJg|Sorry, no access to °%1°", item.label())
            else ""
            Warning(message)
        } else when (type) {
            "menu" -> Menu(providerItem, dimensions, ResProvider.getSingle())
            "list" -> List(providerItem)
            "statistics" -> Statistics(providerItem)
            "clock" -> Clock(providerItem)
            "apiStatus" -> ApiStatus(providerItem, dimensions)
            "textView" -> TextView(providerItem)
            "imageView" -> RemoteImage(providerItem, httpClient)
            "noProvider" -> null
            else -> Warning(
                I18n.getInstance().t("Cannot display invalid ui-provider type '%1'", type)
            )
        }
    }

}
