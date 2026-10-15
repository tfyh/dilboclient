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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.dilbo.uiProvider.ImageView
import org.dilbo.uiProvider.List
import org.dilbo.uiProvider.Menu
import org.dilbo.uiProvider.Warning
import org.dilbo.uiProvider.Statistics
import org.dilbo.uiProvider.TextView
import org.tfyh.api.ApiHandler
import org.tfyh.kmp.ui.AppLabel
import org.tfyh.kmp.ui.Dimensions
import org.tfyh.kmp.ui.Stage
import org.tfyh.kmp.ui.SubmitButton
import org.tfyh.kmp.design.Theme
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import org.tfyh.data.Indices
import org.tfyh.data.Item
import org.tfyh.data.ParserConstraints
import org.tfyh.data.Record
import org.tfyh.data.Table
import org.tfyh.util.I18n
import org.tfyh.util.Language
import org.tfyh.util.User
import org.tfyh.kmp.ui.UIProviderFactoryIF
import org.tfyh.kmp.ui.UIProviderIF

class UIProviderFactory: UIProviderFactoryIF {


    companion object {

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
            Stage.dialogContent = {
                Column(modifier = Modifier.fillMaxWidth().padding(Theme.dimensions.largeSpacing)) {
                    AppLabel(headline, large = true)
                    Spacer(modifier = Modifier.height(Theme.dimensions.regularSpacing))
                    AppLabel(text)
                    Spacer(modifier = Modifier.height(Theme.dimensions.regularSpacing))
                    SubmitButton(text = "Ok", onClick = { Stage.viewModel.setVisibleDialog(false) } )
                }
            }
            Stage.viewModel.setVisibleDialog(true)
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
            val config = Config.getInstance()
            val recordItem = config.getItem(".tables.$tableName")
            if (recordItem.isValid()) {
                val record = Record(recordItem)
                record.parse(row, Language.CSV)
                Stage.viewModel.setVisibleModal(false)
                val modalWidth = Stage.getModalMaxDimensions().widthDp()
                Stage.modalContent = {
                    Box(Modifier.padding(start = Theme.dimensions.regularSpacing, top =
                        Theme.dimensions.regularSpacing, bottom = Theme.dimensions.regularSpacing,
                        end = Theme.dimensions.smallSpacing)) {
                        Column (modifier = Modifier.width(modalWidth)
                            .padding(start = Theme.dimensions.largeSpacing, end = Theme.dimensions.largeSpacing)) {
                            Spacer(Modifier.height(Theme.dimensions.regularSpacing))
                            AppLabel(text = recordItem.label() + ": " + record.recordToTemplate("name"),
                                large = true, color = Theme.colors.color_text_h1_h3, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(Theme.dimensions.regularSpacing))
                            Column (
                                modifier = Modifier.wrapContentSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val evenRowsModifier = Modifier.background(Theme.colors.color_background_body)
                                for ((i, field) in recordItem.getChildren().withIndex()) {
                                    if (!ParserConstraints.isEmpty(record.value(field.name()),
                                            field.type().parser()))
                                        Row {
                                            val rowModifier = if ((i % 2) == 1) evenRowsModifier else Modifier
                                            AppLabel(text = field.label() + ": ",
                                                modifier = rowModifier.width(modalWidth * 0.4F))
                                            val toShow =
                                                if ((field.name() != "uuid") && (field.name() != "uid"))
                                                    record.valueToDisplay(field, config.language())
                                                else
                                                    record.formatValue(field, config.language())
                                            AppLabel(text = toShow,
                                                modifier = Modifier.width(modalWidth * 0.55F))

                                        }
                                }
                                Spacer(Modifier.height(Theme.dimensions.smallSpacing))
                                SubmitButton("Ok") {
                                    Stage.viewModel.setVisibleModal(false)
                                }
                                Spacer(Modifier.height(Theme.dimensions.regularSpacing))
                            }
                        }
                    }
                }
                Stage.viewModel.setVisibleModal(true)
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
            "menu" -> Menu(providerItem, dimensions)
            "list" -> List(providerItem)
            "statistics" -> Statistics(providerItem)
            "clock" -> org.dilbo.uiProvider.Clock(providerItem)
            "apiStatus" -> org.dilbo.uiProvider.ApiStatus(providerItem, dimensions)
            "textView" -> TextView(providerItem)
            "imageView" -> ImageView(providerItem, dimensions)
            "noProvider" -> null
            else -> Warning(
                I18n.getInstance().t("Cannot display invalid ui-provider type '%1'", type)
            )
        }
    }

}
