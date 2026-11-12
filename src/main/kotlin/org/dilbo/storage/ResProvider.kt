package org.dilbo.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfyh.util.ResProviderIF
import kotlin.collections.set

class ResProvider private constructor(override val directories: List<String>): ResProviderIF {

    companion object {
        private val resDirectories: List<String> = listOf("css", "drawable", "files", "font")
        val instance = ResProvider(resDirectories)
        fun getSingleton() = instance
    }

    val resources = mutableMapOf<String, ByteArray>()

    override suspend fun readBytes(path: String): ByteArray {
        val resourceAsStream = this.javaClass.getResourceAsStream(path) ?: return byteArrayOf()
        return withContext(Dispatchers.IO) {
            resourceAsStream.readAllBytes() ?: byteArrayOf()
        }
    }

    suspend fun readResourceDir(path: String) {
        val resourceAsStream = this.javaClass.getResourceAsStream(path)
        if (resourceAsStream != null) {
            withContext(Dispatchers.IO) {
                resources[path] = resourceAsStream.readAllBytes()
                val resourceNames = (resources[path]?.toString(Charsets.UTF_8) ?: "").split("\n")
                // if the resource was a String, all lines of that string will be tried to be read
                // therefore filter criteria are applied
                val first = resourceNames[0]
                if ((first.length > 1) && // configuration file names must be longer than 1 character
                    (first.indexOf(";") < 0) && // typical for csv files
                    (first.indexOf("/") < 0) && // comment lines
                    (first.indexOf("#") < 0) && // comment lines
                    (first.indexOf(" ") < 0)) // configuration file names must not contain spaces
                    resourceNames.forEach { name -> if (name.isNotEmpty()) readResourceDir("$path/$name") }

            }
        }
    }

    /**
     * Initialise the resources of all types.
     */
    override suspend fun init() {
        directories.forEach { readResourceDir(it) }
    }

    override fun getBytes(path: String): ByteArray {
        return resources[path] ?: byteArrayOf()
    }

    override fun getString(path: String): String {
        return resources[path]?.toString(Charsets.UTF_8) ?: ""
    }
}