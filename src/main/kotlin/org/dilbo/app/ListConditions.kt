package org.dilbo.app

import org.tfyh.api.ListConditionsIF
import org.tfyh.data.Formatter
import org.tfyh.data.ParserName
import org.tfyh.util.Language

object ListConditions: ListConditionsIF {
    override fun getFurtherConditions(listName: String): Map<String, String> {
        return when (listName) {
            "logbook" -> mapOf(
                "sports_year_start" to
                        Formatter.format(DilboSettings.getInstance().sportsYearStart(),
                            ParserName.DATE, Language.CSV),
                "logbookname" to DilboSettings.getInstance().currentLogbook())
            else -> mapOf()
        }
    }
}