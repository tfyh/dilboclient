package org.dilbo.uiProvider

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dilbo.storage.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.api.ApiStatusEnum
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

    override fun build(): Region {
        val imageView = ImageView()
        val resProvider = ResProvider.getSingleton()
        imageView.image = Image(ByteArrayInputStream(resProvider.getBytes("none")))

        imageView.apply {
            if ((image.width / dimensions.widthPx) < (image.height / dimensions.heightPx))
                fitHeight = dimensions.heightPx.toDouble() - 4 * AppDimensions.smallSpacing
            else fitWidth = dimensions.widthPx.toDouble() - 4 * AppDimensions.smallSpacing
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