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

import javafx.scene.layout.Region
import javafx.util.Builder
import org.tfyh.jfx.ui.Looper
import org.tfyh.jfx.ui.ViewBuilder

/**
 * The controller for the application.
 */
class Controller private constructor() {

    companion object {
        private val instance = Controller()
        fun getInstance() = instance
    }

    val model: Model = Model()
    val interactor: EventHandler = EventHandler.getSingleton()
    val looper: Looper = Looper(interactor)
    private val viewBuilder: Builder<Region?> = ViewBuilder.getInstance()

    fun getView(): Region? {
        return viewBuilder.build()
    }

}
