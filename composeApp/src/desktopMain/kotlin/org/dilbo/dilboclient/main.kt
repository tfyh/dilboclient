package org.dilbo.dilboclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

// build with ./gradlew packageDeb
// find result in composeApp/build/compose/binaries/main/deb
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "dilboclient",
    ) {
        App(
            httpClient = HttpClient(OkHttp.create()) {
                install(HttpTimeout) {
                    // timeout for establishing a connection (not in JavaScript, or Darwin
                    connectTimeoutMillis = 10000
                    // timeout for a whole HTTP call (all platforms)
                    requestTimeoutMillis = 30000
                    // timeout for the maximum time in between two data packets (not in JavaScript)
                    socketTimeoutMillis = 600000
                }
            }
        )
    }
}