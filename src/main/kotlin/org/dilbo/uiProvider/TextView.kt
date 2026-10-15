package org.dilbo.uiProvider

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import org.dilbo.app.UIProviderFactory
import org.tfyh.kmp.ui.StyledText
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.data.Item
import org.tfyh.kmp.design.Theme

class TextView(val item: Item): UIProviderIF {

    val viewModel = TextViewModel()
    class TextViewModel : ViewModel()
    @Composable
    override fun ComposeContent() {
        val text =
            UIProviderFactory.fill((item.getChild("template")?.valueCsv() ?: "?template?"), mutableMapOf())
        Box (modifier = Modifier.padding(Theme.dimensions.regularSpacing)) {
            Text(
                StyledText.toAnnotatedString(text),
                style = Theme.fonts.p
            )
        }
    }
}