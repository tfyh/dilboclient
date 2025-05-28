package org.dilbo.dilboclient

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.browser.document

// build with ./gradlew wasmJsBrowserDistribution
// find result in composeApp/build/dist/wasmJs/productionExecutable
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App(
            httpClient = HttpClient() {
                install(HttpTimeout) {
                    // timeout for establishing a connection (not in JavaScript, or Darwin
                    // timeout for a whole HTTP call (all platforms)
                    requestTimeoutMillis = 30000
                    // timeout for the maximum time in between two data packets (not in JavaScript)
            }
        }
        )
    }
}