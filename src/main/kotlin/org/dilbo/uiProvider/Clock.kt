package org.dilbo.uiProvider

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tfyh.kmp.ui.AppLabel
import org.tfyh.kmp.ui.UIProviderIF
import org.tfyh.data.Item
import org.tfyh.data.NoZoneDateTime
import org.tfyh.util.Language
import org.tfyh.util.Timer

/**
 * Provide a date and time display. Will be updated all 0.5 seconds.
 */
class Clock(val item: Item): UIProviderIF {

    val viewModel = ClockViewModel()

    /**
     * Provide a time display. Will be updated all 10 seconds.
     */
    @Composable
    override fun ComposeContent() {
        viewModel.timer.start(10000L)
        val time = viewModel.time.collectAsStateWithLifecycle()
        Box (contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            AppLabel(time.value, textAlign = TextAlign.Center)
        }
    }

    companion object {
        /**
         * A view model triggering update twice per second for time display
         */
        class ClockViewModel : ViewModel() {
            private val _time = MutableStateFlow("") // private mutable state flow
            val time = _time.asStateFlow() // publicly exposed as read-only state flow

            // "suspend" is needed for timer instantiation
            suspend fun update() {
                _time.update {
                    NoZoneDateTime.now().toDateTimeString(Language.CSV).substring(11, 16)
                } // atomic, safe for concurrent use
            }
            val timer = Timer(::update)
        }
    }

}