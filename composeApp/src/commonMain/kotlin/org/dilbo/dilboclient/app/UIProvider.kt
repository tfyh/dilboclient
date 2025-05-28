package org.dilbo.dilboclient.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dilboclient.composeapp.generated.resources.Res
import dilboclient.composeapp.generated.resources.connection_alert
import dilboclient.composeapp.generated.resources.menu_cancelTrip
import dilboclient.composeapp.generated.resources.menu_editPreferences
import dilboclient.composeapp.generated.resources.menu_editTrip
import dilboclient.composeapp.generated.resources.menu_endTrip
import dilboclient.composeapp.generated.resources.menu_enterTrip
import dilboclient.composeapp.generated.resources.menu_reportDamage
import dilboclient.composeapp.generated.resources.menu_requestReservation
import dilboclient.composeapp.generated.resources.menu_sendMessage
import dilboclient.composeapp.generated.resources.none
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.api.Transaction
import org.dilbo.dilboclient.composable.DilboLabel
import org.dilbo.dilboclient.composable.ListItem
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.composable.StyledText
import org.dilbo.dilboclient.composable.SubmitButton
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Codec
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.DataBase
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.Indices
import org.dilbo.dilboclient.tfyh.data.Item
import org.dilbo.dilboclient.tfyh.data.ParserConstraints
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.data.Record
import org.dilbo.dilboclient.tfyh.data.SettingsLoader
import org.dilbo.dilboclient.tfyh.data.Table
import org.dilbo.dilboclient.tfyh.util.I18n
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.LocalCache
import org.dilbo.dilboclient.tfyh.util.Timer
import org.dilbo.dilboclient.tfyh.util.User
import org.jetbrains.compose.resources.painterResource

class UIProvider(val item: Item, private val layoutItem: Item): Table.Listener {

    inner class UIProviderViewModel: ViewModel() {
        fun show() {
            _version.update { _version.value + 1 }
        }
        fun hide() {
            _version.update { 0 }
        }
        private val _version : MutableStateFlow<Int> = MutableStateFlow(0)
        val version : StateFlow<Int> = _version
    }
    val viewModel = UIProviderViewModel()

    // a cache for statistics tables retrieved from the server.
    private val statistics: MutableMap<String, MutableMap<String, List<MutableMap<String, String>>>> = mutableMapOf()
    private val listElements = mutableListOf<ListItem>()

    private val type: String = item.getChild("type")?.valueCsv() ?: ""
    private val optionsMap: MutableMap<String, String> = mutableMapOf()
    private val permissionsString: String

