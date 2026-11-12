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

package org.dilbo.client

import io.ktor.client.*
import javafx.application.Application
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.dilbo.core.EventHandler
import org.dilbo.core.FormHandler
import org.dilbo.core.ListConditions
import org.dilbo.core.UIProviderFactory
import org.dilbo.storage.LocalCache
import org.dilbo.storage.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.control.Logger
import org.tfyh.control.LoggerSeverity
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import org.tfyh.data.Formatter
import org.tfyh.jfx.design.AppColours

val logger = Logger("app")
var urlCheck = -1
private val httpClient = HttpClient()

/**
 * The main entry function to launch the application.
 */
suspend fun main() {
    // load the configuration. Because this involves resource reading, it must be executed asynchronously.
    withContext(Dispatchers.Default) {
        val config = Config.getInstance()
        val localCache = LocalCache.getInstance()

        // FOR TESTING PURPOSES ONLY
        localCache.clear()
        // FOR TESTING PURPOSES ONLY

        config.setLocalCache(localCache)
        config.localFilePath = localCache.init()

        try {
            // load the configuration
            if (!config.ready())
                config.load(ResProvider.getSingleton())
            logger.log(LoggerSeverity.INFO, "App()", "Configuration loaded")
            Formatter.useSeconds = false

            // initialise the resource provider and UI provider factory
            ResProvider.getSingleton().apply { init() }
            AppColours.load() // from configuration
            logger.log(LoggerSeverity.INFO, "App()", "Resources loaded")
            UIProviderFactory.setHttpClient(httpClient)

            // instantiate the database singleton
            DataBase.getInstance().apply {
                setLocalCache(localCache)  // the storage location
                setListConditionsIF(ListConditions)  // the object to get list parameters from
                setEventListener(EventHandler.getSingleton())  // to handle data change events
                load(true)  // read the locally available data
            }
            logger.log(LoggerSeverity.INFO, "App()", "Database loaded")

            // initialise event management
            EventHandler.getSingleton()

            // instantiate the API handler singleton and add the event handler
            val apiHandler = ApiHandler.getInstance().apply {
                setLocalCache(localCache)
            }
            logger.log(LoggerSeverity.INFO, "App()", "API initialised")

            // instantiate the form handler singleton and add the event handler
            FormHandler.getInstance()

            // try to connect on stored session credentials
            if (!apiHandler.isConnected()) {
                apiHandler.setHttpClient(httpClient)
                urlCheck = apiHandler.connect()
            } else
                urlCheck = apiHandler.getConnectionStatus()

            Application.launch(Main::class.java)

        } catch (e: Exception) {
            if (e is CancellationException) {
                ensureActive()
            }
            e.printStackTrace()
        }
    }
}