/**
 * tools-for-your-hobby
 * https://www.tfyh.org
 * Copyright  2023-2025  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.dilbo.dilboclient.tfyh.control

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.tfyh.data.Formatter
import org.dilbo.dilboclient.tfyh.data.ParserName
import org.dilbo.dilboclient.tfyh.util.Language
import org.dilbo.dilboclient.tfyh.util.LocalCache


class Logger(
    private val logName: String) {

    companion object {
        const val CACHE_PATH = "Log"
    }

    private val logFileSize = 1048576
    private var language: Language = Language.EN
    private var timeZone: TimeZone = TimeZone.currentSystemDefault()
    private var log: MutableList<String> = mutableListOf()
    private val localCache: LocalCache = LocalCache.getInstance()

    fun setLocale(language: Language, timeZone: TimeZone) {
        this.language = language
        this.timeZone = timeZone
    }

    fun log(severity: LoggerSeverity, caller: String, message: String) {
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val logLine = Formatter.format(now, ParserName.DATETIME, language) +
                "- $severity by $caller: $message"
        println(logLine)
        log += logLine

        val logStored = localCache.getItem(logName)
        if (logStored.length > logFileSize) {
            localCache.setItem("$CACHE_PATH/${logName}_previous", logStored)
            localCache.setItem("$CACHE_PATH/$logName", logLine)
        } else
            localCache.setItem("$CACHE_PATH/$logName", logStored + "\n" + logLine)
    }

    override fun toString() = logName
}