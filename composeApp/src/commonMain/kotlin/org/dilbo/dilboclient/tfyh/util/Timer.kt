package org.dilbo.dilboclient.tfyh.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KSuspendFunction0

class Timer(private val updateInterface: KSuspendFunction0<Unit>) {
    private var timer: Job? = null

    fun start(millis: Long) {
        val callback = updateInterface
        timer = CoroutineScope(EmptyCoroutineContext).launch {
            // repeat "eternally". Using a 500 ms interval this wll run for 34 years.
            repeat (Int.MAX_VALUE) {
                // enclose the callback into a try/catch block to ensure, that
                // the timer will nor stop due to an exception
                try {
                    callback()
                    delay(millis)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stop() {
        timer?.cancel()
    }
}