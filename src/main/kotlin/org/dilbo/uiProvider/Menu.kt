package org.dilbo.uiProvider

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.dilbo.core.EventHandler
import org.tfyh.control.EventHandlerIF
import org.tfyh.control.EventHandlerIF.EventType
import org.tfyh.data.Item
import org.tfyh.jfx.design.AppColours
import org.tfyh.jfx.design.AppDimensions
import org.tfyh.jfx.ui.Looper
import org.tfyh.jfx.ui.Sector
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.util.ResProviderIF
import org.tfyh.util.User
import java.io.ByteArrayInputStream


class Menu(val item: Item, val dimensions: ViewBuilder.Dimensions, val resProvider: ResProviderIF,
           val host: Sector?): UIProviderIF, EventHandlerIF {

    private var limitedTo = arrayOf<String>()
    fun limitTo(optionsList: Array<String>) { limitedTo = optionsList }
    private var horizontal = (this@Menu.item.getChild("horizontal")?.value() ?: false) as Boolean
    fun setHorizontal(horizontal: Boolean) { this.horizontal = horizontal }

    private val menuOptions = mutableMapOf<String, MenuOption>()
    private var selectedOption = ""
    private var uid = ""

    init {
        EventHandler.getSingleton().registerHandler(this, EventType.API_EVENT)
    }
    // MENU ITEMS
    // ==========
    data class MenuOption (
        val item: Item,
        private val resProvider: ResProviderIF
    ) {
        val icon: Image =
            Image(ByteArrayInputStream(resProvider.getBytes("drawable/menu_" + item.name() + ".png")))
        val permissions = item.nodeReadPermissions()
        fun isAllowedMenuItem(): Boolean = User.getInstance().isAllowedItem(permissions)
        fun isHiddenMenuItem(): Boolean = User.getInstance().isHiddenItem(permissions)
    }

    /**
     * Set the uid of the record presetting the form, when clicking on the menu
     */
    fun setUid(uid: String) { this.uid = uid }
    /**
     * Set the currently selected option of the menu which will not be responding on clicks.
     */
    fun setSelectedOption(selectedOption: String) {
        this.selectedOption = selectedOption
    }
    override fun build(): Region {
        val user = User.getInstance()

        menuOptions.clear()
        for (menuItem in this@Menu.item.getChildren()) {
            val permissionsString: String = menuItem.nodeReadPermissions()
            val isDisplayAllowed = user.isAllowedItem(permissionsString)
            val isOfSet = limitedTo.isEmpty() || (limitedTo.indexOf(menuItem.name()) >= 0)
            if (isDisplayAllowed && isOfSet)
                menuOptions[menuItem.name()] = MenuOption(menuItem, resProvider)
        }
        val menuIcons = mutableListOf<StackPane>()
        // All menu images must be squares, their size is fit to the smaller dimension of the menu sector
        val hw = (if (horizontal) dimensions.heightPx else dimensions.widthPx) - 4 * AppDimensions.smallSpacing
        for (menuOptionName in menuOptions.keys) {
            val menuOption = menuOptions[menuOptionName]
            if (menuOption != null) {
                val showOption = menuOption.isAllowedMenuItem()
                        && (!menuOption.isHiddenMenuItem() || (limitedTo.indexOf(menuOptionName) >= 0))
                val imageView = ImageView(menuOption.icon).apply {
                    fitWidth = hw
                    fitHeight = hw
                }
                val tooltip = Tooltip(menuOption.item.label() + " [" + menuOption.hashCode() + "]")
                Tooltip.install(imageView, tooltip)
                if (showOption) menuIcons.add(StackPane(imageView).apply {
                    border = menuBorder
                    background = menuBackground
                    if (menuOptionName != selectedOption) {
                        cursor = Cursor.HAND
                        setOnMouseEntered { background = menuBackgroundHover }
                        setOnMouseExited { background = menuBackground }
                        setOnMouseClicked {
                            Looper.getSingleton().append(
                                EventHandlerIF.Event(
                                    EventType.MENU_EVENT,
                                    menuOption.item.name(), uid, menuOption.item
                                )
                            )
                        }
                    } else {
                        background = menuBackgroundHover
                    }
                })
            }
        }
        val menuPane = if (horizontal) HBox(*menuIcons.toTypedArray()).apply { alignment = Pos.CENTER }
                       else VBox(*menuIcons.toTypedArray()).apply { alignment = Pos.CENTER }
        return menuPane.apply { background = Sector.sectorBackground }
    }

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if ((event.type == EventType.API_EVENT) &&
            ((event.name == "sessionStarted") or (event.name  == "userChanged"))) {
            host?.update()
            return true
        }
        return false
    }

    companion object {
        val menuInsets = Insets(AppDimensions.smallSpacing)
        val menuBackground = Background(BackgroundFill(
            AppColours.background_body, CornerRadii.EMPTY, menuInsets
        ))
        val menuBackgroundHover = Background(BackgroundFill(
            AppColours.background_form_input_hover, CornerRadii.EMPTY, menuInsets
        ))
        val menuBorder = Border(BorderStroke(
            AppColours.background_body, BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY, BorderWidths(0.0), menuInsets
        ))
    }

}