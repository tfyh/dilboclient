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

package org.dilbo.core

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.parameters
import javafx.application.Platform
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import org.dilbo.storage.ResProvider
import org.dilbo.uiProvider.Menu
import org.tfyh.api.ApiHandler
import org.tfyh.control.EventHandlerIF
import org.tfyh.control.Logger
import org.tfyh.control.LoggerSeverity
import org.tfyh.data.*
import org.tfyh.jfx.design.AppDimensions
import org.tfyh.jfx.ui.Form
import org.tfyh.jfx.ui.FormField
import org.tfyh.jfx.ui.Looper
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.util.I18n
import org.tfyh.util.Language
import org.tfyh.util.User
import kotlin.time.Clock

class FormHandler private constructor(): EventHandlerIF {

    val config = Config.getInstance()
    val i18n = I18n.getInstance()
    val logger = Logger("formHandler")
    private var lang = config.language()
    private var recordItem: Item = config.invalidItem
    private var formDefinition: String = ""
    private lateinit var form : Form
    private lateinit var httpClient: HttpClient
    lateinit var postURL: String
    lateinit var postParameters: Parameters

    private var eventUid: String = ""
    private var eventRecord: Record = Record(config.invalidItem)
    private var eventTableName: String = eventRecord.item.name()

    companion object {
        private val instance = FormHandler()
        fun getInstance() = instance
        fun getModalDimensions() = ViewBuilder.getInstance().getModalMaxDimensions(ViewBuilder.modalSizing)
    }

    fun setHttpClient(httpClient: HttpClient) { this.httpClient = httpClient }

