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

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dilbo.storage.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.data.Item
import org.tfyh.jfx.design.AppDimensions
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import java.io.ByteArrayInputStream

/**
 * Provide an image loaded from the server. Used for tenant club logo asf.
 */
class RemoteImage(val item: Item, val httpClient: HttpClient?, val dimensions: ViewBuilder.Dimensions): UIProviderIF, ImageView() {

    // use the actual server URL, not the configured one. Maybe a manually entered URL on login.
    val serverUrl = ApiHandler.getInstance().getUrl()

    override fun build(): Pane {
        val imageView = ImageView()
        val resProvider = ResProvider.getSingleton()
        imageView.image = Image(ByteArrayInputStream(resProvider.getDrawable("none.png")))

        imageView.apply {
            if ((image.width / dimensions.widthPx) < (image.height / dimensions.heightPx))
                fitHeight = dimensions.heightPx.toDouble() - 4 * AppDimensions.SMALL_SPACING
            else fitWidth = dimensions.widthPx.toDouble() - 4 * AppDimensions.SMALL_SPACING
            preserveRatioProperty().set(true)
        }
        val imagePane = StackPane(imageView).apply {
            alignment = Pos.CENTER
            onMouseClicked = { e ->
                if (!e.isControlDown) // controlDown click is used for debugging.
                {
                    // TODO
                }
            }
        }

        if (httpClient != null) {
            CoroutineScope(Dispatchers.IO).launch {
                var url = item.getChild("resources")?.valueStr()
                if (url != null) {
                    if (!url.startsWith("http")) url = serverUrl + url
                    val httpResponse: HttpResponse = httpClient.get(url)
                    if (httpResponse.status.value in 200..299) {
                        val byteArrayBody: ByteArray = httpResponse.body()
                        imageView.image = Image(ByteArrayInputStream(byteArrayBody))
                    }
                }
            }
        }
        return imagePane
    }

}