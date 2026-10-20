package org.dilbo.uiProvider

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Label
import javafx.scene.layout.Region
import kotlinx.coroutines.flow.update
import org.tfyh.api.ApiHandler
import org.tfyh.api.ApiStatusEnum
import org.tfyh.data.Item
import org.tfyh.data.NoZoneDateTime
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.util.Language
import org.tfyh.util.Timer

/**
 * Provide a date and time display. Will be updated all 0.5 seconds.
 */
class Clock(val item: Item): UIProviderIF {

    val model = Model()

    override fun build(): Region {
        model.timer.start(10000L)
        val label = Label(model.timeProperty.value)
        label.styleClass.add("p")
        label.textProperty().bindBidirectional(model.timeProperty)
        return label
    }

    companion object {
        /**
         * A view model triggering update twice per second for time display
         */
        class Model {
            val timeProperty = SimpleStringProperty("00:00")

            // "suspend" is needed for timer instantiation
            suspend fun update() {
                timeProperty.value =
                    NoZoneDateTime.now().toDateTimeString(Language.CSV).substring(11, 14)
            }
            val timer = Timer(::update)
        }
    }

}