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

import kotlinx.datetime.LocalDateTime

/**
 * A utility to hold empty, default, min, and max values for any parser
 */
object ParserConstraints {
    /**
     * The absolute limits for size constraints. They assume 64 Bit operation.
     */
    // for 32 bit - 2147483648
    private const val INT_EMPTY = -2_147_483_647
    internal const val INT_MIN = -2_147_483_646
    internal const val INT_MAX = 2_147_483_647

    // for 32 bit: - 3.40E+38
    private const val LONG_EMPTY = -9_223_372_036_854_775_807L
    internal const val LONG_MIN = -9_223_372_036_854_775_806L
    internal const val LONG_MAX = 9_223_372_036_854_775_807L

    // for 32 bit: - 3.40E+38
    private const val DOUBLE_EMPTY = -1.791E+308
    internal const val DOUBLE_MIN = -1.790E+308
    internal const val DOUBLE_MAX = 1.790E+308

    // time shall be limited to -99:25;29 .. 99:25;29, 100 hours is 360.000 seconds
    private const val TIME_EMPTY = -360_000
    internal const val TIME_MIN = -359_999
    internal const val TIME_MAX = 359_999
    
    const val FOREVER_SECONDS = 9.223372e+15

    // the native value for local dates is also localDateTime to simplify handling
    private val DATETIME_EMPTY = LocalDateTime(1582, 12, 31, 0, 0)
    internal val DATETIME_MIN = LocalDateTime(1583, 1, 1, 0, 0)
    internal val DATETIME_MAX = LocalDateTime(2999, 12, 31, 23, 59, 59)

    // this is an arbitrary limit which will fit into a MySQL data field
    private const val STRING_EMPTY = ""
    internal const val STRING_MIN = ""
    /**
     * There is no maximum String. 50 times z is an approximation to a String which most probably
     * ends up at the end of all sorting. Be careful on using it
     */
    private const val STRING_MAX = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"
    internal const val STRING_SIZE = 4096
    internal const val TEXT_SIZE = 65_536

    /* ------------------------------------------------------------------------ */
    /* ----- MIN, MAX, DEFAULT AND EMPTY VALUES ------------------------------- */
    /* ------------------------------------------------------------------------ */

    fun max(parser: ParserName): Any {
        return when (parser) {
            ParserName.BOOLEAN -> true
            ParserName.INT -> INT_MAX
            ParserName.LONG -> LONG_MAX
            ParserName.DOUBLE -> DOUBLE_MAX
            ParserName.DATE -> DATETIME_MAX.date
            ParserName.DATETIME -> DATETIME_MAX
            ParserName.TIME -> TIME_MAX
            ParserName.STRING -> STRING_MAX
            ParserName.NONE -> ""
            ParserName.INT_LIST -> mutableListOf<Int>()
            ParserName.STRING_LIST -> mutableListOf<String>()
        }
    }

    fun min(parser: ParserName): Any {
        return when (parser) {
            ParserName.BOOLEAN -> false
            ParserName.INT -> INT_MIN
            ParserName.LONG -> LONG_MIN
            ParserName.DOUBLE -> DOUBLE_MIN
            ParserName.DATE -> DATETIME_MIN.date
            ParserName.DATETIME -> DATETIME_MIN
            ParserName.TIME -> TIME_MIN
            ParserName.STRING,
            ParserName.NONE -> ""
            ParserName.INT_LIST -> mutableListOf<Int>()
            ParserName.STRING_LIST -> mutableListOf<String>()
        }
    }

    /**
     * The "empty"-value represents null. It will be formatted in any language and CSV to an
     * empty String and an empty String will be parsed to an empty value. At the SQL interface
     * the empty value is formatted to NULL, if the value allows null, else to its numeric
     * value, and the result of parsing an empty String or NULL instead.
     * To get a memory representation empty values are the lowest possible number and date,
     * an empty String or a Boolean false.
     * NB: that reduces the numeric range at the lower level. For a type Boolean this will
     * always return false.
     */
    internal fun empty(parser: ParserName): Any {
        return when (parser) {
            ParserName.BOOLEAN -> false
            ParserName.INT -> INT_EMPTY
            ParserName.LONG -> LONG_EMPTY
            ParserName.DOUBLE -> DOUBLE_EMPTY
            ParserName.DATE -> DATETIME_EMPTY.date
            ParserName.DATETIME -> DATETIME_EMPTY
            ParserName.TIME -> TIME_EMPTY
            ParserName.STRING -> STRING_EMPTY
            ParserName.NONE -> ""
            ParserName.INT_LIST -> mutableListOf<Int>()
            ParserName.STRING_LIST -> mutableListOf<String>()
        }
    }

    fun isEmpty(value: Any?, parser: ParserName): Boolean {
        if (value == null)
            return true
        return when (parser) {
            ParserName.BOOLEAN -> false
            ParserName.INT -> if (value is Int) (value <= INT_MIN) else false
            ParserName.LONG -> if (value is Long) (value == LONG_EMPTY) else false
            ParserName.DOUBLE -> if (value is Double) (value == DOUBLE_EMPTY) else false
            ParserName.DATE -> value == DATETIME_EMPTY.date // Local Date is an Object, no cast needed
            ParserName.DATETIME -> value == DATETIME_EMPTY // Local Datetime is an Object, no cast needed
            ParserName.TIME -> if (value is Int) (value == TIME_EMPTY) else false
            ParserName.STRING -> if (value is String) (value == STRING_EMPTY) else false
            ParserName.NONE -> true // none values are empty by definition
            ParserName.INT_LIST, ParserName.STRING_LIST -> if (value is MutableList<*>) value.size == 0 else false
        }
    }

}