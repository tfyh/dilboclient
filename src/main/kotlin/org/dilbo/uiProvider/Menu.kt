package org.dilbo.uiProvider

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dilboapp.shared.generated.resources.Res
import dilboapp.shared.generated.resources.menu_cancelTrip
import dilboapp.shared.generated.resources.menu_editPreferences
import dilboapp.shared.generated.resources.menu_editTrip
import dilboapp.shared.generated.resources.menu_endTrip
import dilboapp.shared.generated.resources.menu_enterTrip
import dilboapp.shared.generated.resources.menu_reportDamage
import dilboapp.shared.generated.resources.menu_requestReservation
import dilboapp.shared.generated.resources.menu_sendMessage
import dilboapp.shared.generated.resources.none
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.dilbo.app.EventHandler
import org.jetbrains.compose.resources.painterResource
import org.tfyh.kmp.ui.Dimensions
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.control.EventHandlerIF
import org.tfyh.control.EventHandlerIF.EventType
import org.tfyh.data.Config
import org.tfyh.data.Item
import org.tfyh.kmp.design.Theme
import org.tfyh.util.User

class Menu(val item: Item, dimensions: Dimensions): UIProviderIF {

    val viewModel = MenuViewModel(dimensions)

    // MENU ITEMS
    // ==========
    data class MenuItem (
        val menuItemConfig: Item
    ) {
        val permissions = menuItemConfig.nodeReadPermissions()
        val icon = when (menuItemConfig.name()) {
            "enterTrip" -> Res.drawable.menu_enterTrip
            "endTrip" -> Res.drawable.menu_endTrip
            "editTrip" -> Res.drawable.menu_editTrip
            "cancelTrip" -> Res.drawable.menu_cancelTrip
            "reportDamage" -> Res.drawable.menu_reportDamage
            "requestReservation" -> Res.drawable.menu_requestReservation
            "sendMessage" -> Res.drawable.menu_sendMessage
            "editPreferences" -> Res.drawable.menu_editPreferences
            else -> Res.drawable.none
        }
        fun isAllowedMenuItem(): Boolean = User.getInstance().isAllowedItem(permissions)
        fun isHiddenMenuItem(): Boolean = User.getInstance().isHiddenItem(permissions)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ComposeMenu() {
        val eventHandler = EventHandler.getInstance()

        val menuItemsState by remember { mutableStateOf(viewModel.menuItems) }
        val menuItems = menuItemsState.collectAsState().value
        val dimensionsState by remember { mutableStateOf(viewModel.dimensions) }
        val horizontal = !dimensionsState.collectAsState().value.isPortrait()

        for (menuItemName in menuItems.keys) {
            val menuItem = menuItems[menuItemName]
            if ((menuItem != null) && menuItem.isAllowedMenuItem() && !menuItem.isHiddenMenuItem()) {
                val boxModifier = if (horizontal) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()
                BoxWithConstraints (
                    modifier = boxModifier.padding(Theme.dimensions.smallSpacing)
                ) {
                    val imageModifier = if (horizontal) Modifier.height(maxHeight) else Modifier.width(maxWidth)
                    Image(
                        painterResource(menuItem.icon),
                        contentDescription = "icon",
                        modifier = imageModifier
                            .combinedClickable(
                                onClick = {
                                    eventHandler.handleEvent(
                                        EventHandlerIF.Event(EventType.MENU_EVENT,
                                            menuItem.menuItemConfig.name(),
                                            sourceItem = menuItem.menuItemConfig))
                                }
                            )
                    )
                }
            }
        }
    }

    @Composable
    override fun ComposeContent() {
        val dimensionsState by remember { mutableStateOf(viewModel.dimensions) }
        val dimensions = dimensionsState.collectAsStateWithLifecycle().value
        val isPortrait = dimensions.isPortrait()
        if (isPortrait)
            Column { ComposeMenu() }
        else
            Row { ComposeMenu() }
    }
    companion object {
        /**
         * A view model triggering update twice per second for API status display
         */
        fun getMenu(menuType: String): Map<String, MenuItem> {
            val config = Config.getInstance()
            val menu = config.getItem(".access.menus.$menuType")
            val menuItems = mutableMapOf<String, MenuItem>()
            for (menuItem in menu.getChildren()) {
                val permissionsString: String = menuItem.getChild("permissions")?.valueStr() ?: "anonymous"
                val isDisplayAllowed = User.getInstance().isAllowedItem(permissionsString)
                if (isDisplayAllowed)
                    menuItems[menuItem.name()] = MenuItem(menuItem)
            }
            return menuItems.toMap()
        }

        class MenuViewModel(dimensions: Dimensions) : ViewModel() {
            private val _menuItems = MutableStateFlow(getMenu("client")) // private mutable state flow
            val menuItems = _menuItems.asStateFlow() // publicly exposed as read-only state flow
            private val _dimensions = MutableStateFlow(dimensions) // private mutable state flow
            val dimensions = _dimensions.asStateFlow() // publicly exposed as read-only state flow

            fun refresh() {
                _menuItems.update { getMenu("client") }
            }

        }
    }


}