    // ==============================
    // COMMON PRE- AND POSTPROCESSING
    // ==============================
    /**
     * Set the event context by retrieving the last event's uid from the event handler
     */
    private fun setEventContext(event: EventHandlerIF.Event) {
        eventUid = event.uid
        val db = DataBase.getInstance()
        val row = db.findByUid(event.uid)
        if (row.isNotEmpty()) {
            val tableRecord = config.getItem(".tables.${db.tableOfFindBy()}")
            eventRecord = Record(tableRecord)
            eventRecord.parse(row, Language.CSV, false)
            eventTableName = eventRecord.item.name()
        } else {
            eventRecord = Record(config.invalidItem)
            eventTableName = "[undefined]"
        }
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
                    DilboSettings.getInstance().sportsYearStart()
                )
            else null
        val maxNumberRecords = db.select(tableName, where, 1, "-number")
        return try { maxNumberRecords.first()["number"]?.toInt() ?: 0 } catch (_: Exception) { 0 }
    }
    /**
     * Add the close form event and hide the form
     */
    private fun hideForm() {
        Platform.runLater { ViewBuilder.getInstance().hideModal() }
    }

    /**
     * split a field which contains a list into a set of fields, one per allowed entry
     */
    private fun splitCrewField(presets: MutableMap<String, String>) {
        val listFieldName = "crew"
        val list = Parser.parse(presets[listFieldName] ?: "", ParserName.STRING_LIST, Language.CSV) as List<*>
        for (i in list.indices)
            presets["$listFieldName#" + (i + 1)] = list.getOrNull(i) as String? ?: ""
    }

    /**
     * join a set of fields which contain elements of a single list field, into the single list field.
     */
    private fun joinCrewField() {
        val fieldName = "crew"
        val listField = form.inputFields[fieldName]
        if (listField != null) {
            var list = ""
            for (i in 1 .. 24) {
                val inputField = form.inputFields["crew#$i"]
                if ((inputField != null) && (inputField.entered.value.ifEmpty { inputField.preset }).isNotEmpty())
                    list += "," + Codec.encodeCsvEntry(inputField.entered.value.ifEmpty { inputField.preset }, ",")
            }
            if (list.isNotEmpty()) list = list.substring(1)
            listField.entered.value = "[$list]"
            listField.changed = (listField.entered.value != listField.preset)
        }
    }

    /**
     * Compose the left hand context menu for the form.
     */
    private fun menuItemSet(menuOptions: Array<String>, selectedOption: String): Pane {
        val modalDimensions = ViewBuilder.getInstance().getModalMaxDimensions(ViewBuilder.modalSizing)
        val menu = Menu(Config.getInstance().getItem(".access.menus.client"),
            ViewBuilder.Dimensions((modalDimensions.widthPx * 0.1).toInt(),modalDimensions.heightPx),
            ResProvider.getSingleton(), null)
        menu.limitTo(menuOptions)
        menu.setHorizontal(false)
        menu.setUid(eventUid)
        ViewBuilder.getInstance().registerHandler(menu, EventHandlerIF.EventType.MENU_EVENT)
        menu.setSelectedOption(selectedOption)
        return menu.build()
    }

    /**
     * Compose the form including the left hand context menu. The context menu options shall always start with the
     * selected menu option which will be deactivated.
     */
    fun formAndMenu(menuOptions: Array<String>): Pane {
        val formPane = HBox(AppDimensions.REGULAR_SPACING,
            menuItemSet(menuOptions, menuOptions.first()), form.build()).apply {
            prefWidth = form.widthPx * 1.1
        }
        isDisplayedOn = formPane
        return formPane
    }

    // ============
    // TEXT EDITING
    // ============
    fun suggestTextDo(event: EventHandlerIF.Event) {
        // initialise form
        recordItem = config.invalidItem
        formDefinition = "R;~defaultText;\nR;suggestedTranslation;\nr;submit;"
        val doneEvent = event.clone(name = "suggestTextDone")
        form = Form(recordItem, formDefinition, ViewBuilder.Dimensions(500, 500))
            { Looper.getSingleton().append(doneEvent) }
        form.formHeadline = i18n.t("Please suggest a different translation")
        form.init()
        form.presetWithStrings(mapOf(
            "defaultText" to I18n.getInstance().tDefault(event.sourceItem.labelToken() + "|"),
            "suggestedTranslation" to I18n.getInstance().t(event.sourceItem.labelToken() + "|")), true)
        // show form
        ViewBuilder.getInstance().showEditDialogue(form.build().apply {
            prefWidth = form.widthPx.toDouble()
            maxWidth = form.widthPx.toDouble()
            minWidth = form.widthPx.toDouble()
        })
    }

    fun suggestTextDone(event: EventHandlerIF.Event) {
        // first validate the form
        if (form.validate(event.clone(name = "noRedoForTextSuggestion")) {}) {
            hideForm()
            val token = event.sourceItem.labelToken()
            val language = config.language()
            val suggested = form.inputFields["suggestedTranslation"]?.entered?.value ?: ""
            println("Suggestion submitted (language $language) for $token|... is: $suggested")
            postURL = "https://www.dilbo.org/translations/suggest_translation.php"
            postParameters = parameters {
                append("tk", token)
                append("lg", language.toString())
                append("tl", suggested)
            }
            val response = runBlocking {
                try {
                    httpClient.submitForm (url = postURL, formParameters = postParameters)
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        currentCoroutineContext().ensureActive()
                    }
                    if ((e is HttpRequestTimeoutException) ||
                        (e is ConnectTimeoutException) ||
                        (e is SocketTimeoutException)) {
                        val msg = "$postURL: Connection failed, translation suggestion not saved."
                        logger.log(LoggerSeverity.ERROR,"FormHandler.suggestTextDone()", msg)
                        ViewBuilder.getInstance().showDialogue(msg)
                    }
                    null
                }
            }
            if ((response != null) && (response.status.value >= 300)) {
                val msg = "$postURL: Connection error $response, translation suggestion not saved."
                logger.log(LoggerSeverity.ERROR, "FormHandler.suggestTextDone()", msg)
                ViewBuilder.getInstance().showDialogue(msg)
            } else {
                val msg = "Translation suggestion saved."
                logger.log(LoggerSeverity.INFO, "FormHandler.suggestTextDone()", msg)
                ViewBuilder.getInstance().showDialogue(msg)
            }
        }
    }

    // ==========
    // USER LOGIN
    // ==========
    fun loginDo(event: EventHandlerIF.Event, firstTime: Boolean = false, previousErrors: String = "") {
        if (firstTime) {
            // initialise form
            recordItem = config.invalidItem
            formDefinition = "R;*account;\nr;*password;\nR;submit;log in"
            if (!ApiHandler.getInstance().isConnected())
                formDefinition = "R;url;\n$formDefinition"
            val doneEvent = event.clone(name = event.name + "ne")   // for login: loginDo => loginDone
            form = Form(recordItem, formDefinition,
                ViewBuilder.Dimensions(500, 500))
                    { Looper.getSingleton().append(doneEvent) }
            form.formHeadline = i18n.t("pWT7jB|Please log in")
            form.init()
            form.presetWithStrings(mapOf(
                "url" to "http://localhost",
                "account" to "1142"), true)
            ApiHandler.getInstance().setUrl("http://localhost")
        }
        // show form
        if ((event.type == EventHandlerIF.EventType.API_EVENT) &&
            previousErrors.isNotEmpty()) // previous errors were provided by the API, after successful form validation.
            form.previousErrors = previousErrors
        ViewBuilder.getInstance().showModal(form.build().apply {
            prefWidth = form.widthPx.toDouble()
            maxWidth = form.widthPx.toDouble()
            minWidth = form.widthPx.toDouble()
        })
    }

    fun loginDone(event: EventHandlerIF.Event) {
        // first validate the form
        if (form.validate(
                event.clone(name = event.name.replace("Done", "Do")),
                ::loginDo)) {
            hideForm()
            // validation was successful. Hide form
            val apiHandler = ApiHandler.getInstance()
            val serverUrl =
                if (apiHandler.isConnected()) apiHandler.getUrl()
                else form.inputFields["url"]?.entered?.value ?: ""
            val userId =
                try { form.inputFields["account"]?.entered?.value?.toInt() ?: -1 }
                catch (_: Exception) { -1 }
            if (userId < 1) {
                val redoEvent = event.clone(name = "loginDo")
                loginDo(redoEvent, false, I18n.getInstance().t("Please use a numeric user id"))
            } else {
                if (serverUrl.isNotEmpty()) apiHandler.setUrl(serverUrl)
                apiHandler.setCredentials(userId, form.inputFields["password"]?.entered?.value ?: "")
                hideForm()
                // TODO: what was the loginRetryActive flag meant for?
                if (apiHandler.loginRetryActive)
                    apiHandler.loginRetryActive = false
                else
                    apiHandler.start()
            }
        } else {
            // do nothing on invalid entries or no change. At least the password field must have changed.
        }
    }

    // ================
    // USER PREFERENCES
    // ================
    fun editPreferencesDo(event: EventHandlerIF.Event) {
        event.clone(name = event.name + "Done")
        // TODO
    }
    fun editPreferencesDone(event: EventHandlerIF.Event) {
        event.clone(name = event.name.replace("Done", ""))
        // TODO
    }


    // ============
    // ASSET DAMAGE
    // ============
    fun reportDamageDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        // initialise form
        recordItem = config.getItem(".tables.damages")
        formDefinition = "R;~number,;\n" +
                "r;~reported_on,reported_by;\n" +
                "r;asset_uuid,severity;\n" +
                "R;logbook_text;\n" +
                "R;description;\n" +
                "R;submit,;"
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
        form.formHeadline = i18n.t("ln94nL|Please enter the damage ...")
        form.init()
        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        val user = User.getInstance()
        val indices = Indices.getInstance()
        presets["number"] = "" + (getMaxNumber("damages") + 1)
        if (user.role() != "bths")
            presets["reported_by"] = indices.getNameForUuid(user.uuid())
        presets["reported_on"] = NoZoneDateTime.now().dateTimeCleansed
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
        val modalContent =
            when (eventTableName) {
                "logbook" -> formAndMenu(arrayOf("reportDamage", "endTrip", "editTrip", "cancelTrip"))
                "assets" -> formAndMenu(arrayOf("reportDamage", "requestReservation", "enterTrip"))
                else -> formAndMenu(arrayOf("reportDamage"))
            }
        ViewBuilder.getInstance().showModal(modalContent)
    }

    fun reportDamageDone(event: EventHandlerIF.Event) {
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::reportDamageDo)) {
            hideForm()
            form.submit()
        }
    }

    // =================
    // ASSET RESERVATION
    // =================
    fun requestReservationDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        // initialise form
        recordItem = config.getItem(".tables.reservations")
        formDefinition = "R;~number,;\n" +
                "r;start,end;\n" +
                "r;reason;\n" +
                "R;asset_uuid,person_uuid;\n" +
                "R;submit,;"
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
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
        val modalContent =
            if (eventTableName == "assets")
                formAndMenu(arrayOf("requestReservation", "enterTrip", "reportDamage"))
            else formAndMenu(arrayOf("requestReservation"))
        ViewBuilder.getInstance().showModal(modalContent)
    }

    fun requestReservationDone(event: EventHandlerIF.Event) {
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::requestReservationDo)) {
            hideForm()
            form.submit()
        }
    }

    // ===============
    // MESSAGE SENDING
    // ===============
    fun sendMessageDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        // initialise form
        recordItem = config.getItem(".tables.messages")
        formDefinition = "R;~number,~sent_on;\n" +
                "r;*sent_to,;\n" +
                "r;*sent_from,reply_to;\n" +
                "R;*subject;\n" +
                "r;body;\n" +
                "R;submit,;"
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
        form.formHeadline = i18n.t("M8hzhW|Please provide your mess...")
        form.init()

        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        presets["sent_on"] = NoZoneDateTime.now().dateTimeCleansed
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
        val modalContent = formAndMenu(arrayOf("sendMessage"))
        ViewBuilder.getInstance().showModal(modalContent)
    }

    fun sendMessageDone(event: EventHandlerIF.Event) {
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::sendMessageDo)) {
            hideForm()
            form.submit()
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
        val variantItem = config.getItem(".catalogues.boat_variants.$variantOption")
        val places =
            if (variantItem.hasChild("places")) (variantItem.getChild("places")?.value() as Int?) ?: 0
            else 0
        // adjust the crew member input fields according to the places count
        if (places > 0)
            for (inputFieldName in form.inputFields.keys) {
                val inputField = form.inputFields[inputFieldName]
                if ((inputField != null) && (inputField.nameDataField == "crew"))
                    if (inputField.listPosition > places) inputField.setVisibility(false)
                    else inputField.setVisibility(true)
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
            watersInput?.entered?.value = watersNames.substring(1)
        }
        // get the distance
        val distance = destinationRow["distance"] ?: "0"
        val distanceInput = form.inputFields["distance"]
        distanceInput?.entered?.value = distance
    }

    fun enterTripDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        // initialise form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "r;~number,~logbookname,;\n" +
                "R;start,;\n" +
                "r;asset_uuid,session_type,;\n" +
                "r;destination,waters,distance;\n" +
                "R;crew#1,crew#2,crew#3;\n" +
                "r;crew#4,crew#5,crew#6;\n" +
                "r;crew#7,crew#8,crew#9;\n" +
                "R;comments;\n" +
                "R;submit,;"
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
        form.formHeadline = i18n.t("Ozq6QO|Please enter your trip d...")
        form.init()

        // set listeners for boat and destination
        setBoatAndDestinationListeners()

        // prepare presets
        val presets: MutableMap<String, String> = mutableMapOf()
        presets["logbookname"] = config.getItem(".app.user_preferences.logbook").valueCsv()
        presets["number"] = "" + (getMaxNumber("logbook") + 1)
        presets["session_type"] = config.getItem(".catalogues.session_types.NORMAL").label()
        presets["start"] = Formatter.format(
            Clock.System.now(),
            ParserName.DATETIME, Language.CSV
        )
        // There are two ways to trigger the form display: a click on the enter trip menu icon and a click
        // on the available boats list. Only the latter provides a boat as context information
        if (eventTableName == "assets") {
            presets["asset_uuid"] = eventRecord.valueToDisplayByName("name", lang)
            onBoatChange(presets["asset_uuid"] ?: "")
        }
        form.presetWithStrings(presets)
        // show form
        val modalContent =
                if (eventTableName == "assets")
                    formAndMenu(arrayOf("enterTrip", "reportDamage", "requestReservation"))
                else formAndMenu(arrayOf("enterTrip"))
        ViewBuilder.getInstance().showModal(modalContent.apply {
            prefWidth = form.widthPx.toDouble()
        })
    }

    fun enterTripDone(event: EventHandlerIF.Event) {
        joinCrewField()
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::enterTripDo)) {
            hideForm()
            form.submit()
        }
    }

    // =========
    // TRIP END
    // =========
    fun endTripDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        if (eventTableName != "logbook")
            return
        // initialise form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "r;!number,!logbookname,;\n" +
                "r;start,end,;\n" +
                "r;asset_uuid,session_type,;\n" +
                "r;destination,waters,distance;\n" +
                "R;crew#1,crew#2,crew#3;\n" +
                "r;crew#4,crew#5,crew#6;\n" +
                "r;crew#7,crew#8,crew#9;\n" +
                "r;comments;\n" +
                "R;submit,;"
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
        form.formHeadline = i18n.t("JUnXbB|Please check entries and...")
        form.init()

        // set listeners for boat and destination
        setBoatAndDestinationListeners()

        // prepare presets
        val presets: MutableMap<String, String> = eventRecord.formatToDisplay(lang, true).toMutableMap()
        presets["end"] = Formatter.format(
            Clock.System.now(),
            ParserName.DATETIME, Language.CSV
        )
        splitCrewField(presets)
        form.presetWithStrings(presets, true)
        // show form
        onBoatChange(presets["asset_uuid"] ?: "")
        onDestinationChange(presets["destination"] ?: "")
        val modalContent =
            formAndMenu(arrayOf("endTrip", "editTrip", "cancelTrip", "reportDamage"))
        ViewBuilder.getInstance().showModal(modalContent)
    }

    fun endTripDone(event: EventHandlerIF.Event) {
        joinCrewField()
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::editTripDo)) {
            hideForm()
            form.submit(eventUid)
        }
    }

    private fun setBoatAndDestinationListeners() {
        form.inputFields["asset_uuid"]?.addListener(
            FormField.FormFieldListener(onFocusLost = {
                onBoatChange(it)
            })
        )
        form.inputFields["destination"]?.addListener(
            FormField.FormFieldListener(onFocusLost = {
                onDestinationChange(it)
            })
        )
    }

    // =========
    // TRIP EDIT
    // =========
    fun editTripDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        if (eventTableName != "logbook")
            return
        // initialise form
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
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
        form.formHeadline = i18n.t("rT119B|Please edit your trip de...")
        form.init()

        // set listeners for boat and destination
        setBoatAndDestinationListeners()

        // prepare presets
        val presets: MutableMap<String, String> = eventRecord.formatToDisplay(lang, true)
        splitCrewField(presets)
        form.presetWithStrings(presets)
        onBoatChange(presets["asset_uuid"] ?: "")
        onDestinationChange(presets["destination"] ?: "")
        // show form
        val modalContent =
            formAndMenu(arrayOf("editTrip", "endTrip", "cancelTrip", "reportDamage"))
        ViewBuilder.getInstance().showModal(modalContent)
    }

    fun editTripDone(event: EventHandlerIF.Event) {
        joinCrewField()
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::cancelTripDo)) {
            hideForm()
            form.submit(eventUid)
        }
    }

    // ===========
    // TRIP CANCEL
    // ===========
    fun cancelTripDo(event: EventHandlerIF.Event) {
        setEventContext(event)
        if (eventTableName != "logbook")
            return
        // initialise form
        recordItem = config.getItem(".tables.logbook")
        formDefinition = "R;!number,!asset_uuid;\nr;!start,!end;\nR;!destination,!distance;\nR;comments;\nR;submit;"
        val doneEvent = event.clone(name = event.name + "Done")
        form = Form(recordItem, formDefinition, getModalDimensions())
            { Looper.getSingleton().append(doneEvent) }
        val tripDetails = eventRecord.recordToTemplate("full")
        form.formHeadline = i18n.t("CXoF90|Really delete trip:...", tripDetails)
        form.init()
        val presets: MutableMap<String, String> = eventRecord.formatToDisplay(lang, true)
        presets["end"] = Formatter.format(Clock.System.now(), ParserName.DATETIME, Language.CSV )
        form.presetWithStrings(presets)
        // show form
        val modalContent =
            formAndMenu(arrayOf("cancelTrip", "endTrip", "editTrip", "reportDamage"))
        ViewBuilder.getInstance().showModal(modalContent)
    }

    fun cancelTripDone(event: EventHandlerIF.Event) {
        form.inputFields["destination"]?.entered?.value = I18n.getInstance().t("JUPSpF|Trip cancelled")
        if (form.validate(
                event.clone(name = event.name.replace("Done", "")),
                ::cancelTripDo)) {
            hideForm()
            form.submit(eventUid)
        }
    }

    override var isDisplayedOn: Pane? = null
        get() = field // Access the backing field
        set(value) { field = value } // Modify the backing field

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if (event.type == EventHandlerIF.EventType.MENU_EVENT)
            when (event.name) {
                "loginDo" -> {
                    loginDo(event, true)
                    return true
                }
            }
        return false
    }
}