    companion object {

        // STATIC FUNCTIONS FOR TEXT FORMATTING
        // ====================================
        /**
         * Fill a template string with available values. Use the uid to get the rows definition.
         */
        private fun fill(template: String, row: Map<String, String>): String {

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
         * Display a dialog with a notice
         */
        fun displayDialog(headline: String, text: String) {
            Stage.dialogContent = {
                Column(modifier = Modifier.fillMaxWidth().padding(Theme.dimensions.largeSpacing)) {
                    DilboLabel(headline, true)
                    Spacer(modifier = Modifier.height(Theme.dimensions.regularSpacing))
                    DilboLabel(text)
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
            val uid = UIEventHandler.getInstance().getLastUid()
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
                val modalWidth = Stage.getModalMaxWidth()
                Stage.modalContent = {
                    Box(Modifier.padding(start = Theme.dimensions.regularSpacing, top =
                        Theme.dimensions.regularSpacing, bottom = Theme.dimensions.regularSpacing,
                        end = Theme.dimensions.smallSpacing)) {
                        Column (modifier = Modifier.width(modalWidth)
                            .padding(start = Theme.dimensions.largeSpacing, end = Theme.dimensions.largeSpacing)) {
                            Spacer(Modifier.height(Theme.dimensions.regularSpacing))
                            DilboLabel(text = recordItem.label() + ": " + record.recordToTemplate("name"),
                                large = true, color = Theme.colors.color_text_h1_h3, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(Theme.dimensions.regularSpacing))
                            Column (
                                modifier = Modifier.wrapContentSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                val i = 0
                                val evenRowsModifier = Modifier.background(Theme.colors.color_background_body)
                                for (field in recordItem.getChildren()) {
                                    if (!ParserConstraints.isEmpty(record.value(field.name()),
                                            field.type().parser()))
                                        Row {
                                            val rowModifier = if ((i % 2) == 1) evenRowsModifier else Modifier
                                            DilboLabel(text = field.label() + ": ",
                                                modifier = rowModifier.width(modalWidth * 0.4F))
                                            val toShow =
                                                if ((field.name() != "uuid") && (field.name() != "uid"))
                                                    record.valueToDisplay(field, config.language())
                                                else
                                                    record.formatValue(field, config.language())
                                            DilboLabel(text = toShow,
                                                modifier = Modifier.width(modalWidth * 0.55F))

                                        }
                                }
                                Spacer(Modifier.height(Theme.dimensions.smallSpacing))
                                SubmitButton("Ok") {
                                    Stage.viewModel.setVisibleModal(false)
                                    UIEventHandler.getInstance().handleButtonEvent("displayDetailsDone")
                                }
                                Spacer(Modifier.height(Theme.dimensions.regularSpacing))
                            }
                        }
                    }
                }
                Stage.viewModel.setVisibleModal(true)
            }
        }

        // IMAGE DISPLAY
        // =============
        @Composable
        fun displayRemoteImage(url: String) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        }

        // MENU ITEMS
        // ==========
        data class MenuItem (
            val menuItemConfig: Item
        ) {
            val permissions = menuItemConfig.nodeReadPermissions()
            val icon = when (menuItemConfig.name()) {
                "enterTrip" -> Res.drawable.menu_enterTrip
                "endTrip" -> Res.drawable.menu_endTrip
                "editTrip" -> Res.drawable.menu_editTrip
                "cancelTrip" -> Res.drawable.menu_cancelTrip
                "reportDamage" -> Res.drawable.menu_reportDamage
                "requestReservation" -> Res.drawable.menu_requestReservation
                "sendMessage" -> Res.drawable.menu_sendMessage
                "editPreferences" -> Res.drawable.menu_editPreferences
                else -> Res.drawable.none
            }
            fun isAllowedMenuItem(): Boolean = User.getInstance().isAllowedItem(permissions)
            fun isHiddenMenuItem(): Boolean = User.getInstance().isHiddenItem(permissions)
        }

        val menuItems: MutableMap<String, MenuItem> = mutableMapOf()

    }

    init {
        val db = DataBase.getInstance()
        if (item.hasChild("table"))
            db.addListener(tableName = item.getChild("table")?.valueStr() ?: "no_name", this)
        permissionsString = item.getChild("permissions")?.valueStr() ?: "admin"
        val options = item.getChild("options")?.valueStr()?.split(";") ?: mutableListOf()
        for (option in options)
            if (option.isNotEmpty())
                optionsMap[option.split("=")[0]] = option.split("=")[1]
        if (optionsMap["asset_status"] != null)
            // if a list oses the asset information make sure it receives updates for all related tables
            AssetInfos.addListenerToTables(this)
    }

    private fun isDisplayAllowed() = User.getInstance().isAllowedItem(permissionsString)

    // BUILD SECTION
    // =============

    // MENU
    // ====

    private fun loadMenu() {
        val config = Config.getInstance()
        val menu = config.getItem(".access.menus.client")
        for (menuItem in menu.getChildren())
            menuItems[menuItem.name()] = MenuItem(menuItem)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun showMenu(horizontal: Boolean) {
        val uiEventHandler = UIEventHandler.getInstance()
        for (menuItemName in menuItems.keys) {
            val menuItem = menuItems[menuItemName]
            if ((menuItem != null) && menuItem.isAllowedMenuItem() && !menuItem.isHiddenMenuItem()) {
                val boxModifier = if (horizontal) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()
                BoxWithConstraints (modifier = boxModifier.padding(Theme.dimensions.regularSpacing)) {
                    val imageModifier = if (horizontal) Modifier.height(maxHeight) else Modifier.width(maxWidth)
                    Image(
                        painterResource(menuItem.icon),
                        contentDescription = "icon",
                        modifier = imageModifier
                            .combinedClickable(
                                onClick = {
                                    uiEventHandler.handleUiEvent(
                                        UIEventHandler.UIEvent("",
                                            menuItem.menuItemConfig.name(),
                                            menuItem.menuItemConfig))
                                }
                            )
                    )
                }
            }
        }
    }

    // LIST
    /**
     * Build a list according to the provider settings
     */
    private fun buildList() {

        val tableName = item.getChild("table")?.valueStr() ?: return
        val template = item.getChild("template")?.valueStr() ?: return

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
                        filtered.sortedWith(compareBy({ it["places"]?.toInt() ?: 0 },
                            { it["name"] ?: "no-name" }))
                    }
                    "logbook" -> Table.sortRows("start.number", filtered, recordItem)
                    else -> Table.sortRows("name.number", filtered, recordItem)
                }
            } else
                filtered

