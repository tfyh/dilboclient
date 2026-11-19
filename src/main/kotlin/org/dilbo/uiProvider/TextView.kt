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

import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import org.dilbo.core.UIProviderFactory
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Item
import org.tfyh.jfx.ui.StyledText
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder

class TextView(val item: Item): UIProviderIF, EventHandlerIF {

    val pane: StackPane = StackPane()

    init {
        ViewBuilder.getInstance().registerHandler(this, EventHandlerIF.EventType.API_EVENT)
    }

    fun getText() =
        UIProviderFactory.fill((item.getChild("template")?.valueCsv() ?: "?template?"), mutableMapOf())

    fun update() {
        pane.children.clear()
        pane.children.add(StyledText.toAnnotatedString(getText()))
    }
    override fun build(): Pane {
        update()
        isDisplayedOn = pane
        return pane
    }

    override var isDisplayedOn: Pane? = null
        get() = field // Access the backing field
        set(value) { field = value } // Modify the backing field

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if ((event.type === EventHandlerIF.EventType.API_EVENT) && (event.name == "sessionStarted")) {
            update()
            return true
        }
        return false
    }

}