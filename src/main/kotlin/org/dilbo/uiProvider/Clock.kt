package org.dilbo.uiProvider

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import org.dilbo.core.EventHandler
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

    override fun build(): Region {
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
        EventHandler.getSingleton().registerHandler(this, EventHandlerIF.EventType.SYSTEM_EVENT)
        return stackPane
    }

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