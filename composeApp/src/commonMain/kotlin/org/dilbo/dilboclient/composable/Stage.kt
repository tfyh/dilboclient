package org.dilbo.dilboclient.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.dilbo.dilboclient.app.FormHandler
import org.dilbo.dilboclient.app.UIProvider
import org.dilbo.dilboclient.design.DilboTheme
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.design.dilboDimensions
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Item
import kotlin.math.max
import kotlin.math.min

object Stage {

    var density = 1.0F
    private var widthDp: Dp = 400.dp
    private var heightDp: Dp = 400.dp
    private var sizeFloat: Float = 0.0F

    // main content
    var sectorPropertyChildrenCount = 4
    val sectors: MutableMap<Item, Sector> = mutableMapOf()

    // modal content
    val emptyModalContent: @Composable () -> Unit = {
        Text(text = "The Modal", textAlign = TextAlign.Center,style = Theme.fonts.h2)
    }
    var modalContent = emptyModalContent

    // dialog content
    private val emptyDialogContent: @Composable () -> Unit = {
        Text(text = "The Dialog", textAlign = TextAlign.Center,style = Theme.fonts.h2)
    }
    var dialogContent = emptyDialogContent

    private fun setDimensions(width: Dp, height: Dp) {
        widthDp = width
        heightDp = height
        sizeFloat = width.value * height.value
    }

    fun getWidthDp() = widthDp
    fun getHeight() = heightDp
    fun update() {
        for (sectorItem in sectors.keys)
            sectors[sectorItem]?.update()
    }

    /**
     * Get he width of a form field depending on its span
     */
    fun getFieldWidth(columnsSpan: Int = 1): Dp {
        val formColumnsCount = FormHandler.responsiveFormColumnsCount()
        val basicWidth = when (formColumnsCount) {
            1 -> if (widthDp < 400.dp) widthDp * 0.9F
            else if (widthDp < 600.dp) widthDp * 0.75F
            else 350.dp

            2 -> if (widthDp < 600.dp) widthDp * 0.43F
            else if (widthDp < 900.dp) widthDp * 0.35F
            else 350.dp

            else -> if (widthDp < 900.dp) widthDp * 0.28F
            else if (widthDp < 1200.dp) widthDp * 0.25F
            else 350.dp
        }
        return ((basicWidth +
                dilboDimensions.regularSpacing) * min(columnsSpan, formColumnsCount) -
                dilboDimensions.regularSpacing)
    }

    fun getModalMaxWidth() =
        if (widthDp < 500.dp) widthDp * 0.9F
        else if (widthDp < 1200.dp) widthDp * 0.8F
        else 960.dp

    internal fun pxToDp(px: Double): Dp { return (px / density).dp }
    internal fun dpToPx(dp: Dp): Double { return (dp * density).value.toDouble() }
    fun isPortrait() = ((dpToPx(widthDp) / dpToPx(heightDp)) < 0.8)

    class StageViewModel: ViewModel() {
        // main visibility
        fun setVisibleMain(visibility: Boolean) { _versionMain.update { if (visibility) _versionMain.value + 1 else 0 } }
        private val _versionMain : MutableStateFlow<Int> = MutableStateFlow(0)
        internal val versionMain : StateFlow<Int> = _versionMain
        // modal visibility
        fun setVisibleModal(visibility: Boolean) { _versionModal.update { if (visibility) _versionModal.value + 1 else 0 } }
        private val _versionModal : MutableStateFlow<Int> = MutableStateFlow(0)
        internal val versionModal : StateFlow<Int> = _versionModal
        // dialog visibility
        fun setVisibleDialog(visibility: Boolean) { _versionDialog.update { if (visibility) _versionDialog.value + 1 else 0 } }
        private val _versionDialog : MutableStateFlow<Int> = MutableStateFlow(0)
        internal val versionDialog : StateFlow<Int> = _versionDialog
    }
    val viewModel = StageViewModel()

    /**
     * Show a text as dialog. The text may have formatting information.
     */
    fun showDialog(text: String) {
        dialogContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(Theme.dimensions.largeSpacing),
                horizontalAlignment = Alignment.CenterHorizontally) {
                DilboLabel(text)
                SubmitButton(text = "Ok", onClick = { viewModel.setVisibleDialog(false) } )
            }
        }
        viewModel.setVisibleDialog(true)
    }

    @Composable
    fun buildUi() {
        DilboTheme {
            // the main
            val showMain = viewModel.versionMain.collectAsStateWithLifecycle()
            if (showMain.value > 0) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                        .zIndex(1.0F)
                ) {
                    setDimensions(maxWidth, maxHeight)
                    val config = Config.getInstance()
                    val layoutName =
                        if (isPortrait())
                            config.getItem(".ui.profiles.dilbo.portrait").valueCsv()
                        else
                            config.getItem(".ui.profiles.dilbo.landscape").valueCsv()
                    val uiItem = config.getItem(".ui.layouts.$layoutName")
                    val sectorTemplate = config.getItem(".templates.sector")
                    sectorPropertyChildrenCount = sectorTemplate.getChildren().size
                    val layoutRoot = Sector(uiItem, isPortrait())
                    sectors[uiItem] = layoutRoot
                    layoutRoot.computeChildrenDimensions(widthDp, heightDp)
                    for (sectorItem in sectors.keys)
                        sectors[sectorItem]?.compose(isPortrait())
                }
            }
            // the modal
            val showModal = viewModel.versionModal.collectAsStateWithLifecycle()
            if (showModal.value > 0) {
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().zIndex(2.0F)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .wrapContentSize()
                            .background(Theme.colors.color_background_modal)
                            .shadow(elevation = Theme.dimensions.smallSpacing)
                    ) {
                        // repeat the check for the modal version here
                        // to ensure that the modal content is recomposed
                        // e.g. because of a form change within the modal
                        if (showModal.value > 0) {
                            modalContent()
                        }
                    }
                }
            }

            // the dialog
            val showDialog = viewModel.versionDialog.collectAsStateWithLifecycle()
            if (showDialog.value > 0) {
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().zIndex(2.0F)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(maxWidth * 0.8F)
                            .height(maxHeight * 0.6F)
                            .background(Theme.colors.color_background_form_input)
                            .shadow(elevation = Theme.dimensions.smallSpacing)
                    ) {
                        dialogContent()
                    }
                }
            }
        }
    }
}
