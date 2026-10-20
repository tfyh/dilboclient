/**
 * dilbo - digital logbook for Rowing and Canoeing
 * https://www.dilbo.org
 * Copyright:  2023-2025  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.dilbo.client

import org.tfyh.control.EventHandlerIF

/**
 * The class for user activity.
 */
class EventHandler private constructor() : EventHandlerIF {

    private val controller = Controller.getInstance()

    fun log(message: String) {
        controller.looper.append(EventHandlerIF.Event(EventHandlerIF.EventType.UI_EVENT, name = "log", message = message))
    }

    override fun handleEvent(event: EventHandlerIF.Event): Boolean {
        if (event.type == EventHandlerIF.EventType.UI_EVENT) {
            if (event.name == "log") {
                controller.model.addToLogFile(event.message)
                return true
            }
        }
        return false
    }

    companion object {
        val instance = EventHandler()
        fun getSingleton() = instance
    }

}
