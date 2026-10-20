package org.dilbo.uiProvider

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dilbo.app.ResProvider
import org.tfyh.api.ApiHandler
import org.tfyh.data.Item
import org.tfyh.jfx.ui.UIProviderIF
import java.io.ByteArrayInputStream

/**
 * Provide an image loaded from the server. Used for tenant club logo asf.
 */
class RemoteImage(val item: Item, val httpClient: HttpClient?): UIProviderIF, ImageView() {

    // use the actual server URL, not the configured one. Maybe a manually entered URL on login.
    val serverUrl = ApiHandler.getInstance().getUrl()

    override fun build(): Region {
        val imageView = ImageView()
        val resProvider = ResProvider.getSingle()
        imageView.image = Image(ByteArrayInputStream(resProvider.getDrawable("none")))
        if (httpClient != null) {
            CoroutineScope(Dispatchers.IO).launch {
                var url = item.getChild("url")?.valueStr()
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
        return HBox(imageView)
    }

}