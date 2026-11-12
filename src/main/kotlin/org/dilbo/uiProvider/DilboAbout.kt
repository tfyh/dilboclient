package org.dilbo.uiProvider

import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.dilbo.core.UIProviderFactory.Companion.dilboAbout
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.jfx.ui.appLabel
import org.tfyh.util.ResProviderIF
import java.io.ByteArrayInputStream

class DilboAbout(val resProvider: ResProviderIF): UIProviderIF {

    override fun build(): Region {
        val text = dilboAbout().replace("~\\", "\n")
        val logo =
            Image(ByteArrayInputStream(resProvider.getBytes("drawable/app_logo.png")))
        return VBox(20.0, ImageView(logo).apply {
            fitWidth = 250.0
            fitHeight = 250.0
        }, appLabel(text)).apply {
            alignment = Pos.CENTER
        }
    }

}