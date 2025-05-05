package org.dilbo.dilboclient.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.dilbo.dilboclient.app.UIProvider
import org.dilbo.dilboclient.design.Theme
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.Item
import kotlin.math.floor

class Sector(val item: Item, val isPortraitLayout: Boolean) {

    inner class SectorViewModel: ViewModel() {
        // the visibility is static and depends on the screen orientation. Change in
        // screen coordinates will trigger a recomposition rather than a change of state.
        private val _show : MutableStateFlow<Boolean> = MutableStateFlow(isPortraitLayout)
        val show : StateFlow<Boolean> = _show
    }

    // read the configuration
    private val sizeMin = (item.getChild("size_min")?.value() as Int? ?: 0).toDouble()
    private val weight = (item.getChild("weight")?.value() as Int? ?: 1.0).toDouble()
    private val horizontal = item.getChild("horizontal")?.value() as Boolean? ?: false
    private val providerName = item.getChild("provider")?.value() as String? ?: ""
    private val providerItem = Config.getInstance().getItem(".ui.providers.$providerName")
    internal val uiProvider = UIProvider(providerItem, item)

    // the pixel size and offset values
    private var w = 0.0
    private var h = 0.0
    private var x = 0.0
    private var y = 0.0

    // mark whether the size is the min size and the sector has children
    private var isMinSize = false
    private var isLeaf = true
    private val viewModel = SectorViewModel()

    private var width = Stage.pxToDp(w)
    private var height = Stage.pxToDp(h)
    private var offsetX = Stage.pxToDp(x)
    private var offsetY = Stage.pxToDp(y)

    private fun size() = if (horizontal) w else h

    private fun setAllDimensions() {
        w = floor(w * 100) / 100
        h = floor(h * 100) / 100
        x = floor(x * 100) / 100
        y = floor(y * 100) / 100
        width = Stage.pxToDp(w)
        height = Stage.pxToDp(h)
        offsetX = Stage.pxToDp(x)
        offsetY = Stage.pxToDp(y)
    }
    /**
     * Recursive layout computation. Call by the stage to compute all sizes and offsets. For
     * the composition only the leaf layer is drawn. Call it with stage width and height in dp, the offset
     * of the stage is always [0, 0]
     */
    fun computeChildrenDimensions(stageWidthDp: Dp = 0.dp, stageHeightDp: Dp = 0.dp) {
        w = if (stageWidthDp > 0.dp) Stage.dpToPx(stageWidthDp) else w
        h = if (stageHeightDp > 0.dp) Stage.dpToPx(stageHeightDp) else h
        var sumWeight = 0.0
        val childSectors: MutableList<Sector> = mutableListOf()
        // compute sum of weight
        for (child in item.getChildren()) {
            if ((child.valueType() == "template") && (child.valueReference() == ".templates.sector")) {
                isLeaf = false
                sumWeight += (child.getChild("weight")?.value() as Int? ?: 1.0).toDouble()
                val childSector = Sector(child, this.isPortraitLayout)
                // the list is used for size computation, the map for placement o stage
                childSectors.add(childSector)
            }
        }

        // compute sizes
        var sumMinSizes = 0.0
        var sumMinWeight = 0.0
        var sumSizes: Double
        var adjusted: Boolean
        do {
            // the computation must continue, until all size_min constraints are met.
            adjusted = false
            sumSizes = 0.0
            // the elements which get their minimum size do not take part at the distribution
            val pxPerWeight = (size() - sumMinSizes) / (sumWeight - sumMinWeight)
            for (sector in childSectors) {
                // compute size
                var sectorSize =
                    if (sector.isMinSize)
                        sector.sizeMin
                    else
                        sector.weight * pxPerWeight
                // adjust the size if its below the min constraint and sufficient room is left.
                if ((sectorSize < sector.sizeMin)
                    && (sumSizes + sector.sizeMin < size())) {
                    sectorSize = sector.sizeMin
                    sumMinSizes += sectorSize
                    sumMinWeight += sector.weight
                    sector.isMinSize = true
                    adjusted = true
                }
                if (horizontal) sector.w = sectorSize else sector.h = sectorSize
                sumSizes += sectorSize
                Stage.sectors[sector.item] = sector
            }
        // if a child sector has been adjusted, the remaining space is shrunk and
        // needs redistribution
        } while (adjusted)

        // set dimensions and offset for the child sectors
        var offset = 0.0
        for (sector in childSectors) {
            if (horizontal) {
                sector.h = h
                sector.y = y
                sector.x = x + offset
                offset += sector.w
            }
            else {
                sector.w = w
                sector.x = x
                sector.y = y + offset
                offset += sector.h
            }
            sector.setAllDimensions()
        }

        // drill down. Dimension of the child were already set.
        for (sector in childSectors)
            if (sector.item.getChildren().size > Stage.sectorPropertyChildrenCount)
                sector.computeChildrenDimensions()

    }

    fun update() {
        if (uiProvider.viewModel.version.value > 0)
            uiProvider.viewModel.show()
    }

    @Composable
    fun compose(forPortrait: Boolean) {
        val isPortrait : Boolean by viewModel.show.collectAsStateWithLifecycle()
        if ((isPortrait == forPortrait) && isLeaf) {
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .width(width).height(height)
                    .background(Theme.colors.color_background_body)
                    .border(
                        BorderStroke(
                            width = 2.dp,
                            color = Theme.colors.color_background_menubar
                        )
                    )
                    .zIndex(1.0F)
            ) {
                uiProvider.viewModel.show()
                uiProvider.compose()
            }
        }
    }

    // get a readable String for debugging purposes
    override fun toString(): String {
        return "$w*$h @($x,$y)"
    }

}