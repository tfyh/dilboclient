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

package org.dilbo.dilboclient.tfyh.data

// for localization settings: Language, timezone
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dilbo.dilboclient.tfyh.data.Findings.addFinding
import org.dilbo.dilboclient.tfyh.util.Language
import kotlin.math.abs

/**
 * Provide the utility to transcode a CSV encoded String as received from the API or a UI input
 * into a typed value.
 */
object Parser {

    private var language = Language.EN
    private var timeZone = TimeZone.currentSystemDefault()

    fun setLocale(language: Language, timeZone: TimeZone) {
        Parser.language = language
        Parser.timeZone = timeZone
    }

    /* ------------------------------------------------------------------------ */
    /* ----- DATE AND DATETIME CLEANSING -------------------------------------- */
    /* ----- Using some heuristic to add missing bits ------------------------- */
    /* ------------------------------------------------------------------------ */

    /**
     * Add leading zeros before the number to obtain the "len" expected. If
     * n.toString.length >= len the number is converted to a String and not
     * changed.
     */
    private fun padZeros (n: Number, len: Int) = n.toString().padStart(len, '0')

    /**
     * Cleanse a date string into the YYYY-MM-DD format. If a single integer is passed, it is taken as year
     * and the 1st of January added. If two numbers are detected, they are taken as month an day
     * and the current year added.
     */
    private fun cleanseDate(dateString: String, language: Language): String?
    {
        // an empty String shall be an empty date
        if (dateString.isEmpty())
            return ""
        // parse the string to filter day, month and year
        var dateTemplate = language.dateTemplate
        var parts = dateString.split(dateTemplate[1]).toMutableList()

        // the result may not match, if the format is not according to the language expected.
        if ((parts.size == 1) && (parts[0].toDoubleOrNull() == null)) {
            if (dateString.split("-").size == 3)
            // assume ISO formatting, typically a result of a form entry
                dateTemplate = Language.CSV.dateTemplate
            else if (dateString.split(".").size == 3)
            // assume DE formatting
                dateTemplate = Language.DE.dateTemplate
            else if (dateString.split("/").size == 3)
            // assume EN formatting
                dateTemplate = Language.EN.dateTemplate
            parts = dateString.split(dateTemplate[1]).toMutableList()
        }
        // If a DateTime ist provided instead of a date, cut the time off of the last element
        if ((parts.size == 3) && parts[2].contains(" "))
            parts[2] = parts[2].split(" ")[0]
        // convert to Integer
        val partsInt: MutableList<Int> = mutableListOf()
        for (part in parts)
            try {
                val i = part.toInt()
                partsInt += i
            } catch (e: Exception) {
                partsInt += 0
            }

        // if there is just one value, assume it to be the year, if > 31
        // else to be the day of the month and add month and year
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val lastDayOfMonth = when (now.monthNumber) {
            4,6,9,11 -> 30
            2 -> 28  // no leap year support in date autocompletion.
            else -> 31
        }
        if (parts.size == 1) {
            if ((partsInt[0] > 1000) && (partsInt[0] < 2999))
            // a four digit integer in the date range is taken to be a year. Add first of Jánuary
                return "$dateString-01-01"
            else if ((partsInt[0] >= 1) && (partsInt[0] <= lastDayOfMonth)) {
                // an integer in the day of month range is taken ro be the actual month's day
                return "${now.year}-${now.monthNumber}-" + padZeros(partsInt[0],2)
            } else {
                // any other value is regarded as an error
                addFinding(1, dateString)
                return null
            }
        }

        val yearIsFirst = (dateTemplate.lowercase().startsWith("y"))
        // if just two integers were detected, assume that the year is missing and add the
        // current year
        if ((parts.size == 2) || ((parts.size == 3) && parts[2].isEmpty())) {
            val y = now.year
            val m = if (yearIsFirst) partsInt[0] else partsInt[1]
            val d = if (yearIsFirst) partsInt[1] else partsInt[0]
            if ((m >= 1) && (m <= 12) && (d >= 1) && (d <= lastDayOfMonth))
            // try to build a date, causes an exception if invalid
                try {
                    LocalDate(y, m, d)
                    return padZeros(y, 4) + "-" + padZeros(m,2) + "-" + padZeros(d, 2)
                } catch (e: Exception) {
                    addFinding(1, dateString)
                    return null
                }
        }

        // three numbers are given
        // if all are lower than 100, extend the year by a heuristic guess
        var y = if (yearIsFirst) partsInt[0] else partsInt[2]
        if (y < 100) {
            // extend two digits. Get the century
            val yearNow2Digit = now.year % 100
            val centuryNow = now.year - yearNow2Digit
            val centuryNext = centuryNow + 100
            val centuryPrevious = centuryNow - 100
            // apply heuristics: go 90 years back to 10 years forward
            y = if (yearNow2Digit < 90) {
                if (y > (yearNow2Digit + 10)) (centuryPrevious + y) else (centuryNow + y)
            } else {
                if (y > (yearNow2Digit + 10) % 100) (centuryNow + y) else (centuryNext + y)
            }
        }
        // try to build a date, causes an exception if invalid
        try {
            val m = partsInt[1]
            val d = partsInt[if (yearIsFirst) 2 else 0]
            LocalDate(y, m, d)
            return padZeros(y, 4) + "-" + padZeros(m,2) + "-" + padZeros(d, 2)
        } catch (e: Exception) {
            addFinding(1, dateString)
            return null
        }
    }