        // trigger removal of existing list elements
        for (element in listElements)
            element.visible = false
        listElements.clear()

        // build new list
        val groupBy = optionsMap["groupBy"] ?: ""
        var lastGroup = ""
        for (element in sorted) {
            val elementUuid = if (tableName == "assets") element["uuid"] else element["asset_uuid"]
            val elementUid = element["uid"] ?: "?uid?"
            val group = element[groupBy] ?: ""
            if (group != lastGroup) {
                listElements.add(
                    ListItem("", Res.drawable.none, group, this, evenRow = true)
                )
                lastGroup = group
            }
            val icon = AssetInfos.getInstance()[elementUuid].iconResource
            listElements.add(
                ListItem(elementUid, icon, fill(template, element), this)
            )
        }
    }

    // STATISTICS
    /**
     * get the statistics from the server for a uiProvider
     */
    internal fun getRankingFromServer(ranking: String) {
        ApiHandler.getInstance().addNewTxToPending(Transaction.TxType.LIST, "statistics",
            mutableMapOf("providerName" to ranking))
    }

    /**
     * read the returned statistics result into the statistics cache
     */
    fun buildStatistics(providerName: String, resultMessage: String) {
        val type = optionsMap["type"] ?: "?type?"
        val field = optionsMap["field"] ?: "?field?"
        this.statistics[type]?.set(field, Codec.csvToMap(resultMessage).toList())
    }

    /**
     * A view model triggering update twice per second and providing both the time and the Api status.
     */
    inner class ClockViewModel: ViewModel() {
        private val _time = MutableStateFlow("") // private mutable state flow
        val time = _time.asStateFlow() // publicly exposed as read-only state flow
        private val _apiState = MutableStateFlow(Res.drawable.connection_alert) // private mutable state flow
        val apiState = _apiState.asStateFlow() // publicly exposed as read-only state flow
        // "suspend" is needed for timer instantiation, not recognized by IDE
        suspend fun update() {
            _time.update {
                Formatter.format(
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    ParserName.DATETIME, Config.getInstance().language()
                ).substring(11, 16)
            } // atomic, safe for concurrent use
            _apiState.update { ApiHandler.getInstance().getApiStatus() }
        }
        val timer = Timer(::update)
    }

    @Composable
    fun showClock() {
        val viewModel = ClockViewModel()
        viewModel.timer.start(500L)
        val time = viewModel.time.collectAsStateWithLifecycle()
        Box (contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            DilboLabel(time.value, textAlign = TextAlign.Center)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun showApiStatus() {
        val viewModel = ClockViewModel()
        viewModel.timer.start(500L)
        val apiStatus = viewModel.apiState.collectAsStateWithLifecycle()
        val horizontal = layoutItem.getChild("horizontal")?.value() as Boolean? ?: false
        val boxModifier = if (horizontal) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()
        BoxWithConstraints (modifier = boxModifier.padding(Theme.dimensions.regularSpacing)) {
            val imageModifier = if (horizontal) Modifier.height(maxHeight) else Modifier.width(maxWidth)
            Image(
                painterResource(apiStatus.value),
                contentDescription = "icon",
                modifier = imageModifier
                    .combinedClickable(
                        onClick = {
                            // refresh application
                            LocalCache.getInstance().clear()
                            SettingsLoader().requestModified()
                            DataBase.getInstance().load()
                        },
                        // do the same for a double click (desktop) and a long click (smartphone)
                        onLongClick = { displayDialog("dilbo", Config.getInstance().dilboAbout()) },
                        onDoubleClick = { displayDialog("dilbo", Config.getInstance().dilboAbout()) },
                    )

            )
        }
    }

    // DISPLAY SECTION
    // ===============
    /**
     * Compose the UI
     */
    @Composable
    fun compose() {
        val version = viewModel.version.collectAsStateWithLifecycle()
        if (version.value > 0)
            if (!isDisplayAllowed()) {
                Box (contentAlignment = Alignment.Center) {
                    val message = if (User.getInstance().userId() > 0)
                        I18n.getInstance().t("aJAUJg|Sorry, no access to °%1°", item.label())
                    else ""
                    DilboLabel(message, textAlign = TextAlign.Center)
                }
            }
            else when (type) {
                "menu" -> {
                    val horizontal = layoutItem.getChild("horizontal")?.value() as Boolean? ?: false
                    loadMenu()
                    if (horizontal)
                        Row { showMenu(horizontal) }
                    else
                        Column { showMenu(horizontal) }
                }
                "list" -> {
                    buildList()
                    Column {
                        Box (modifier = Modifier.fillMaxWidth()
                                .background(Theme.colors.color_background_body)
                                .padding(start = Theme.dimensions.smallSpacing * 2))
                             {
                            DilboLabel(item.label(), true,
                                color = Theme.colors.color_text_h1_h3,
                                textAlign = TextAlign.Center)
                        }
                        Column (
                            modifier = Modifier.verticalScroll(rememberScrollState())
                                .padding(start = Theme.dimensions.smallSpacing,
                                    end = Theme.dimensions.smallSpacing)
                        ) {
                            for (element in listElements)
                                element.compose()
                        }
                    }
                }
                "statistics" -> {
                    // TODO
                }
                "clock" -> { showClock() }
                "apiStatus" -> { showApiStatus() }
                "ranking" -> {
                    // TODO
                }
                "textView" -> {
                    val text = fill((item.getChild("template")?.valueCsv() ?: "?template?"), mutableMapOf())
                    Box (modifier = Modifier.padding(Theme.dimensions.regularSpacing)) {
                        Text(
                            StyledText.toAnnotatedString(text),
                            style = Theme.fonts.p
                        )
                    }
                }
                "imageView" -> {
                    // use the actual URL, not the configured one. May be a manually entered URL on login
                    val serverUrl = ApiHandler.getInstance().getUrl()
                    val resourceUrl = item.getChild("resources")?.valueCsv() ?: ""
                    if (resourceUrl.isNotEmpty()) {
                        val url =
                            if (resourceUrl.startsWith("../")) serverUrl + resourceUrl.substring(2)
                            else resourceUrl
                        displayRemoteImage(url)
                    }
                }
            }
    }

    override fun changed(table: Table, uid: String) {
        // whatever happened, redo the entire UI
        viewModel.show()
    }

    override fun toString(): String {
        return item.name()
    }


}
