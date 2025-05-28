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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.api.Transaction
import org.dilbo.dilboclient.app.UIProvider.Companion.menuItems
import org.dilbo.dilboclient.composable.ModalCloseButton
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Codec
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.DataBase
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Indices
import org.dilbo.dilboclient.tfyh.data.Item
import org.dilbo.dilboclient.tfyh.data.Parser
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.data.Record
import org.dilbo.dilboclient.tfyh.data.Table
import org.dilbo.dilboclient.tfyh.util.Form
import org.dilbo.dilboclient.tfyh.util.FormFieldListener
import org.dilbo.dilboclient.tfyh.util.I18n
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.User
import org.jetbrains.compose.resources.painterResource

object FormHandler {

    val config = Config.getInstance()
    val i18n = I18n.getInstance()
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
    /**
     * Set the event context by retrieving the last event's uid from the event handler
     */
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
    /**
     * Get the maximum number in a numbered table. Will filter on the sports year start for logbook
     * and workbook.
     */
    private fun getMaxNumber(tableName: String): Int {
        val db = DataBase.getInstance()
        val where =
            if (tableName.contains("book"))
                Table.Selector(if (tableName == "workbook") "date" else  "start",
                    Table.Comparison.GREATER_OR_EQUAL,
                    LocalDateTime(DilboSettings.getInstance().sportsYearStart(),
                        LocalTime(0, 0))
                )
            else null
        val maxNumber = db.select(tableName, where, 1, "-number")
        return try { maxNumber.first()["number"]?.toInt() ?: 0 } catch (e: Exception) { 0 }
    }
    /**
     * Add the close form event and hide the form
     */
    private fun hideForm(formName: String) {
        form.viewModel.setVisible(false)
        UIEventHandler.getInstance().handleButtonEvent("$formName-hidden")
        Stage.modalContent = Stage.emptyModalContent
        Stage.viewModel.setVisibleModal(false)
    }
    private fun splitListField(presets: MutableMap<String, String>, listFieldName: String) {
        val list = Parser.parse(presets[listFieldName] ?: "", ParserName.STRING_LIST, Language.CSV) as List<*>
        for (i in list.indices)
            presets["$listFieldName#" + (i + 1)] = list.getOrNull(i) as String? ?: ""
    }
    private fun joinListField(fieldName: String) {
        val listField = form.inputFields[fieldName]
        if (listField != null) {
            var list = ""
            for (i in 1 .. 24) {
                val inputField = form.inputFields["crew#$i"]
                if ((inputField != null) && (inputField.entered.ifEmpty { inputField.preset }).isNotEmpty())
                    list += "," + Codec.encodeCsvEntry(inputField.entered.ifEmpty { inputField.preset }, ",")
            }
            if (list.isNotEmpty()) list = list.substring(1)
            listField.entered = "[$list]"
            listField.changed = (listField.entered != listField.preset)
        }
    }

