package org.dilbo.dilboclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(HttpClient(OkHttp.create()))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
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