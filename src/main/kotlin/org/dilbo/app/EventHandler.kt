/**
 *
 *       dilbo - digital logbook for Rowing and Canoeing
 *       -----------------------------------------------
 *       https://www.dilbo.org
 *
 * Copyright  2023-2024  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dilbo.app

import org.tfyh.control.EventHandlerIF
import org.tfyh.control.EventHandlerIF.EventType
import org.tfyh.data.DataBase
import org.tfyh.data.Indices
import org.tfyh.util.I18n
import org.tfyh.util.User

class EventHandler : EventHandlerIF {

    // The UI events called. Used for go back
    private val calledEvents: MutableList<EventHandlerIF.Event> = mutableListOf()

    companion object {
        private val eventHandler = EventHandler()
        fun getInstance() = eventHandler
    }


    /**
     * All events go through the menu. To go back it is sufficient to call the
     * menu item that was used before this one. Returns true, if the rewind succeeded.
     */
    private fun goBack(clearHistory: Boolean): Boolean {
        if (clearHistory)
            calledEvents.clear()
        else {
            if (calledEvents.isEmpty())
                return false
            // remove current menu event
            calledEvents.removeAt(calledEvents.size - 1)
            // redo previous menu event
            if (calledEvents.isNotEmpty()) {
                val previousUiEvent = calledEvents.removeAt(calledEvents.size - 1)
                handleEvent(previousUiEvent)
            }
        }
        return true
    }

    /**
     * Return the uid of the last UI event.
     */
    fun getLastUid() =
        if (calledEvents.isEmpty()) ""
        else calledEvents.last().uid

    /**
     * Handle all events coming in
     */
    override fun handleEvent(event: EventHandlerIF.Event): Boolean {

        calledEvents.add(event)

        when (event.type) {
            EventType.API_EVENT -> {
                when (event.name) {
                    "httpsError" -> {
                        // Stage.showDialogue(event.message); return true
                    }
                    "urlConfirmed" -> {
                        // this happens when the user enters the URL as part of the login process
                        FormHandler.getInstance().loginDo(true); return true
                    }
                    "authenticationFailed", "unknowClient" -> {
                        FormHandler.getInstance().loginDo(false); return true
                    }
                    "sessionStarted" -> return true // nothing to do
                    "userChanged" -> {
                        calledEvents.clear()
                        // Stage.viewModel.setVisibleMain(true)
                        LocalCache.getInstance().clear()
                        DataBase.getInstance().load()
                        return true
                    }
                    else -> return false
                }
            }
            EventType.MENU_EVENT -> {
                val permissionString = event.sourceItem.nodeReadPermissions()
                val isAllowedAction = User.getInstance().isAllowedItem(permissionString)
                if (isAllowedAction) {
                    val formHandler = FormHandler.getInstance()
                    when (event.name) {
                        // forms (menu actions)
                        "login" -> formHandler.loginDo()
                        "enterTrip" -> formHandler.enterTripDo()
                        "endTrip" -> formHandler.endTripDo()
                        "editTrip" -> formHandler.editTripDo()
                        "cancelTrip" -> formHandler.cancelTripDo()
                        "reportDamage" -> formHandler.reportDamageDo()
                        "requestReservation" -> formHandler.requestReservationDo()
                        "sendMessage" -> formHandler.sendMessageDo()
                        "displayDetails" -> UIProviderFactory.displayDetails()
                        "editPreferences" -> formHandler.editPreferencesDo()
                        // go back
                        "backwards" -> { return goBack(false) }
                    }
                    return true
                } else if (event.name == "backwards")
                    // go back is always allowed, because it will call the former event which will
                    // be checked for allowance then
                    return goBack(false)
                else {
                    UIProviderFactory.displayDialog(
                        I18n.getInstance().t("QJrL6O|Forbidden"),
                        I18n.getInstance().t("Q9FnS3|I°m afraid action °%1° i...", event.name))
                    return false
                }
            }
            EventType.DATA_EVENT -> {
                // rebuild the asset status information
                AssetInfos.getInstance().handleEvent(event)
                // reflect the change in the user interface
                // Stage.handleEvent(event)
                return true
            }
            EventType.UI_EVENT -> {
                val providerType = event.sourceItem.getChild("type")?.valueStr() ?: ""
                when (providerType) {
                    "list" -> {
                        val listEvent = EventHandlerIF.Event(EventType.MENU_EVENT, "", event.uid, event.sourceItem)
                        // typically an event triggered by a list item
                        if (event.name == "click") {
                            val indices = Indices.getInstance()
                            val tableName = indices.getTableForUid(event.uid)
                            when (tableName) {
                                "assets" -> listEvent.name = "enterTrip"
                                "logbook" -> listEvent.name = "endTrip"
                                // else includes by purpose damages and reservations
                                else -> listEvent.name = "displayDetails"
                            }
                            return handleEvent(listEvent)
                        } else {
                            UIProviderFactory.displayDetails()
                            return true
                        }
                    }
                    else -> return false
                }
            }
        }
        return false
    }

}
