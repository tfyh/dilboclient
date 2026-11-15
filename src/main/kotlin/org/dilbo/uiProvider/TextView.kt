package org.dilbo.uiProvider

import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import org.dilbo.core.EventHandler
import org.dilbo.core.UIProviderFactory
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Item
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.jfx.ui.appText

class TextView(val item: Item): UIProviderIF, EventHandlerIF {

    val pane: StackPane = StackPane()

    init {
        ViewBuilder.getInstance().registerHandler(this, EventHandlerIF.EventType.API_EVENT)
    }

    fun getText() =
        UIProviderFactory.fill((item.getChild("template")?.valueCsv() ?: "?template?"), mutableMapOf())

    fun update() {
        pane.children.clear()
        pane.children.add(appText(getText()))
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