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
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.dilbo.core.EventHandler
import org.dilbo.core.UIProviderFactory
import org.dilbo.storage.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.jfx.design.AppFonts
import org.tfyh.jfx.ui.Looper
import org.tfyh.jfx.ui.ViewBuilder

/**
 * The main entry class. Just an entry point for the application.
 */
class Main : Application() {

    val eventHandler = EventHandler.getSingleton()
    val uiProviderFactory = UIProviderFactory()
    val looper = Looper.getSingleton()

    companion object {
        const val LOOPER_PERIOD_MILLIS = 20L
        val screenWidth = Screen.getPrimary().bounds.width
        val screenHeight = Screen.getPrimary().bounds.height
        val windowHeight = screenHeight
        val windowWidth = screenWidth
    }

    override fun start(primaryStage: Stage) {
        val viewBuilder = ViewBuilder.getInstance()
        AppFonts.load(ResProvider.getSingleton(), ViewBuilder.fontZoom)
        viewBuilder.init(eventHandler, uiProviderFactory, primaryStage, windowWidth.toInt(), windowHeight.toInt())
        primaryStage.initStyle(StageStyle.UNDECORATED)
        primaryStage.setScene(Scene(viewBuilder.build(), windowWidth, windowHeight))
        primaryStage.show()
        looper.start(LOOPER_PERIOD_MILLIS, eventHandler)
        ApiHandler.getInstance().start()
    }

    override fun stop() {
        looper.stop()
        super.stop()
    }


}