    /**
     * Cleanse a time string to HH:MM:SS format. Milliseconds are dropped.
     */
    private fun cleanseTime(timeString: String, noHours: Boolean): String?
    {
        if (timeString.length < 2)
            return null
        // split off the "minus", if existing.
        var sign = ""
        var times = timeString
        if (times[0] == '-') {
            times = times.substring(1).trim()
            sign = "-"
        }
        // cleanse the remainder
        val hms = times.split(":")
        if ((hms.size < 2) || (hms.size > 3))
            return null
        val hmsInt: MutableList<Int> = mutableListOf()
        for (part in hms)
            try {
                val i = part.toInt()
                hmsInt += i
            } catch (e: Exception) {
                hmsInt += 0
            }
        val hms0 = padZeros(hmsInt[0], 2)
        val hms1 = padZeros(hmsInt[1], 2)
        if (hms.size == 2)
            return if (noHours) sign + "00:$hms0:$hms1" else "$sign$hms0:$hms1:00"
        val hms2 = padZeros(hmsInt[2], 2)
        return "$sign$hms0:$hms1:$hms2"
    }

    /**
     * Cleanse a datetime string to YYYY-MM-DD HH:MM:SS format. Milliseconds are
     * dropped. If no date is given, the current date is inserted. If no time is
     * given, the current time is inserted.
     */
    private fun cleanseDateTime(datetimeString: String, language: Language): String?
    {
        val dt = datetimeString.trim().split(" ")
        if (dt.size == 1) {
            // try both, date or time
            val date = cleanseDate(dt[0], language)
            val time = cleanseTime(dt[0], false) // always with hours
            if (date != null)
                return "$date 00:00:00"
            else if (time != null) {
                val dtNow = Clock.System.now().toLocalDateTime(timeZone)
                val dateNow = padZeros(dtNow.year, 4) + "-" + padZeros(
                    dtNow.monthNumber,
                    2
                )
                return dateNow + "-" + padZeros(dtNow.dayOfMonth, 2)
            } else {
                addFinding(1, datetimeString.trim())
                return null
            }
        } else {
            val date = cleanseDate(dt[0], language)
            val time = cleanseTime(dt[1], false) // always with hours
            if ((date == null) || (time == null)) {
                addFinding(1, datetimeString.trim())
                return null
            }
            return "$date $time"
        }
    }

    /**
     * Parse a value from storage or the database for processing. Array values must start and end with square brackets
     * and comma separated (,), quoting is needed (like [a,b,", and c"]). Empty Strings are parsed into empty values
     * (see TypeConstraints) or empty Lists. For Language::SQL the String NULL without quotes is also parsed into an empty value.
     * Boolean values will be true for any non-empty String except the String "false" (not case-sensitive) and the String
     * "0". For Languages .CSV and .SQL quoted Strings are unquoted before parsing. The function never returns null.
     * If the value is not a string, but matches the target native type of the parser, it is returned unchanged.
     */
    fun parse(value: Any, parser: ParserName, language: Language = Parser.language): Any {
        if (value !is String) {
            if (isMatchingNative(value, parser))
                return value
            val valueForError = value.toString()
            addFinding(3, valueForError, value::class.simpleName ?: "unknown")
            return ParserConstraints.empty(parser)
        }
        // remove quotes, if existing.
        var toParse: String = value
        if ((language == Language.CSV)
            && value.startsWith( "\"") && value.endsWith("\""))
                toParse = value.substring(1, value.length - 1)
                    .replace("\"\"", "\"").trim()
        else if (language == Language.SQL) {
            if (value.startsWith( "'") && value.endsWith("'"))
                 toParse = value.substring(1, value.length - 1)
                     .replace("\\'", "'").trim()
            else
                 if (value.lowercase() == "null")
                    // Special case: unquoted NULL for Language.SQL
                     return ParserConstraints.empty(parser)
        }
        // parse value
        return when (parser) {
            ParserName.BOOLEAN -> parseBoolean(toParse)
            ParserName.INT -> parseInt(toParse, language)
            ParserName.INT_LIST -> parseList(toParse, language, ParserName.INT)
            ParserName.LONG -> parseLong(toParse, language)
            ParserName.DOUBLE -> parseDouble(toParse, language)
            ParserName.DATE -> parseDate(toParse, language)
            ParserName.DATETIME -> parseDateTime(toParse, language)
            ParserName.TIME -> parseTime(toParse)
            ParserName.STRING -> toParse
            ParserName.STRING_LIST -> parseList(toParse, language, ParserName.STRING)
            ParserName.NONE -> ""
        }
    }