    /**
     * Compose the left hand context menu for the form.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun menuItemSet(menuItemsNames: List<String>, selected: Int) {
        val uiEventHandler = UIEventHandler.getInstance()
        val formVisible = form.viewModel.show.collectAsStateWithLifecycle()
        var i = 0
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

    /**
     * Compose the form including the left hand context menu.
     */
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
    fun loginDo(firstTime: Boolean = true) {
        if (firstTime) {
            // initialize form
            recordItem = config.invalidItem
            formDefinition = "R;url;\nR;*account;\nr;*password;\nR;submit;log in"
            form = Form(recordItem, formDefinition) { loginDone() }
            form.formHeadline = i18n.t("pWT7jB|Please log in")
            form.init()
        }
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
        hideForm("login")
        form.validate()
        val apiHandler = ApiHandler.getInstance()
        if (form.formErrors.isNotEmpty()) {
            form.previousErrors = form.formErrors
            form.formErrors = ""
            loginDo(false)
        } else {
            val serverUrl =
                if (apiHandler.connected) apiHandler.getUrl()
                else form.inputFields["url"]?.entered ?: ""
            val userId =
                try { form.inputFields["account"]?.entered?.toInt() ?: -1 }
                catch (e: Exception) { -1 }
            if (userId < 1) {
                form.previousErrors = I18n.getInstance().t("o5zZhu|Invalid user Id.")
                loginDo(false)
            } else {
                if (serverUrl.isNotEmpty()) apiHandler.setUrl(serverUrl)
                apiHandler.setCredentials(userId, form.inputFields["password"]?.entered ?: "")
                if (apiHandler.loginRetryActive)
                    apiHandler.loginRetryActive = false
                else
                    apiHandler.start()
            }
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
        formDefinition = "R;!number,;\n" +
                "r;reported_on,reported_by;\n" +
                "r;asset_uuid,severity;\n" +
                "R;logbook_text;\n" +
                "R;description;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { reportDamageDone() }
        form.formHeadline = i18n.t("ln94nL|Please enter the damage ...")
        form.init()
        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        val user = User.getInstance()
        val indices = Indices.getInstance()
        presets["number"] = "" + (getMaxNumber("damages") + 1)
        if (user.role() != "bths")
            presets["reported_by"] = indices.getNameForUuid(user.uuid())
        presets["reported_on"] = Formatter.microTimeToString(0.0, Language.CSV).substring(0, 16)
        if (eventTableName == "assets") {
            presets["asset_uuid"] = eventRecord.valueToDisplayByName("name", lang)
        } else if (eventTableName == "logbook") {
            presets["asset_uuid"] = eventRecord.valueToDisplayByName("asset_uuid", Language.CSV)
            presets["logbook_text"] = "#" + eventRecord.valueToDisplayByName("number", lang) +
                    ": " + indices.getTableForUid(eventUid) +
                    " (" + eventRecord.valueToDisplayByName("start", lang) +
                    " ...) " + eventRecord.valueToDisplayByName("destination", lang)
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent = {
            when (eventTableName) {
                "logbook" -> FormAndMenu(listOf("endTrip", "editTrip", "cancelTrip", "reportDamage"), 3)
                "assets" -> FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 1)
                else -> FormAndMenu(listOf("reportDamage"), 0)
            }
        }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun reportDamageDone() {
        form.validate()
        val tx = form.submit(false)
        tx.callBack = { reportDamageSaved(tx) }
    }
    private fun reportDamageSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("7AX0Uq|The form could not be sa...", tx.resultMessage))
            reportDamageDo()
        } else {
            hideForm("reportDamage")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            DataBase.getInstance().updateTable("damages", microTimeNow - 3600.0)
        }
    }

    // =================
    // ASSET RESERVATION
    // =================
    fun requestReservationDo() {
        setEventContext()
        // initialize form
        recordItem = config.getItem(".tables.reservations")
        formDefinition = "R;!number,;\n" +
                "r;start,end;\n" +
                "r;reason;\n" +
                "R;asset_uuid,person_uuid;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { requestReservationDone() }
        form.formHeadline = i18n.t("fIFMck|Please enter your reserv...")
        form.init()
        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        presets["number"] = "" + (getMaxNumber("reservations") + 1)
        val user = User.getInstance()
        val indices = Indices.getInstance()
        if (user.role() != "bths")
            presets["contact"] = indices.getNameForUuid(user.uuid())
        if ((eventTableName == "assets") || (eventTableName == "logbook")  || (eventTableName == "damages")) {
            presets["asset_uuid"] = eventRecord.valueToDisplayByName("name", lang)
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent = {
            if (eventTableName == "assets")
                FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 2)
            else
                FormAndMenu(listOf("requestReservation"), 0)
        }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun requestReservationDone() {
        form.validate()
        val tx = form.submit(false)
        tx.callBack = { requestReservationSaved(tx) }
    }
    private fun requestReservationSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("ZfSO28|The form could not be sa...", tx.resultMessage))
            requestReservationDo()
        } else {
            hideForm("requestReservation")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            DataBase.getInstance().updateTable("reservations", microTimeNow - 3600.0)
        }
    }

    // ===============
    // MESSAGE SENDING
    // ===============
    fun sendMessageDo() {
        setEventContext()
        // initialize form
        recordItem = config.getItem(".tables.messages")
        formDefinition = "R;!number,!sent_on;\n" +
                "r;*sent_to,;\n" +
                "r;*sent_from,reply_to;\n" +
                "R;*subject;\n" +
                "r;body;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { sendMessageDone() }
        form.formHeadline = i18n.t("M8hzhW|Please provide your mess...")
        form.init()

        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        presets["sent_on"] = Formatter.format(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            ParserName.DATETIME, Language.CSV
        )
        presets["number"] = "" + (getMaxNumber("messages") + 1)
        val user = User.getInstance()
        val indices = Indices.getInstance()
        if (user.role() != "bths")
            presets["sent_from"] = indices.getNameForUuid(user.uuid())

        presets["subject"] = when (eventTableName) {
            "assets"
                -> eventRecord.valueToDisplayByName("name", lang)
            "damages", "reservations", "logbook"
                -> eventRecord.item.label() + " #" + eventRecord.valueToDisplayByName("number", lang)
            else -> ""
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent = {
            if (eventTableName == "assets")
                FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 2)
            else
                FormAndMenu(listOf("requestReservation"), 0)
        }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun sendMessageDone() {
        form.validate()
        val tx = form.submit(false)
        tx.callBack = { sendMessageSaved(tx) }
    }
    private fun sendMessageSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("8g9K2b|The form could not be sa...", tx.resultMessage))
            sendMessageDo()
        } else {
            hideForm("sendMessage")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            DataBase.getInstance().updateTable("messages", microTimeNow - 3600.0)
        }
    }

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
            watersInput?.entered = watersNames.substring(1)
            watersInput?.viewModel?.entered = watersNames.substring(1)
            watersInput?.viewModel?.show()
        }
        // get te distance
        val distance = destinationRow["distance"] ?: "0"
        val distanceInput = form.inputFields["distance"]
        distanceInput?.entered = distance
        distanceInput?.viewModel?.entered = distance
        distanceInput?.viewModel?.show()
    }
    fun enterTripDo() {
        setEventContext()
        // initialize form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "r;!number,!logbookname,;\n" +
                "r;start,;\n" +
                "r;asset_uuid,session_type,;\n" +
                "r;destination,waters,distance;\n" +
                "R;crew#1,crew#2,crew#3;\n" +
                "r;crew#4,crew#5,crew#6;\n" +
                "r;crew#7,crew#8,crew#9;\n" +
                "r;comments;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { enterTripDone() }
        form.formHeadline = i18n.t("Ozq6QO|Please enter your trip d...")
        form.init()

        // set listeners for boat and destination
        form.inputFields["asset_uuid"]?.addListener(FormFieldListener(onFocusLost =  { onBoatChange(it) }))
        form.inputFields["destination"]?.addListener(FormFieldListener(onFocusLost =  { onDestinationChange(it) }))

        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        presets["logbookname"] = config.getItem(".app.user_preferences.logbook").valueCsv()
        presets["number"] = "" + (getMaxNumber("logbook") + 1)
        presets["start"] = Formatter.format(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            ParserName.DATETIME, Language.CSV
        )
        if (eventTableName == "assets") {
            presets["asset_uuid"] = eventRecord.valueToDisplayByName("name", lang)
            onBoatChange(presets["asset_uuid"] ?: "")
        }
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent =
            {
                if (eventTableName == "assets")
                    FormAndMenu(listOf("enterTrip", "reportDamage", "requestReservation"), 0)
                else
                    FormAndMenu(listOf("enterTrip"), 0)
            }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun enterTripDone() {
        joinListField("crew")
        form.validate()
        val tx = form.submit(false)
        tx.callBack = { enterTripSaved(tx) }
    }
    private fun enterTripSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("ArEHZd|The form could not be sa...", tx.resultMessage))
            enterTripDo()
        } else {
            hideForm("enterTrip")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            DataBase.getInstance().updateTable("logbook", microTimeNow - 3600.0)
        }
    }

    // =========
    // TRIP END
    // =========
    fun endTripDo() {
        setEventContext()
        if (eventTableName != "logbook")
            return
        // initialize form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "r;!number,,;\n" +
                "r;start,end,;\n" +
                "r;asset_uuid,session_type,;\n" +
                "r;destination,waters,distance;\n" +
                "R;crew#1,crew#2,crew#3;\n" +
                "r;crew#4,crew#5,crew#6;\n" +
                "r;crew#7,crew#8,crew#9;\n" +
                "r;comments;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { endTripDone() }
        form.formHeadline = i18n.t("JUnXbB|Please check entries and...")
        form.init()

        // set listeners for boat and destination
        form.inputFields["asset_uuid"]?.addListener(FormFieldListener(onFocusLost =  { onBoatChange(it) }))
        form.inputFields["destination"]?.addListener(FormFieldListener(onFocusLost =  { onDestinationChange(it) }))

        // prepare presets
        val presets: MutableMap<String, String> = eventRecord.formatToDisplay(lang, true).toMutableMap()
        presets["end"] = Formatter.format(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            ParserName.DATETIME, Language.CSV
        )
        splitListField(presets, "crew")
        form.presetWithStrings(presets, true)
        // show form
        form.viewModel.setVisible(true)
        onBoatChange(presets["asset_uuid"] ?: "")
        onDestinationChange(presets["destination"] ?: "")
        Stage.modalContent =
            { FormAndMenu(listOf("endTrip", "editTrip", "cancelTrip", "reportDamage"), 0) }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun endTripDone() {
        joinListField("crew")
        form.validate()
        val tx = form.submit(true, eventUid)
        tx.callBack = { endTripSaved(tx) }
    }
    private fun endTripSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("ByvElj|The form could not be sa...", tx.resultMessage))
            // if failed, do not return to the end trip dialog
        } else {
            hideForm("endTrip")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            // use a time lag of 4 hours, because tim zones may not match between server and client.
            DataBase.getInstance().updateTable("logbook", microTimeNow - 14400.0)
        }
    }

    // =========
    // TRIP EDIT
    // =========
    fun editTripDo() {
        setEventContext()
        if (eventTableName != "logbook")
            return
        // initialize form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "r;!number,,;\n" +
                "r;start,end,;\n" +
                "r;asset_uuid,session_type,;\n" +
                "r;destination,waters,distance;\n" +
                "R;crew#1,crew#2,crew#3;\n" +
                "r;crew#4,crew#5,crew#6;\n" +
                "r;crew#7,crew#8,crew#9;\n" +
                "r;comments;\n" +
                "R;submit,;"
        form = Form(recordItem, formDefinition) { editTripDone() }
        form.formHeadline = i18n.t("rT119B|Please edit your trip de...")
        form.init()

        // set listeners for boat and destination
        form.inputFields["asset_uuid"]?.addListener(FormFieldListener(onFocusLost =  { onBoatChange(it) }))
        form.inputFields["destination"]?.addListener(FormFieldListener(onFocusLost =  { onDestinationChange(it) }))

        // prepare presets
        val presets: MutableMap<String, String> = eventRecord.formatToDisplay(lang, true)
        splitListField(presets, "crew")
        form.presetWithStrings(presets)
        onBoatChange(presets["asset_uuid"] ?: "")
        onDestinationChange(presets["destination"] ?: "")
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent =
            { FormAndMenu(listOf("endTrip", "editTrip", "cancelTrip", "reportDamage"), 1) }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun editTripDone() {
        joinListField("crew")
        form.validate()
        val tx = form.submit(true, eventUid)
        tx.callBack = { editTripSaved(tx) }
    }
    private fun editTripSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("IuPun7|The form could not be sa...", tx.resultMessage))
            // if failed, do not return to the end trip dialog
        } else {
            hideForm("editTrip")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            DataBase.getInstance().updateTable("logbook", microTimeNow - 3600.0)
        }
    }

    // ===========
    // TRIP CANCEL
    // ===========
    fun cancelTripDo() {
        setEventContext()
        if (eventTableName != "logbook")
            return
        // initialize form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "R;!destination,!distance;\nR;comments;\nR;submit;"
        form = Form(recordItem, formDefinition) { cancelTripDone() }
        val tripDetails = eventRecord.recordToTemplate("full")
        form.formHeadline = i18n.t("CXoF90|Really delete trip:...", tripDetails)
        form.init()
        val presets: MutableMap<String, String> = eventRecord.formatToDisplay(lang, true)
        presets["destination"] = I18n.getInstance().t("JUPSpF|Trip cancelled")
        presets["distance"] = "0"
        form.presetWithStrings(presets)
        // show form
        form.viewModel.setVisible(true)
        Stage.modalContent =
            { FormAndMenu(listOf("endTrip", "editTrip", "cancelTrip", "reportDamage"), 2) }
        Stage.viewModel.setVisibleModal(true)
    }
    private fun cancelTripDone() {
        form.validate()
        val tx = form.submit(true, eventUid)
        tx.callBack = { cancelTripSaved(tx) }
    }
    private fun cancelTripSaved(tx: Transaction) {
        if (tx.resultCode >= 40) {
            Stage.showDialog(I18n.getInstance().t("l1h4Qy|The form could not be sa...", tx.resultMessage))
            // if failed, do not return to the end trip dialog
        } else {
            hideForm("cancelTrip")
            val microTimeNow = Clock.System.now().toEpochMilliseconds() / 1000.0
            DataBase.getInstance().updateTable("logbook", microTimeNow - 3600.0)
        }
    }


}
