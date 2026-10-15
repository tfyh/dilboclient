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

import javafx.beans.property.StringProperty
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.util.Builder
import org.dilbo.client.Main.Companion.windowWidth

/**
 * The user interface provider class.
 */
class ViewBuilder(private val model: Model, private val interactor: Interactor) : Builder<Region?> {

    override fun build(): Region? {
        val css = this.javaClass.getResource("css/dilboClient.css")
        return if (css != null)
            HBox(6.0,
                createLogoView(),
                VBox(
                    headingLabel("dilbo <-> efa converter"),
                    createLogView())
                    .apply {
                        this.padding = Insets(20.0)
                        this.spacing = 6.0
                        this.stylesheets.add(css.toExternalForm())
                        prefWidth = windowWidth
                        // border = Border(BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
                    }
            ).apply {
                prefWidth = windowWidth
                // border = Border(BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT))
            }
        else null
    }

    private fun createLogoView(): Node {
        val imageView = ImageView(
            Image(this.javaClass.getResourceAsStream("drawables/app_logo.png"))
        ).apply {
            fitWidth = 128.0
            fitHeight = 128.0
        }
        return VBox(0.0,imageView)
    }

    private fun createLogView(): Node {
        return VBox(promptLabel("Displaying the log"),
            boundTextArea(model.logFileProperty()))
    }

    private fun boundTextArea(boundProperty: StringProperty): Node {
        val textArea = TextArea()
        textArea.textProperty().bindBidirectional(boundProperty)
        textArea.textProperty().addListener { _, _, _ ->
            run { textArea.scrollTop = Double.MAX_VALUE }
        }
        textArea.apply {
            prefRowCount = 60
            font = Font.font("Prompt",16.0)
        }
        return textArea
    }

    private fun promptLabel(contents: String): Node {
        return styledLabel(contents, "prompt-label")
    }

    private fun headingLabel(contents: String): Node {
        return styledLabel(contents, "heading-label")
    }

    private fun styledLabel(contents: String, styleClass: String): Node {
        val label = Label(contents)
        label.styleClass.add(styleClass)
        return label
    }
}
