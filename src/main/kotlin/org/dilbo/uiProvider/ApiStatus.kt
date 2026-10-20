package org.dilbo.uiProvider

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import org.dilbo.app.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.api.ApiStatusEnum
import org.tfyh.data.Item
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.util.ResProviderIF
import org.tfyh.util.Timer
import java.io.ByteArrayInputStream

/**
 * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
 */
class ApiStatus(val item: Item, val dimensions: ViewBuilder.Dimensions): UIProviderIF {

    val model = Model(this)
    val imageView: ImageView = ImageView()

    internal fun getApiStatusIcon(apiStatus: ApiStatusEnum, resProvider: ResProviderIF): Image {
        return when (apiStatus) {
            ApiStatusEnum.IDLE -> Image(ByteArrayInputStream(resProvider.getDrawable("connection_idle")))
            ApiStatusEnum.BUSY -> Image(ByteArrayInputStream(resProvider.getDrawable("connection_busy")))
            ApiStatusEnum.ALERT -> Image(ByteArrayInputStream(resProvider.getDrawable("connection_alert")))
        }
    }

    fun update(status: ApiStatusEnum) {
        imageView.image = getApiStatusIcon(status, ResProvider.getSingle())
    }

    /**
     * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
     */
    override fun build(): Region {
        model.timer.start(2000L)
        imageView.image = getApiStatusIcon(ApiStatusEnum.IDLE, ResProvider.getSingle())
        return HBox(1.0, imageView)
    }

    companion object {
        /**
         * A view model triggering update twice per second for API status display
         */
        class Model(val parent: ApiStatus) {
            private val apiStatus =
                SimpleIntegerProperty(ApiStatusEnum.IDLE.ordinal)

            init {
                apiStatus.addListener { _, oldValue, newValue ->
                    if (oldValue != newValue) {
                        parent.update(ApiStatusEnum.entries[newValue.toInt()])
                    }
                }
            }

            // "suspend" is needed for timer instantiation
            suspend fun update() {
                apiStatus.value = ApiHandler.apiHandler.getApiStatus().ordinal
            }
            val timer = Timer(::update)
        }
    }

}