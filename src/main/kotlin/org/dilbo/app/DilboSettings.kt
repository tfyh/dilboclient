/**
 *
 *       dilbo - digital logbook for Rowing and Canoeing
 *       -----------------------------------------------
 *       https://www.dilbo.org
 *
 * Copyright  2023-2026  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dilbo.app

import org.tfyh.data.Config
import org.tfyh.data.NoZoneDateTime
import kotlin.time.ExperimentalTime

class DilboSettings {

    companion object {
        private val dilboSettings = DilboSettings()
        fun getInstance() = dilboSettings
    }

    val today = NoZoneDateTime.now()
    val config = Config.getInstance()

    /**
     * Get the current sports year start for today. This is not a value, but a function, because
     * the application may continue to run over a year change.
     */
    fun sportsYearStart(): NoZoneDateTime {
        var sportsYearStartMonth = try {
                config.getItem(".club.habits.sports_year_start").valueStr().toInt()
            } catch (_: Exception) { 1 }
        if ((sportsYearStartMonth < 1) || (sportsYearStartMonth > 12))
            sportsYearStartMonth = 1
        val thisYearsSportsYearStart =
            NoZoneDateTime.parse(NoZoneDateTime.ymdToDateString(today.year(), sportsYearStartMonth, 1))
        val lastYearsSportsYearStart =
            NoZoneDateTime.parse(NoZoneDateTime.ymdToDateString(today.year() - 1, sportsYearStartMonth, 1))
        return if (today < thisYearsSportsYearStart)
            lastYearsSportsYearStart
        else
            thisYearsSportsYearStart
    }

    /**
     * Get the current. This is not a value, but a function, because during the
     * application runtime the configuration may change.
     */
    fun currentLogbook() = config.getItem(".app.user_preferences.logbook").valueStr()

}