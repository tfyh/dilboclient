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

package org.dilbo.uiProvider

import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.dilbo.core.UIProviderFactory.Companion.dilboAbout
import org.tfyh.data.Config
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.appLabel
import org.tfyh.util.ResProviderIF
import java.io.ByteArrayInputStream

class DilboAbout(val resProvider: ResProviderIF): UIProviderIF {

    override fun build(): Pane {
        val text = dilboAbout().replace("~\\", "\n")
        val logo =
            Image(ByteArrayInputStream(resProvider.getDrawable("app_logo.png")))
        return VBox(20.0, ImageView(logo).apply {
            fitWidth = 250.0
            fitHeight = 250.0
        }, appLabel(text, Config.getInstance().invalidItem)).apply {
            alignment = Pos.CENTER
        }
    }

}