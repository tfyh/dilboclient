package org.dilbo.uiProvider

import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.appLabel

class Warning(val message: String): UIProviderIF {

    override fun build(): Pane {
        return VBox(appLabel(message))
    }

}