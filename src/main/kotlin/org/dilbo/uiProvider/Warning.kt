package org.dilbo.uiProvider

import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.appLabel

class Warning(val message: String): UIProviderIF {

    override fun build(): Region {
        return VBox(appLabel(message, "h3"))
    }

}