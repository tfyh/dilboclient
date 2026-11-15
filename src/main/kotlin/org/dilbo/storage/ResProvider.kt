package org.dilbo.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfyh.control.LoggerSeverity
import org.tfyh.data.Config
import org.tfyh.util.ResProviderIF

/**
 * Provides access to the jar-bundled resources. Because this class is located at org/dilbo/storage within the java
 * archive, all resource files will also end up there. Make sure the jar includes the sources.
 */
class ResProvider private constructor(): ResProviderIF {

    companion object {
        val instance = ResProvider()
        fun getSingleton() = instance

        val resourceFileList = mutableMapOf(
            // Because there is no way to list files in the jar archives directory, the list has to
            // be provided here. Make sure it is the complete list.
            "drawables" to listOf("app_logo.png",
                "boat_available.png",
                "boat_booked.png",
                "boat_damaged.png",
                "boat_onthewater.png",
                "clubLogo.png",
                "connection_alert.png",
                "connection_busy.png",
                "connection_idle.png",
                "menu_cancelTrip.png",
                "menu_displayList.png",
                "menu_displayRanking.png",
                "menu_editPreferences.png",
                "menu_editTrip.png",
                "menu_endTrip.png",
                "menu_enterTrip.png",
                "menu_find.png",
                "menu_reportDamage.png",
                "menu_requestReservation.png",
                "menu_sendMessage.png",
                "none.png"),
            "fonts" to listOf("prompt_bold.ttf",
                "prompt_bold_italic.ttf",
                "prompt_italic.ttf",
                "prompt_medium.ttf")
        )

    }

    val drawables = mutableMapOf<String, ByteArray>()
    val fonts = mutableMapOf<String, ByteArray>()

    override suspend fun readBytes(path: String): ByteArray {
        val resourceAsStream = this.javaClass.getResourceAsStream(path)
        if (resourceAsStream == null) {
            Config.getInstance().logger.log(LoggerSeverity.WARNING,"ResourceProvider.readBytes()",
                "Resource not found: $path")
            return byteArrayOf()
        }
        return withContext(Dispatchers.IO) {
            resourceAsStream.readAllBytes()
        }
    }

    override suspend fun initDrawablesAndFonts() {
        val directories = mutableListOf("drawables", "fonts")
        for (resDirectory in directories) {
            val fileList = resourceFileList[resDirectory]
            if (fileList != null) {
                for (file in fileList) {
                    if (file.isNotEmpty()) {
                        if (resDirectory == "drawables")
                            drawables[file] = readBytes("$resDirectory/$file")
                        else
                            fonts[file] = readBytes("$resDirectory/$file")
                    }
                }
            }
        }
    }

    override fun getDrawable(fileName: String): ByteArray {
        return drawables[fileName] ?: byteArrayOf()
    }

    override fun getFont(fileName: String): ByteArray {
        return fonts[fileName] ?: byteArrayOf()
    }

}