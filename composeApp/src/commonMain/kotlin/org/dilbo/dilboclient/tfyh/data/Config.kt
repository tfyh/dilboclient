/**
 * tools-for-your-hobby
 * https://www.tfyh.org
 * Copyright  2023-2025  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.dilbo.dilboclient.tfyh.data

import dilboclient.composeapp.generated.resources.Res
import kotlinx.datetime.TimeZone
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.delay
import org.dilbo.dilboclient.tfyh.control.Logger
import org.dilbo.dilboclient.tfyh.control.LoggerSeverity
import org.dilbo.dilboclient.tfyh.util.I18n
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.LocalCache
import org.dilbo.dilboclient.tfyh.util.User
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.time.Duration

/**
 * A utility class to load all application configuration.
 */
class Config private constructor() {

    companion object {

        // the path in the local cache must be identical to the settings files' path at the server side
        private const val CACHE_PATH = "Config"
        val allSettingsFiles = arrayOf(  // the sequence is relevant for the loading process, do not change
            // settings descriptor and data types
            "descriptor", // the file to define the properties which describe a value
            "types", // the file to define the available value types
            // immutable settings. These will never change in structure nor get actual values
            "access", // the settings which configure roles, menus, workflows, concessions and subscriptions
            "templates", // configuration branches which are available for multiple use in this app
            "framework", // the setting which configure the framework classes for this app
            "tables", // the database table layout
            // tenant mutable structure. These may get added or deleted items and actual values
            "lists", // the settings which configure list retrievals off the database
            "app", // all settings of the application needed to run at the tenant.
            "catalogs", // the catalogs of types, like the valid boat variants asf.
            "ui" // user interface layout and other settings
        )
        private val rootItemDefinition = mapOf("_path" to "#none", "_name" to "root", "default_label" to "root", "value_type" to "none" )
        private val invalidItemDefinition = mapOf("_path" to "#none", "default_label" to "invalid item", "value_type" to "none" )
        private val instance = Config()

        fun getInstance() = instance
        // no Kotlin implementation
        fun getModified(): String = ""
    }

    // temporary initialize the root and invalid item to ensure they are never null.
    // Will be replaced during load.
    val rootItem: Item = Item.getFloating(rootItemDefinition)
    val invalidItem: Item = Item.getFloating(invalidItemDefinition)

    private val branchesCsv: MutableMap<String, String> = mutableMapOf()
    private val loaded: MutableMap<String, Boolean> = mutableMapOf()
    var ready = false

    private var language: Language = Language.EN
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
    internal val logger = Logger("../Log/config.log")

    private var appVersion: String = "0.0"
    private var appName: String = ""
    private var appUrl: String = ""

    /**
     * Get an Item by its path. Returns the invalid Handle on errors
     */
    fun getItem(path: String): Item {
        if (path.isEmpty())
            return rootItem
        if (!path.startsWith("."))
            return invalidItem
        val names = path.substring(1).split(".")
        var i = 0
        var parent: Item? = getInstance().rootItem
        while ((i < names.size) && (parent != null) && parent.hasChild(names[i]))
            parent = parent.getChild(names[i++])
        return if (parent == null) invalidItem
        else if (i == names.size) parent // path fully resolved
        else if (parent == parent.parent()) {
            // hit top level
            if (this.loaded[names[0]] != true) {
                this.loadBranch(names[0])
                this.getItem(path)
            } else
            return parent
        }
        else invalidItem // path not resolved
    }

    fun language() = language
    fun timeZone() = timeZone

    /**
     * load a top level branch. This is not pat of the main loading procedure, but performed on demand based on
     * the getItem() requests.
     */
    private fun loadBranch(branchName: String) {
        if (this.loaded[branchName] == true)
            return
        val settingsMap = Codec.csvToMap(this.branchesCsv[branchName])
        val loadingResult = this.rootItem.readBranch(settingsMap)
        if (loadingResult.isNotEmpty())
            this.logger.log(LoggerSeverity.ERROR, "Config->load", loadingResult)
        else
            this.loaded[branchName] = true
    }
    /**
     * Load the configuration. Descriptor and types are read from resource files
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        // read the settings csv-Files into memory. Start with local cache and use the default
        // settings as fallback
        val lc = LocalCache.getInstance()
        for (settingsFile in allSettingsFiles) {

            this.branchesCsv[settingsFile] =
                if (SettingsLoader.invalidConfigurationFile(
                        "$CACHE_PATH/$settingsFile", lc.getItem("$CACHE_PATH/$settingsFile")))
                    try {
                        Res.readBytes("files/$CACHE_PATH/$settingsFile").decodeToString()
                    } catch (e: Exception) {
                        println("Missing settings file: $settingsFile")
                            .toString().substring(0, 0) // provide a String return type
                    }
                else
                    lc.getItem("$CACHE_PATH/$settingsFile")

            // the first two files read are descriptor and settings. Once read, initialize the
            // types in order to be able to parse the configuration
            if (settingsFile == "types")
                Type.init(this.branchesCsv["descriptor"] ?: "",
                    this.branchesCsv["types"] ?: "")

        }

        // temporary for Kotlin. No session management yet, therefore only User management
        User.setIncludedRoles()

        // set tables and language
        Record.copyCommonFields()

        // specific for the client implementation: read ui layouts
        getItem(".templates")
        getItem(".app")
        getItem(".ui")

        val languageString = getItem(".app.user_preferences.language").valueCsv()
        language = Language.valueOfOrDefault(languageString.uppercase())
        this.appName = getItem(".framework.app.name").valueCsv()
        this.appUrl = getItem(".framework.app.url").valueCsv()
        this.appVersion = getItem(".framework.app.version").valueStr()

        // initialize the locale settings for parser and formatter
        I18n.getInstance().loadResource(language)
        logger.setLocale(language, timeZone)
        Formatter.setLocale(instance.language(), instance.timeZone())
        Parser.setLocale(instance.language(), instance.timeZone())

        // loading completed, trigger recomposition
        ready = true
        Stage.viewModel.setVisibleMain(true)
    }
}