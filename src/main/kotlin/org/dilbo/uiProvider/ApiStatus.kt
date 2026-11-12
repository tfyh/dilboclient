package org.dilbo.uiProvider

import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.dilbo.storage.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.api.ApiStatusEnum
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Item
import org.tfyh.jfx.design.AppDimensions
import org.tfyh.jfx.ui.Looper
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.jfx.ui.appLabel
import org.tfyh.util.ResProviderIF
import org.tfyh.util.Timer
import java.io.ByteArrayInputStream

/**
 * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
 */
class ApiStatus(val item: Item, val dimensions: ViewBuilder.Dimensions): UIProviderIF {

    val model = Model(this)
    val imageView: ImageView = ImageView()

    fun update(status: ApiStatusEnum) {
        imageView.image = getApiStatusIcon(status, ResProvider.getSingleton())
    }

    /**
     * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
     */
    override fun build(): Region {
        val hw = (if (dimensions.isPortrait()) dimensions.widthPx else dimensions.heightPx) - 8 * AppDimensions.smallSpacing

        model.timer.start(2000L)
        imageView.apply {
            image = getApiStatusIcon(ApiStatusEnum.IDLE, ResProvider.getSingleton())
            fitWidth = hw
            fitHeight = hw
        }
        val statusPane = StackPane(imageView).apply {
            alignment = Pos.CENTER
            onMouseClicked = { e ->
                if (!e.isControlDown) // controlDown click is used for debugging.
                    Looper.instance.append(EventHandlerIF.Event(EventHandlerIF.EventType.UI_EVENT,
                        "apiAbout", sourceItem = item))
            }
        }
        return statusPane
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
                apiStatus.value = ApiHandler.getInstance().getApiStatus().ordinal
            }
            val timer = Timer(::update)
        }

        private fun getApiStatusIcon(apiStatus: ApiStatusEnum, resProvider: ResProviderIF): Image {
            return when (apiStatus) {
                ApiStatusEnum.IDLE -> Image(ByteArrayInputStream(resProvider.getBytes("drawable/connection_idle.png")))
                ApiStatusEnum.BUSY -> Image(ByteArrayInputStream(resProvider.getBytes("drawable/connection_busy.png")))
                ApiStatusEnum.ALERT -> Image(ByteArrayInputStream(resProvider.getBytes("drawable/connection_alert.png")))
            }
        }

        fun buildAbout(): Region {
            val apiHandler = ApiHandler.getInstance()
            val apiStatus = apiHandler.getApiStatus()
            val apiAbout = "URL: " +  apiHandler.getUrl() + "\n" +
                    "Status: " +  apiStatus + "\n" +
                    "server: " + apiHandler.getWelcomeMessage().replace("//", "~\\")
            val apiStatusIcon =
                getApiStatusIcon(apiStatus, ResProvider.getSingleton())
            return VBox(20.0, ImageView(apiStatusIcon).apply {
                fitWidth = 250.0
                fitHeight = 250.0
            }, appLabel(apiAbout)).apply {
                alignment = Pos.CENTER
            }
        }


    }

}