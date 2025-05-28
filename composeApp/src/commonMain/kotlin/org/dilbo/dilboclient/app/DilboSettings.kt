package org.dilbo.dilboclient.app

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.tfyh.data.Config

class DilboSettings {

    companion object {
        private val dilboSettings = DilboSettings()
        fun getInstance() = dilboSettings
    }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val config = Config.getInstance()

    /**
     * Get the current sports year start for today. This is not a value, but a function, because
     * the application may continue to run over a year change.
     */
    fun sportsYearStart(): LocalDate {
        var sportsYearStartMonth =
            config.getItem(".club.habits.sports_year_start").value() as Int
        if ((sportsYearStartMonth < 1) || (sportsYearStartMonth > 12))
            sportsYearStartMonth = 1
        val thisYearsSportsYearStart: LocalDate =
            LocalDate(today.year, sportsYearStartMonth, 1)
        val lastYearsSportsYearStart: LocalDate =
            LocalDate(today.year - 1, sportsYearStartMonth, 1)
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