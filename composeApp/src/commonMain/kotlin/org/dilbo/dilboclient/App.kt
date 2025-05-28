package org.dilbo.dilboclient

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dilbo.dilboclient.api.ApiHandler
import org.dilbo.dilboclient.app.DilboSettings
import org.dilbo.dilboclient.app.FormHandler
import org.dilbo.dilboclient.app.UIProvider
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.util.LocalCache
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration
import kotlin.time.TimeSource

@OptIn(DelicateCoroutinesApi::class)
@Composable
@Preview
fun App(httpClient: HttpClient) {

    // instantiate and asynchronous load of the configuration
    // and cached tables
    val config = Config.getInstance()
    // initialize the local storage
    val localCache = LocalCache.getInstance()
    config.localFilePath = localCache.init()
    var urlCheck = -1

    // load the configuration. Because this involves resource reading, it must be
    // executed asynchronously.
    GlobalScope.launch {
        try {
            // load the configuration
            config.load()
            ApiHandler.getInstance().setHttpClient(httpClient)
            urlCheck = ApiHandler.getInstance().connect()
        } catch (e: Exception) {
            e.printStackTrace()
            UIProvider.displayDialog("Initialization failed.", "")
        }
    }
    // wait for the configuration  load to finish
    while (!config.ready || (urlCheck < 0) || (Stage.getWidthDp() < 0.dp))
        delay(duration = Duration.parse("0.1s"))
    delay(duration = Duration.parse("0.1s"))
    // for debugging purposes:
    // UIProvider.displayDialog("LOCAL: $fileLocation / SERVER: " + config.getItem(".app.server.url").valueCsv() + ": $urlCheck")

    // initializes the current logbook and the sports year start.
    DilboSettings.getInstance()

    // build the screen
    Stage.density = (LocalDensity.current.run { 256.dp.toPx() }) / 256.0F
    Stage.buildUi()

    // show the login screen and start the API, if the login was successful
    FormHandler.loginDo()

}

/**
 * The Android emulator requires a localhost address of "10.0.2.2" instead of the typical 127.0.0.1.
 * Use this function to replace the localhost in the server URL
 */
expect fun localhostUrl(): String
/**
 * Quick and basic KMP delay function.
 *
 * Do not use in production, only in KMP tests with JS targets.
 */
fun delay(duration: Duration) {
    val timeMark = TimeSource.Monotonic.markNow()
    while (timeMark.elapsedNow() < duration) {
        // do nothing
    }
}

