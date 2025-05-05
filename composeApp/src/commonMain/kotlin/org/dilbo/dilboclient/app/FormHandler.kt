package org.dilbo.dilboclient.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.app.UIProvider.Companion.menuItems
import org.dilbo.dilboclient.composable.ModalCloseButton
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.DataBase
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Indices
import org.dilbo.dilboclient.tfyh.data.Item
import org.dilbo.dilboclient.tfyh.data.Parser
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.data.Record
import org.dilbo.dilboclient.tfyh.util.Form
import org.dilbo.dilboclient.tfyh.util.FormFieldListener
import org.dilbo.dilboclient.tfyh.util.I18n
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.User
import org.jetbrains.compose.resources.painterResource

object FormHandler {

    val config = Config.getInstance()
    private var lang = config.language()
    private var recordItem: Item = config.invalidItem
    private var formDefinition: String = ""
    private var form = Form(Config.getInstance().invalidItem, "") {}

    private var eventUid: String = ""
    private var eventRecord: Record = Record(config.invalidItem)
    private var eventTableName: String = eventRecord.item.name()

    fun responsiveFormColumnsCount() = form.responsiveFormColumnsCount()

    // ==============================
    // COMMON PRE- AND POSTPROCESSING
    // ==============================
    private fun setEventContext() {
        eventUid = UIEventHandler.getInstance().getLastUid()
        val db = DataBase.getInstance()
        val row = db.findByUid(eventUid)
        val tableRecord = config.getItem(".tables.${db.tableOfFindBy()}")
        eventRecord = Record(tableRecord)
        eventRecord.parse(row, Language.CSV, false)
        eventTableName = eventRecord.item.name()
        lang = config.language()
    }
    fun hideForm() {
        form.viewModel.setVisible(false)
        Stage.modalContent = Stage.emptyModalContent
        Stage.viewModel.setVisibleModal(false)
    }
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun menuItemSet(menuItemsNames: List<String>, selected: Int) {
        val uiEventHandler = UIEventHandler.getInstance()
        val formVisible = form.viewModel.show.collectAsStateWithLifecycle()
        var i = 0;
        if (formVisible.value > 0) {
            ModalCloseButton()
            for (menuItemName in menuItemsNames) {
                val menuItem = menuItems[menuItemName]
                if ((menuItem != null) && menuItem.isAllowedMenuItem()) {
                    Box(
                        modifier = Modifier.padding(Theme.dimensions.smallSpacing)
                    ) {
                        val modifier =
                            if (i == selected)
                                Modifier
                                    .background(Theme.colors.color_background_form_input)
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            Theme.colors.color_border_form_input
                                        )
                                    )
                            else Modifier
                        Image(
                            painter = painterResource(menuItem.icon),
                            contentDescription = menuItemName,
                            modifier = modifier
                                .width(Theme.dimensions.textFieldHeight)
                                .combinedClickable(
                                    onClick = {
                                        uiEventHandler.handleUiEvent(
                                            UIEventHandler.UIEvent(
                                                eventUid,
                                                menuItem.menuItemConfig.name(),
                                                menuItem.menuItemConfig
                                            )
                                        )
                                    }
                                )
                        )
                    }
                }
                i++
            }
        }
    }

    @Composable
    fun FormAndMenu(menuItemsNames: List<String>, selected: Int) {
        if (Stage.isPortrait())
            Column {
                Row(
                    modifier = Modifier
                        .padding(Theme.dimensions.regularSpacing)
                        .wrapContentSize()
                ) {
                    menuItemSet(menuItemsNames, selected)
                }
                Box(
                    modifier = Modifier
                        .padding(Theme.dimensions.smallSpacing)
                        .wrapContentSize()
                ) {
                    form.compose()
                }
            }
        else
            Row {
                Column(
                    modifier = Modifier
                        .padding(Theme.dimensions.regularSpacing)
                        .wrapContentSize()
                ) {
                    menuItemSet(menuItemsNames, selected)
                }
                Box(
                    modifier = Modifier
                        .padding(Theme.dimensions.smallSpacing)
                        .wrapContentSize()
                ) {
                    form.compose()
                }
            }
    }

    // ==========
    // USER LOGIN
    // ==========
    fun loginDo() {
        // initialize form
        recordItem = config.invalidItem
        formDefinition = "R;url;\nR;account;\nr;password;\nR;submit;log in"
        form = Form(recordItem, formDefinition) { loginDone() }
        form.init()
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent = {
            Box (modifier = Modifier
                .padding(Theme.dimensions.regularSpacing)
                .wrapContentSize()) {
                form.compose()
            }
        }
        if (ApiHandler.getInstance().connected)
            form.setVisible("url", false)
        Stage.viewModel.setVisibleModal(true)
    }
    private fun loginDone() {
        // hide form
        hideForm()
        form.validate()
        val serverUrl = form.inputFields["url"]?.entered ?: ""
        val userId =
            try { form.inputFields["account"]?.entered?.toInt() ?: -1 }
            catch (e: Exception) { -1 }
        if (userId < 1) {
            form.previousErrors = I18n.getInstance().t("Invalid user Id.")
            loginDo()
        } else {
            val apiHandler = ApiHandler.getInstance()
            if (serverUrl.isNotEmpty()) apiHandler.setUrl(serverUrl)
            apiHandler.setCredentials(userId, form.inputFields["password"]?.entered ?: "")
            apiHandler.start()
        }
    }

    // ================
    // USER PREFERENCES
    // ================
    fun editPreferencesDo() {}
    private fun editPreferencesDone() {}


    // ============
    // ASSET DAMAGE
    // ============
    fun reportDamageDo() {
        setEventContext()
        // initialize form
        recordItem = config.getItem(".tables.damages")
        formDefinition = "R;reported_on,reported_by;\n" +
                "r;asset_uuid,severity;\n" +
                "R;logbook_text;\n" +
                "R;description;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { reportDamageDone() }
        form.init()
        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        val user = User.getInstance()
        val indices = Indices.getInstance()
        if (user.role() != "bths")
            presets["reported_by"] = indices.getNameForUuid(user.uuid())
        presets["reported_on"] = Formatter.microTimeToString(0.0, Language.CSV).substring(0, 16)
        if (eventTableName == "assets") {
            presets["asset_uuid"] = eventRecord.valueToDisplay("name", lang)
        } else if (eventTableName == "logbook") {
            presets["asset_uuid"] = eventRecord.valueToDisplay("asset_uuid", Language.CSV)
            presets["logbook_text"] = "#" + eventRecord.valueToDisplay("number", lang) +
                    ": " + indices.getTableForUid(eventUid) +
                    " (" + eventRecord.valueToDisplay("start", lang) +
                    " ...) " + eventRecord.valueToDisplay("destination", lang)
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent = { FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 1) }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun reportDamageDone() {
        hideForm()
        form.validate()
    }

    // =================
    // ASSET RESERVATION
    // =================
    @OptIn(ExperimentalFoundationApi::class)
    fun requestReservationDo() {
        setEventContext()
        // initialize form
        recordItem = config.getItem(".tables.reservations")
        formDefinition = "R;start,end;\n" +
                "r;reason;\n" +
                "R;asset_uuid,person_uuid;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { requestReservationDone() }
        form.init()
        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        val user = User.getInstance()
        val indices = Indices.getInstance()
        if (user.role() != "bths")
            presets["contact"] = indices.getNameForUuid(user.uuid())
        if ((eventTableName == "assets") || (eventTableName == "logbook")  || (eventTableName == "damages")) {
            presets["asset_uuid"] = eventRecord.valueToDisplay("name", lang)
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent = { FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 2) }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun requestReservationDone() {
        hideForm()
        form.validate()
    }

    // ===============
    // MESSAGE SENDING
    // ===============
    fun sendMessageDo() {}
    private fun sendMessageDone() {}

    // ==========
    // TRIP START
    // ==========
    private fun onBoatChange(boatName: String) {
        // get the boat record
        val db = DataBase.getInstance()
        val boatRecord = db.selectByName("assets", boatName)
        if (boatRecord.isEmpty())
            return
        // get the first variant and its places count
        val variantOptions = Parser.parse(boatRecord["variant_options"] ?: "", ParserName.STRING_LIST, Language.CSV) as List<*>
        val variantOption = if (variantOptions.isEmpty()) "" else (variantOptions.first() as String)
        val variantItem = config.getItem(".catalogs.boat_variants.$variantOption")
        val places =
            if (variantItem.hasChild("places")) (variantItem.getChild("places")?.value() as Int?) ?: 0
            else 0
        // adjust the crew member input fields according to the places count
        if (places > 0)
            for (inputFieldName in form.inputFields.keys) {
                val inputField = form.inputFields[inputFieldName]
                if ((inputField != null) && (inputField.nameDataField == "crew"))
                    if (inputField.listPosition > places) inputField.viewModel.hide()
                    else inputField.viewModel.show()
            }
    }
    private fun onDestinationChange(destinationName: String) {
        // get the destination record
        val db = DataBase.getInstance()
        val destinationRow = db.selectByName("destinations", destinationName)
        if (destinationRow.isEmpty())
            return
        // get the waters names
        var watersNames = ""
        val watersList = Parser.parse(destinationRow["waters_uuids"] ?: "", ParserName.STRING_LIST, Language.CSV) as List<*>
        for (waters in watersList) {
            val watersRow = db.findByUuid(waters as String)
            watersNames += "," + (watersRow["name"] ?: waters)
        }
        if (watersNames.isNotEmpty()) {
            val watersInput = form.inputFields["waters"]
            watersInput?.viewModel?.entered = watersNames.substring(1)
            watersInput?.viewModel?.show()
        }
        // get te distance
        val distance = destinationRow["distance"] ?: "0"
        val distanceInput = form.inputFields["distance"]
        distanceInput?.viewModel?.entered = distance
        distanceInput?.viewModel?.show()
    }
    fun enterTripDo() {
        setEventContext()
        // initialize form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "r;!number,start,end;\n" +
                "r;asset_uuid,session_type,;\n" +
                "r;destination,waters,distance;\n" +
                "R;crew#1,crew#2,crew#3;\n" +
                "r;crew#4,crew#5,crew#6;\n" +
                "r;crew#7,crew#8,crew#9;\n" +
                "r;comments;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { enterTripDone() }
        form.init()

        // prepare boat name listener
        form.inputFields["asset_uuid"]?.addListener(FormFieldListener(onFocusLost =  { onBoatChange(it) }))
        form.inputFields["destination"]?.addListener(FormFieldListener(onFocusLost =  { onDestinationChange(it) }))

        // prepare presets
        val db = DataBase.getInstance()
        val maxNumberTrip = db.select("logbook", null, 1, "-number")
        val maxNumber = try { maxNumberTrip.first()["number"]?.toInt() ?: 0 } catch (e: Exception) { 0 }
        val presets: MutableMap<String, String> = mutableMapOf()

        if (eventTableName == "assets") {
            presets["start"] = Formatter.format(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                ParserName.DATETIME, Language.CSV
            )
            presets["number"] = "" + (maxNumber + 1)
            presets["asset_uuid"] = eventRecord.valueToDisplay("name", lang)
            onBoatChange(presets["asset_uuid"] ?: "")
        } else if (eventTableName == "logbook") {
            // TODO
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent =
            { FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 0) }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun enterTripDone() {
        hideForm()
        form.validate()
    }

    // =========
    // TRIP EDIT
    // =========
    fun editTripDo() {}
    private fun editTripDone() {}

    // ===========
    // TRIP CANCEL
    // ===========
    fun cancelTripDo() {}
    private fun cancelTripDone() {}


}