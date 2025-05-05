package org.dilbo.dilboclient.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.dilbo.dilboclient.app.UIEventHandler
import org.dilbo.dilboclient.app.UIProvider
import org.dilbo.dilboclient.design.Theme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class ListItem (
    val uid: String,
    val icon: DrawableResource,
    val text: String,
    val providedBy: UIProvider,
    val textAlign: TextAlign = TextAlign.Start,
    val evenRow: Boolean = false,
    var visible: Boolean = true
) {

    inner class ListItemViewModel: ViewModel() {
        // the visibility is static and depends on the screen orientation. Change in
        // screen coordinates will trigger a recomposition rather than a change of state.
        private val _visible : MutableStateFlow<Boolean> = MutableStateFlow(this@ListItem.visible)
        val visible : StateFlow<Boolean> = _visible
    }
    private val viewModel = ListItemViewModel()
    private val uiEventHandler = UIEventHandler.getInstance()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun compose() {
        val visible : Boolean by viewModel.visible.collectAsStateWithLifecycle()
        if (visible) {
            var rowModifier = Modifier
                .fillMaxWidth()
                .padding(Theme.dimensions.smallSpacing)
                .combinedClickable(
                    onClick = { uiEventHandler.handleUiEvent(UIEventHandler.UIEvent(uid, "click", providedBy.item)) },
                    // do the same for a double click (desktop) and a long click (smartphone)
                    onLongClick = { uiEventHandler.handleUiEvent(UIEventHandler.UIEvent(uid, "doubleClick", providedBy.item)) },
                    onDoubleClick = { uiEventHandler.handleUiEvent(UIEventHandler.UIEvent(uid, "doubleClick", providedBy.item)) },
                )
            if (evenRow)
                rowModifier = rowModifier.background(Theme.colors.color_background_table_even_rows)
            Row(
                modifier = rowModifier
            ) {
                Image(
                    painterResource(icon),
                    contentDescription = "icon",
                    modifier = Modifier.height(24.dp)
                )
                Spacer(
                    modifier = Modifier.weight(0.02F)
                )
                DilboLabel(
                    text = text,
                    textAlign = textAlign,
                    modifier = Modifier.weight(0.91F)
                )
            }
        }
    }
}



