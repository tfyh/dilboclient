package org.dilbo.dilboclient.app

import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Indices
import org.dilbo.dilboclient.tfyh.data.Item
import org.dilbo.dilboclient.tfyh.util.I18n
import org.dilbo.dilboclient.tfyh.util.User

class UIEventHandler {

    // The UI events called. Used for go back
    private val calledUiEvents: MutableList<UIEvent> = mutableListOf()

    companion object {
        private val uiEventHandler = UIEventHandler()
        fun getInstance() = uiEventHandler
    }

    data class UIEvent(
        val uid: String,
        val action: String,
        val sourceItem: Item
    ) {
        fun clickToAction(clickAction: String) = UIEvent(uid, clickAction, sourceItem)
        override fun toString() = "${sourceItem.getPath()} -> uid=$uid: $action"
    }
    
    /**
     * All events go through the menu. To go back it is sufficient to call the
     * menu item that was used before this one.
     */
    private fun goBack(clearHistory: Boolean) {
        if (clearHistory)
            calledUiEvents.clear()
        else {
            // remove current menu event
            calledUiEvents.removeAt(calledUiEvents.size - 1)
            // redo previous menu event
            if (calledUiEvents.size > 0) {
                val previousUiEvent = calledUiEvents.removeAt(calledUiEvents.size - 1)
                handleUiEvent(previousUiEvent)
            }
        }
    }

    /**
     * Return the uid of the last UI event.
     */
    fun getLastUid() =
        if (calledUiEvents.isEmpty()) ""
        else calledUiEvents.last().uid

    /**
     */
    fun handleButtonEvent(buttonEvent: String) {
        calledUiEvents.add(UIEvent("", buttonEvent, Config.getInstance().invalidItem))
    }
    /**
     * Handle all events coming in
     */
    fun handleUiEvent(uiEvent: UIEvent) {

        calledUiEvents.add(uiEvent)
        val config = Config.getInstance()
        val isProvider = uiEvent.sourceItem.isValid()
        val isMenuAction = (uiEvent.sourceItem.parent() == config.getItem(".access.menu.client"))
        val permissionString = uiEvent.sourceItem.nodeReadPermissions()
        val isAllowedAction = User.getInstance().isAllowedItem(permissionString)

        if (isAllowedAction) {
            println(uiEvent)
            when (uiEvent.action) {
                // forms (menu actions)
                "enterTrip" -> FormHandler.enterTripDo()
                "endTrip" -> FormHandler.endTripDo()
                "editTrip" -> FormHandler.editTripDo()
                "cancelTrip" -> FormHandler.cancelTripDo()
                "reportDamage" -> FormHandler.reportDamageDo()
                "requestReservation" -> FormHandler.requestReservationDo()
                "sendMessage" -> FormHandler.sendMessageDo()
                "displayDetails" -> UIProvider.displayDetails()
                "editPreferences" -> FormHandler.editPreferencesDo()
                // go back
                "backwards" -> goBack(false)
                // provider
                else -> if (isProvider) {
                    if (!isMenuAction) {
                        // typically an event triggered by a list item
                        if (uiEvent.action == "click") {
                            val indices = Indices.getInstance()
                            val tableName = indices.getTableForUid(uiEvent.uid)
                            val listEvent = when (tableName) {
                                "assets" -> uiEvent.clickToAction("enterTrip")
                                "logbook" -> uiEvent.clickToAction("endTrip")
                                // else includes by purpose damages and reservations
                                else -> uiEvent.clickToAction("displayDetails")
                            }
                            handleUiEvent(listEvent)
                        } else
                            UIProvider.displayDetails()
                    }
                }
            }
        } else
            UIProvider.displayDialog(I18n.getInstance().t("QJrL6O|Forbidden"),
                I18n.getInstance().t("Q9FnS3|I°m afraid action °%1° i...", uiEvent.action))
    }

}
