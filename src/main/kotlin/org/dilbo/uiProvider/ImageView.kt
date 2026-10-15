package org.dilbo.uiProvider

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tfyh.api.ApiHandler
import org.tfyh.kmp.ui.Dimensions
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.data.Item
import org.tfyh.kmp.design.Theme

/**
 * Provide an image loaded from the server. Used for tenant club logo asf.
 */
class ImageView(val item: Item, dimensions: Dimensions): UIProviderIF {

    val viewModel = ImageViewModel(dimensions, item.getChild("resources")?.valueCsv() ?: "")

    // use the actual server URL, not the configured one. Maybe a manually entered URL on login.
    val serverUrl = ApiHandler.getInstance().getUrl()

    @Composable
    override fun ComposeContent() {
        val resourceUrlState by remember { mutableStateOf(viewModel.resourceUrl) }
        val resourceUrl = resourceUrlState.collectAsStateWithLifecycle().value
        val dimensionsState by remember { mutableStateOf(viewModel.dimensions) }
        val dimensions = dimensionsState.collectAsStateWithLifecycle().value
        if (resourceUrl.isNotEmpty()) {
            val url = serverUrl + "/dilbo/" + resourceUrl.replace("../", "")
            val fillStyle = if (dimensions.isPortrait()) ContentScale.FillWidth
                            else ContentScale.FillHeight
            AsyncImage(
                model = url,
                contentDescription = "clubLogo",
                contentScale = fillStyle,
                modifier = Modifier.padding(Theme.dimensions.smallSpacing).fillMaxSize()
            )
        }
    }

    companion object {
        /**
         * A view model triggering update twice per second for API status display
         */
        class ImageViewModel(dimensions: Dimensions, resourceUrl: String): ViewModel() {
            private val _resourceUrl = MutableStateFlow(resourceUrl) // private mutable state flow
            val resourceUrl = _resourceUrl.asStateFlow() // publicly exposed as read-only state flow

            private val _dimensions = MutableStateFlow(dimensions) // private mutable state flow
            val dimensions = _dimensions.asStateFlow() // publicly exposed as read-only state flow

            @Suppress("Unused")
            fun setUrl(resourceUrl: String) {
                _resourceUrl.update { resourceUrl }
            }
        }
    }

}