    /**
     * Convert a String to boolean. Returns false, if bool_string = "FALSE" or
     * bool_string = "false", else this wil return true. Note that null or
     * undefined are also converted to false, see #parseSingle().
     */
    private fun parseBoolean(boolString: String): Boolean {
        return (boolString.isNotEmpty() && (boolString.lowercase() != "false")
                && (boolString != "0"))
    }

    /**
     * Convert a not-empty String to an integer number. If parsing fails this will
     * return Constraints.empty(Name.LONG).
     */
    private fun parseLong(longString: String, language: Language = Parser.language): Long {
        if (longString.isEmpty())
            return (ParserConstraints.empty(ParserName.LONG) as Long)
        var toParse = longString.trim().replace(" ", "")
        toParse =
            if (language.decimalPoint) toParse.replace(",", "")
            else toParse.replace(".", "")
        if (toParse.isEmpty())
            return ParserConstraints.empty(ParserName.LONG) as Long
        try {
            val ret = toParse.toLong()
            return ret
        } catch (e: Exception) {
            return (ParserConstraints.empty(ParserName.LONG) as Long)
        }
    }

    /**
     * Convert a String to an integer number. If parsing fails this will return
     * ParserConstraints.empty(ParserName.INT). If parsing results into an integer outside the Int
     * range, this will return the respective range limit
     */
    private fun parseInt(intString: String, language: Language = Parser.language): Int {
        if (intString.isEmpty())
            return (ParserConstraints.empty(ParserName.INT) as Int)
        val long = parseLong(intString, language)
        return if (long == (ParserConstraints.empty(ParserName.LONG) as Long))
            (ParserConstraints.min(ParserName.INT) as Int)
        else if (long < (ParserConstraints.min(ParserName.INT) as Int))
            (ParserConstraints.min(ParserName.INT) as Int)
        else if (long > (ParserConstraints.max(ParserName.INT) as Int))
            (ParserConstraints.max(ParserName.INT) as Int)
        else
            long.toInt()
    }

    /**
     * Convert a not-empty String to a number. In case of errors this wll be return
     * Constraints.empty(Name.DOUBLE)
     */
    private fun parseDouble(
        floatString: String,
        language: Language = Parser.language
    ): Double {
        var toParse = floatString.trim().replace(" ", "")
        toParse =
            if (language.decimalPoint) toParse.replace(",", "")
            else toParse.replace(".", "").replace(",", ".")
        if (toParse.isEmpty())
            return ParserConstraints.empty(ParserName.DOUBLE) as Double
        try {
            val ret = toParse.toDouble()
            return ret
        } catch (e: Exception) {
            return ParserConstraints.empty(ParserName.DOUBLE) as Double
        }
    }

    /**
     * Convert a String to a number of seconds. no limits to the number of hours
     * apply. In case of errors this wll be return Constraints.empty(Name.TIME)
     */
    private fun parseTime(timeString: String, noHours: Boolean = false): Int {
        val cleansed = cleanseTime(timeString, noHours)
        if (cleansed == null) {
            addFinding(2, timeString)
            return ParserConstraints.empty(ParserName.TIME) as Int
        }
        val sign = if (timeString[0] == '-') -1 else 1
        val hms = cleansed.split(":")
        var hour = 0L
        var minute = 0
        var second = 0
        try {
            hour = abs(hms[0].toLong())
            minute = hms[1].toInt()
            second = hms[2].toInt()
        } catch (ignored: Exception) {
        }
        return (sign * (hour * 3600 + minute * 60 + second)).toInt()
    }

