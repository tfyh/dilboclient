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

package org.dilbo.uiProvider

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Item
import org.tfyh.data.NoZoneDateTime
import org.tfyh.jfx.design.AppFonts
import org.tfyh.jfx.ui.Looper
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.util.Language
import org.tfyh.util.Timer

/**
 * Provide a date and time display. Will be updated all 0.5 seconds.
 */
class Clock(val item: Item, val dimensions: ViewBuilder.Dimensions): UIProviderIF, EventHandlerIF {

    val model = Model()

    override fun build(): Pane {
        model.timer.start(500L)
        val label = Label()
        label.font = AppFonts.h3
        label.textProperty().bindBidirectional(model.timeProperty)
        // use a stack-pane to centre the element
        val stackPane = StackPane(label).apply {
            alignment = Pos.CENTER
            prefWidth = dimensions.widthPx.toDouble()
            prefHeight = dimensions.heightPx.toDouble()
            onMouseClicked = { e ->
                if (!e.isControlDown) // controlDown click is used for debugging.
                    Looper.instance.append(EventHandlerIF.Event(EventHandlerIF.EventType.UI_EVENT,
                        "dilboAbout", sourceItem = item))
            }
        }
        ViewBuilder.getInstance().registerHandler(this, EventHandlerIF.EventType.SYSTEM_EVENT)
        isDisplayedOn = stackPane
        return stackPane
    }

    override var isDisplayedOn: Pane? = null
        get() = field // Access the backing field
        set(value) { field = value } // Modify the backing field

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if ((event.type == EventHandlerIF.EventType.SYSTEM_EVENT) &&
            (event.name == "clock")) {
            model.timeProperty.value = event.message
            return true
        }
        return false
    }

    companion object {
        /**
         * A view model triggering update twice per second for time display
         */
        class Model {
            val timeProperty = SimpleStringProperty("00:00:00")
            val looper = Looper.getSingleton()
            // "suspend" is needed for timer instantiation
            suspend fun update() {
                looper.append(EventHandlerIF.Event(EventHandlerIF.EventType.SYSTEM_EVENT, "clock",
                    message = NoZoneDateTime.now().toDateTimeString(Language.CSV).substring(11, 19)))
            }
            val timer = Timer(::update)
        }
    }

}