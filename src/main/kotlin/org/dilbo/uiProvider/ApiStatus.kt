package org.dilbo.uiProvider

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dilboapp.shared.generated.resources.Res
import dilboapp.shared.generated.resources.connection_alert
import dilboapp.shared.generated.resources.connection_busy
import dilboapp.shared.generated.resources.connection_idle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.tfyh.api.ApiHandler
import org.tfyh.api.ApiStatusEnum
import org.tfyh.data.Config
import org.tfyh.kmp.ui.Dimensions
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.data.DataBase
import org.tfyh.data.Item
import org.tfyh.kmp.design.Theme
import org.tfyh.util.Timer

/**
 * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
 */
class ApiStatus(val item: Item, dimensions: Dimensions): UIProviderIF {

    val viewModel = ApiStatusViewModel(dimensions)

    internal fun getApiStatusIcon(apiStatus: ApiStatusEnum): DrawableResource {
        return when (apiStatus) {
            ApiStatusEnum.IDLE -> Res.drawable.connection_idle
            ApiStatusEnum.BUSY -> Res.drawable.connection_busy
            ApiStatusEnum.ALERT -> Res.drawable.connection_alert
        }
    }

    /**
     * Provide an API status icon. Will be updated all 0.5 seconds. Clicking on the item will reload all data.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ComposeContent() {
        viewModel.timer.start(2000L)
        val apiStatus = viewModel.apiStatus.collectAsStateWithLifecycle()
        val isPortrait = (viewModel.dimensions.isPortrait())
        val boxModifier = if (isPortrait) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()
        BoxWithConstraints (modifier = boxModifier.padding(Theme.dimensions.regularSpacing)) {
            val imageModifier = if (isPortrait) Modifier.height(maxWidth) else Modifier.width(maxHeight)
            Image(
                painterResource(getApiStatusIcon(apiStatus.value)),
                contentDescription = "icon",
                modifier = imageModifier
                    .combinedClickable(
                        onClick = {
                            // refresh application
                            Config.getInstance().getLocalCache()?.clear()
                            DataBase.getInstance().load()
                        },
                    )
            )
        }
    }

    companion object {
        /**
         * A view model triggering update twice per second for API status display
         */
        class ApiStatusViewModel(val dimensions: Dimensions): ViewModel() {
            private val _apiStatus = MutableStateFlow(ApiStatusEnum.IDLE) // private mutable state flow
            val apiStatus = _apiStatus.asStateFlow() // publicly exposed as read-only state flow

            // "suspend" is needed for timer instantiation
            suspend fun update() {
                val currentStatus = ApiHandler.apiHandler.getApiStatus()
                if (currentStatus != _apiStatus.value)
                    _apiStatus.update { ApiHandler.apiHandler.getApiStatus() }
            }
            val timer = Timer(::update)
        }
    }

}