    /**
     * Convert a String to a Date. If the year is two digits only, It will be assumed to be in the
     * range of this year -89years .. +10years. In case of errors this wll be return
     * Constraints.empty(Name.DATE)
     */
    private fun parseDate(
        dateString: String,
        language: Language = Parser.language
    ): LocalDate {
        // cleanse the date. THis will return a CSV formatted String or null
        val dateCleansed = try {
            cleanseDate(dateString, language)
        } catch (e: Exception) {
            addFinding(1, dateString)
            return ParserConstraints.empty(ParserName.DATE) as LocalDate
        }
        if (dateCleansed == null)
            return ParserConstraints.empty(ParserName.DATE) as LocalDate
        // parse the cleansed date
        try {
            return LocalDate.parse(dateCleansed, Language.CSV.dateFormat())
        } catch (e1: Exception) {
            addFinding(2, dateString)
            return (ParserConstraints.empty(ParserName.DATE) as LocalDate)
        }
    }

    /**
     * Convert a datetime String to a DateTimeImmutable Object. If no time is given, the current
     * time is inserted. In case of errors this will return Constraints.empty(Name.DATETIME)
     */
    private fun parseDateTime(
        datetimeString: String,
        language: Language = Parser.language
    ): LocalDateTime {
        // cleanse the datetime. This will return a CSV formatted String or null
        val dateTimeCleansed = try {
            cleanseDateTime(datetimeString, language)
        } catch (e: Exception) {
            addFinding(1, datetimeString)
            return ParserConstraints.empty(ParserName.DATETIME) as LocalDateTime
        }
        if (dateTimeCleansed == null)
            return ParserConstraints.empty(ParserName.DATETIME) as LocalDateTime
        // parse the cleansed datetime
        try {
            return LocalDateTime.parse(dateTimeCleansed, Language.CSV.dateTimeFormat())
        } catch (e1: Exception) {
            addFinding(2, datetimeString)
            return ParserConstraints.empty(ParserName.DATETIME) as LocalDateTime
        }
    }

    /**
     * Convert a String with a List of Integer like [1,2,3,4] into a MutableList<Int> by parsing
     * all values. Empty Strings or Strings that do not start and end with a square bracket will be
     * parsed into an empty list, empty elements into Constraints.empty(Name.INT).
     */
    private fun parseList(value: String, language: Language = Parser.language, parser: ParserName): MutableList<*> {
        if (value == "")
            return if (parser == ParserName.INT) mutableListOf<Int>() else mutableListOf<String>()
        val trimmed = if (value.startsWith("[") && value.endsWith("]"))
            value.substring(1, value.length - 1).trim() else value
        val values = Codec.splitCsvRow(trimmed, ",")

        val parsed: MutableList<*>
        if (parser == ParserName.INT) {
            parsed = mutableListOf<Int>()
            for (v in values)
                parsed += parseInt(v.trim(), language)
        } else {
            parsed = mutableListOf<String>()
            for (v in values)
                parsed += v.trim()
        }
        return parsed
    }

    /**
     * Get the best matching parser for a native value of unknown Type
     */
    fun nativeToParser(value: Any): ParserName =
        when (value) {
            is Boolean -> ParserName.BOOLEAN
            is Int -> ParserName.INT
            is Long -> ParserName.LONG
            is Double -> ParserName.DOUBLE
            is LocalDate -> ParserName.DATE
            is LocalDateTime -> ParserName.DATETIME
            is String -> ParserName.STRING
            is MutableList<*> -> {
                if (value.size == 0)
                    ParserName.STRING_LIST
                else when (value.first()) {
                    is Int -> ParserName.INT_LIST
                    else -> ParserName.STRING_LIST
                }
            }
            else -> ParserName.NONE
        }

    /**
     * returns true, if the native type of $value matches the ParserName requirements
     */
    fun isMatchingNative(value: Any, parserName: ParserName): Boolean =
        when (parserName) {
            ParserName.BOOLEAN -> (value is Boolean)
            ParserName.INT -> (value is Int)
            ParserName.INT_LIST -> (value is MutableList<*>) && ((value.size == 0) || (value[0] is Int))
            ParserName.LONG -> (value is Long)
            ParserName.DOUBLE -> (value is Double)
            ParserName.DATE -> (value is LocalDate)
            ParserName.DATETIME -> (value is LocalDateTime)
            ParserName.TIME -> (value is Int)
            ParserName.STRING -> (value is String)
            ParserName.STRING_LIST -> (value is MutableList<*>) && ((value.size == 0) || (value[0] is String))
            ParserName.NONE -> true
        }

}