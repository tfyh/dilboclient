package org.dilbo.uiProvider

import io.ktor.util.StringValues
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.dilbo.app.EventHandler
import org.tfyh.control.EventHandlerIF
import org.tfyh.control.EventHandlerIF.EventType
import org.tfyh.data.Item
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.util.ResProviderIF
import org.tfyh.util.User
import java.io.ByteArrayInputStream


class Menu(val item: Item, val dimensions: ViewBuilder.Dimensions, val resProvider: ResProviderIF): UIProviderIF {

    private var limitedTo = arrayOf<String>()
    fun limitTo(optionsList: Array<String>) { limitedTo = optionsList }
    private var horizontal = (item.getChild("horizontal")?.value() ?: false) as Boolean
    fun setHorizontal(horizontal: Boolean) { this.horizontal = horizontal }

    // MENU ITEMS
    // ==========
    data class MenuItem (
        val menuItemConfig: Item, val resProvider: ResProviderIF
    ) {
        val icon: Image =
            Image(ByteArrayInputStream(resProvider.getDrawable(menuItemConfig.name())))
        val permissions = menuItemConfig.nodeReadPermissions()
        fun isAllowedMenuItem(): Boolean = User.getInstance().isAllowedItem(permissions)
        fun isHiddenMenuItem(): Boolean = User.getInstance().isHiddenItem(permissions)
    }


    override fun build(): Region {
        val eventHandler = EventHandler.getInstance()
        val menuItems = mutableMapOf<String, MenuItem>()
        for (menuItem in item.getChildren()) {
            val permissionsString: String = menuItem.getChild("permissions")?.valueStr() ?: "anonymous"
            val isDisplayAllowed = User.getInstance().isAllowedItem(permissionsString)
            val isOfSet = limitedTo.isEmpty() || (limitedTo.indexOf(menuItem.name()) >= 0)
            if (isDisplayAllowed && isOfSet)
                menuItems[menuItem.name()] = MenuItem(menuItem, resProvider)
        }
        val menuIcons = mutableListOf<ImageView>()
        for (menuItemName in menuItems.keys) {
            val menuItem = menuItems[menuItemName]
            if ((menuItem != null) && menuItem.isAllowedMenuItem() && !menuItem.isHiddenMenuItem()) {
                val imageView = ImageView(menuItem.icon)
                imageView.fitWidth = dimensions.widthPx.toDouble()
                imageView.fitHeight = dimensions.heightPx.toDouble()
                imageView.onMouseClicked = {
                    eventHandler.handleEvent(
                        EventHandlerIF.Event(EventType.MENU_EVENT,
                            menuItem.menuItemConfig.name(),
                            sourceItem = menuItem.menuItemConfig))
                }
                menuIcons.add(imageView)
            }
        }
        return if (horizontal) HBox(*menuIcons.toTypedArray())
               else VBox(*menuIcons.toTypedArray())
    }

}