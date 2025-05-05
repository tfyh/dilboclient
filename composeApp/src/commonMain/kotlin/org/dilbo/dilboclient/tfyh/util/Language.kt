package org.dilbo.dilboclient.tfyh.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

enum class Language(
    val code: String,
    val dateTemplate: String,
    val decimalPoint: Boolean,
    // note: for PHP there is also writeNullAsNULL, only at SQL-Interface, not needed here
) {
    // real languages
    EN(
        code = "en",
        dateTemplate = "d-m-Y",
        decimalPoint = true,
    ),
    DE(
        code = "de",
        dateTemplate = "d.m.Y",
        decimalPoint = false,
    ),
    NL(
        code = "nl",
        dateTemplate = "d-m-Y",
        decimalPoint = false,
    ),
    FR(
        code = "fr",
        decimalPoint = true,
        dateTemplate = "d/m/Y",
    ),
    IT(
        code = "it",
        dateTemplate = "d/m/Y",
        decimalPoint = true,
    ),
    CSV(
        code = "csv",
        dateTemplate = "Y-m-d",
        decimalPoint = true,
    ),
    SQL(
        code = "sql",
        dateTemplate = "Y-m-d",
        decimalPoint = true,
    );

    companion object {
        fun valueOfOrDefault(languageCode: String): Language {
            return try { Language.valueOf(languageCode.uppercase()) }
            catch (e: Exception) { DE }
        }
    }

    // Localisation of time: all the same
    private fun timeFormat(): DateTimeFormat<LocalTime> = LocalTime.Format {
        hour(Padding.ZERO); char(':'); minute(Padding.ZERO); char(':'); second(Padding.ZERO)
    }

    // Localisation of date: all somehow different
    fun dateFormat(): DateTimeFormat<LocalDate> {
        val separatorChar: Char = dateTemplate[1]
        val sequence: String = dateTemplate.replace(separatorChar.toString(), "").lowercase()
        when (sequence) {
            "dmy" -> return LocalDate.Format {
                dayOfMonth(Padding.ZERO); char(separatorChar); monthNumber(Padding.ZERO); char(
                separatorChar
            ); year(Padding.ZERO)
            }

            "ymd" -> return LocalDate.Format {
                year(Padding.ZERO); char('-'); monthNumber(Padding.ZERO); char('-'); dayOfMonth(
                Padding.ZERO
            )
            }
            else -> return LocalDate.Format {
                year(Padding.ZERO); char('-'); monthNumber(Padding.ZERO); char('-'); dayOfMonth(
                Padding.ZERO
            )
            }
        }
    }

    // Localisation of dateTime: all somehow different
    fun dateTimeFormat(): DateTimeFormat<LocalDateTime> =
        LocalDateTime.Format {
            date(dateFormat()); char(' '); time(timeFormat())
        }

}