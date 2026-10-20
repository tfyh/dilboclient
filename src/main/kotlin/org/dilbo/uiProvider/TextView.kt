package org.dilbo.uiProvider

import javafx.scene.layout.Region
import org.dilbo.app.UIProviderFactory
import org.tfyh.jfx.ui.UIProviderIF
import org.tfyh.data.Item
import org.tfyh.jfx.ui.appLabel

class TextView(val item: Item): UIProviderIF {

    override fun build(): Region {
        val text =
            UIProviderFactory.fill((item.getChild("template")?.valueCsv() ?: "?template?"), mutableMapOf())
        return appLabel(text, "p")
    }
}