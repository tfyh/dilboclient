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

import javafx.application.Application
import kotlinx.coroutines.*
import org.dilbo.app.FormHandler
import org.dilbo.app.LocalCache
import org.dilbo.app.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.control.Logger
import org.tfyh.control.LoggerSeverity
import org.tfyh.data.Config
import org.tfyh.data.DataBase
import io.ktor.client.HttpClient
import org.dilbo.app.UIProviderFactory

val logger = Logger("app")
var urlCheck = -1
private val httpClient = HttpClient()

/**
 * The main entry function to launch the application.
 */
suspend fun main() {
    // load the configuration. Because this involves resource reading, it must be executed asynchronously.
    ResProvider.getSingle().init()
    withContext(Dispatchers.Default) {
        val config = Config.getInstance()
        val localCache = LocalCache.getInstance()
        config.setLocalCache(localCache)
        config.localFilePath = localCache.init()
        try {
            // load the configuration
            if (!config.ready())
                config.load(ResProvider.getSingle())
            logger.log(LoggerSeverity.INFO, "App()", "getting Config")

            // initialise the resource provider and UI provider factory
            ResProvider.getSingle().init()
            UIProviderFactory.setHttpClient(httpClient)

            // instantiate the database singleton
            val database = DataBase.getInstance()
            database.setLocalCache(localCache)
            database.load(true)

            // instantiate the API handler singleton and add the event handler
            val apiHandler = ApiHandler.getInstance()
            apiHandler.setEventHandler(EventHandler.getSingleton())
            apiHandler.setLocalCache(localCache)

            // instantiate the form handler singleton and add the event handler
            val formHandler = FormHandler.getInstance()

            // try to connect on stored session credentials
            if (!apiHandler.connected) {
                apiHandler.setHttpClient(httpClient)
                urlCheck = ApiHandler.getInstance().connect()
            } else
                urlCheck = apiHandler.connectionStatus
            // if failed: revert to manual login
            if (!apiHandler.connected)
                formHandler.loginDo()

            Application.launch(Main::class.java)

        } catch (e: Exception) {
            println(e.message)
        }
    }
}