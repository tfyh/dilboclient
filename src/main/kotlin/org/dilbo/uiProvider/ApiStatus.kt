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

import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.dilbo.storage.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.control.EventHandlerIF
import org.tfyh.data.Config
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

    fun update(status: ApiHandler.ApiStatusEnum) {
        imageView.image = getApiStatusIcon(status, ResProvider.getSingleton())
    }

    /**
     * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
     */
    override fun build(): Pane {
        val hw = (if (dimensions.isPortrait()) dimensions.widthPx else dimensions.heightPx) - 8 * AppDimensions.SMALL_SPACING

        model.timer.start(2000L)
        imageView.apply {
            image = getApiStatusIcon(ApiHandler.ApiStatusEnum.IDLE, ResProvider.getSingleton())
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
                SimpleIntegerProperty(ApiHandler.ApiStatusEnum.IDLE.ordinal)

            init {
                apiStatus.addListener { _, oldValue, newValue ->
                    if (oldValue != newValue) {
                        parent.update(ApiHandler.ApiStatusEnum.entries[newValue.toInt()])
                    }
                }
            }

            // "suspend" is needed for timer instantiation
            suspend fun update() {
                apiStatus.value = ApiHandler.getInstance().getApiStatus().ordinal
            }
            val timer = Timer(::update)
        }

        private fun getApiStatusIcon(apiStatus: ApiHandler.ApiStatusEnum, resProvider: ResProviderIF): Image {
            return when (apiStatus) {
                ApiHandler.ApiStatusEnum.IDLE -> Image(ByteArrayInputStream(resProvider.getDrawable("connection_idle.png")))
                ApiHandler.ApiStatusEnum.BUSY -> Image(ByteArrayInputStream(resProvider.getDrawable("connection_busy.png")))
                ApiHandler.ApiStatusEnum.ALERT -> Image(ByteArrayInputStream(resProvider.getDrawable("connection_alert.png")))
            }
        }

        fun buildAbout(): Pane {
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
            }, appLabel(apiAbout, Config.getInstance().invalidItem)).apply {
                alignment = Pos.CENTER
            }
        }


    }

}