package org.dilbo.dilboclient.tfyh.util

import dilboclient.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class I18n private constructor() {

    companion object {
        private val i18n = I18n()
        fun getInstance() = i18n
    }

    // localization
    var map = mapOf<String, String>()
    private var loaded = false

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadResource(language: Language) {
        val i18nURI = "files/i18n/${language.code}.lrf"
        val lrf = Res.readBytes(i18nURI).decodeToString()
        val lines = lrf.split("\n")
        var token = "-"
        var text = ""
        var i = 0
        for (line in lines) {
            val pipeAt = line.indexOf("|")
            if (pipeAt >= 0) {
                if (pipeAt == 6) {
                    if (token.length == 6) // new language resource. Store current.
                        map = map.plus(Pair(token, text))
                    token = line.substring(0, 6)
                    text = line.substring(7)
                } else if (pipeAt == 0) { // continued multiline language resource text
                    text += "\n" + line.substring(1)
                }
            }
            i++
        }
        // add last entry
        if (token.isNotEmpty())
            map = map.plus(Pair(token, text))
        loaded = true
    }

    fun isValidI18nReference(toCheck: String): Boolean
    {
        return if ((toCheck.length < 7) || (toCheck.substring(6, 7) != "|"))
            false
        else if (! loaded)
            false
        else
            (map[toCheck.substring(0,6)] != null)
    }

    // translation and placeholder replacement
    fun t(vararg args: String): String {
        if (args.isEmpty())
            return ""
        val i18nResource = args[0]
        if ((i18nResource.length < 7) || (i18nResource.substring(6, 7) != "|"))
            return i18nResource
        val token = i18nResource.substring(0, 6);
        var text =
            if (!loaded)
                i18nResource.substring(7)
            else
                map[token] ?: i18nResource.substring(7)
        if (text.isEmpty())
            return i18nResource.substring(7)
        for (i: Int in 1 ..< args.size)
            text = text.replace("%$i", args[i])
        return text
    }

}