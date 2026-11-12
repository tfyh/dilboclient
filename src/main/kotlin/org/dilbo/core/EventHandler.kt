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

package org.dilbo.core

import org.dilbo.storage.ResProvider
import org.dilbo.uiProvider.ApiStatus
import org.dilbo.uiProvider.DilboAbout
import org.tfyh.control.EventHandlerIF
import org.tfyh.control.EventHandlerIF.EventType
import org.tfyh.data.DataBase
import org.tfyh.data.Indices
import org.tfyh.jfx.ui.ViewBuilder
import org.tfyh.util.I18n
import org.tfyh.util.User

class EventHandler : EventHandlerIF {

    // The UI events called. Used for go back
    private val calledEvents: MutableMap<EventType, MutableList<EventHandlerIF.Event>> = mutableMapOf()
    private val registeredHandlers: MutableMap<EventHandlerIF, MutableList<EventType>> = mutableMapOf()

    companion object {
        private const val MAX_CALLED_EVENTS_IN_QUEUE = 100
        private val eventHandler = EventHandler()
        fun getSingleton() = eventHandler
    }


    /**
     * All events go through the menu. To go back it is sufficient to call the
     * menu item that was used before this one. Returns true, if the rewind succeeded.
     */
    private fun goBackInMenuEvents(clearHistory: Boolean): Boolean {
        if (clearHistory)
            calledEvents.clear()
        else {
            if (calledEvents.isEmpty())
                return false
            // remove current menu event
            calledEvents[EventType.MENU_EVENT]?.removeAt(calledEvents.size - 1)
            // redo previous menu event
            if (calledEvents[EventType.MENU_EVENT]?.isNotEmpty() ?: false) {
                val previousUiEvent = calledEvents[EventType.MENU_EVENT]?.removeAt(calledEvents.size - 1)
                if (previousUiEvent != null)
                    handleEvent(previousUiEvent)
            }
        }
        return true
    }

    fun registerHandler(eventHandler: EventHandlerIF, eventType: EventType) {
        val registration = registeredHandlers[eventHandler] ?: mutableListOf()
        registration.add(eventType)
        registeredHandlers[eventHandler] = registration
    }

    /**
     * Return the uid of the last UI event.
     */
    fun getLastUid(eventType: EventType): String {
        val calledEventsOfType = calledEvents[eventType] ?: return ""
        return if (calledEventsOfType.isEmpty()) "" else calledEventsOfType.last().uid
    }

    fun handleMenuEvent(event: EventHandlerIF.Event): Boolean {
        val permissionString = event.sourceItem.nodeReadPermissions()
        val isAllowedAction = User.getInstance().isAllowedItem(permissionString)
        if (isAllowedAction) {
            val formHandler = FormHandler.getInstance()
            when (event.name) {
                // forms (menu actions)
                "enterTrip" -> formHandler.enterTripDo(event)
                "enterTripDone" -> formHandler.enterTripDone(event)
                "endTrip" -> formHandler.endTripDo(event)
                "endTripDone" -> formHandler.endTripDone()
                "editTrip" -> formHandler.editTripDo(event)
                "editTripDone" -> formHandler.editTripDone()
                "cancelTrip" -> formHandler.cancelTripDo(event)
                "cancelTripDone" -> formHandler.cancelTripDone()
                "reportDamage" -> formHandler.reportDamageDo(event)
                "reportDamageDone" -> formHandler.reportDamageDone(event)
                "requestReservation" -> formHandler.requestReservationDo(event)
                "requestReservationDone" -> formHandler.requestReservationDone(event)
                "sendMessage" -> formHandler.sendMessageDo(event)
                "sendMessageDone" -> formHandler.sendMessageDone(event)
                "displayDetails" -> UIProviderFactory.displayDetails(event)
                "editPreferences" -> formHandler.editPreferencesDo(event)
                "editPreferencesDone" -> formHandler.editPreferencesDone()
                // go back
                "backwards" -> { return goBackInMenuEvents(false) }
            }
            return true
        } else if (event.name == "backwards")
        // go back is always allowed, because it will call the former event which will
        // be checked for allowance then
            return goBackInMenuEvents(false)
        else {
            UIProviderFactory.displayDialog(
                I18n.getInstance().t("QJrL6O|Forbidden"),
                I18n.getInstance().t("Q9FnS3|I°m afraid action °%1° i...", event.name))
            return false
        }

    }

    /**
     * Handle all events coming in
     */
    override fun handleEvent(event: EventHandlerIF.Event): Boolean {

        // add event to history
        val calledEventsOfType = calledEvents[event.type] ?: mutableListOf()
        calledEventsOfType.add(event)
        if (calledEventsOfType.size > MAX_CALLED_EVENTS_IN_QUEUE)
            calledEventsOfType.removeAt(0)
        if (calledEvents[event.type] == null)
            calledEvents[event.type] = calledEventsOfType
        if (event.type != EventType.SYSTEM_EVENT)
            println("EventHandler.handleEvent() -> $event")

        // propagate the event to all registered handlers
        var isHandled = false
        for (registeredHandler in registeredHandlers.keys)
            if (registeredHandlers[registeredHandler]?.contains(event.type) ?: false)
                isHandled = (registeredHandler.handleEvent(event) or isHandled)

        // now apply the standard procedures
        when (event.type) {
            EventType.API_EVENT -> {
                when (event.name) {
                    "httpsError" -> {
                        // Stage.showDialogue(event.message); return true
                    }
                    "loginDo", "urlConfirmed" -> {
                        // this happens when the user enters the URL as part of the login process
                        FormHandler.getInstance().loginDo(event, true); return true
                    }
                    "loginDone" -> {
                        FormHandler.getInstance().loginDone(event)
                    }
                    "authenticationFailed", "unknowClient" -> {
                        FormHandler.getInstance().loginDo(event, false); return true
                    }
                    "sessionStarted", "userChanged" -> {
                        calledEvents.clear()
                        // TODO: what to do with the still pending transactions on user change???
                        // LocalCache.getInstance().clear() => this removes the cached configuration files. No option.
                        DataBase.getInstance().load()
                        return true
                    }
                    else -> return isHandled
                }
            }

            EventType.MENU_EVENT -> return handleMenuEvent(event)

            EventType.DATA_EVENT -> {
                // rebuild the asset status information
                AssetInfos.getInstance().handleEvent(event)
                return true
            }

            EventType.UI_EVENT -> {
                val providerType = event.sourceItem.getChild("type")?.valueStr() ?: ""
                when (providerType) {
                    "apiStatus" -> {
                        ViewBuilder.getInstance().showModal(ApiStatus.buildAbout())
                    }
                    "clock" -> {
                        val dilboAbout = DilboAbout(ResProvider.getSingleton())
                        ViewBuilder.getInstance().showModal(dilboAbout.build())
                    }
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
                        } else if (event.name != "collapse") // the collapse event is handled by the registered Lists
                        {
                            UIProviderFactory.displayDetails(event)
                            return true
                        }
                    }
                    else -> return isHandled
                }
            }

            EventType.SYSTEM_EVENT -> return isHandled // system events will only be handled by registered handlers
        }
        return isHandled
    }

}
