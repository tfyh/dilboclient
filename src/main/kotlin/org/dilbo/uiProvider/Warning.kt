package org.dilbo.uiProvider

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import org.tfyh.kmp.ui.AppLabel
import org.tfyh.kmp.ui.UIProviderIF

class Warning(val message: String): UIProviderIF {

    @Composable
    override fun ComposeContent() {
        Box (contentAlignment = Alignment.Center) {
            AppLabel(message, textAlign = TextAlign.Center)
        }
    }
}