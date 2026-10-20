package org.dilbo.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfyh.util.ResProviderIF
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ResProvider private constructor(): ResProviderIF {

    companion object {
        val instance = ResProvider()
        fun getSingle() = instance
    }

    val resources = mutableMapOf<ResProviderIF.Type, MutableMap<String, ByteArray>>()

    override suspend fun readBytes(path: String): ByteArray {
        return withContext(Dispatchers.IO) {
             this.javaClass.getResourceAsStream(path)?.readAllBytes() ?: byteArrayOf()
        }
    }

    fun getResourceFiles(path: String): List<String> = getResourceAsStream(path).use {
        return if (it == null) emptyList()
        else BufferedReader(InputStreamReader(it)).readLines()
    }

    private fun getResourceAsStream(resource: String): InputStream? =
        Thread.currentThread().contextClassLoader.getResourceAsStream(resource)

    /**
     * Initialise the resources of all types.
     */
    suspend fun init() {
        ResProviderIF.Type.entries.forEach { init(it) }
    }
    /**
     * Initialise the resources of a given type.
     */
    override suspend fun init(type: ResProviderIF.Type) {
        val path = type.toString().lowercase() + "s"
        val drawableNames = getResourceFiles(path)
        val resources = mutableMapOf<String, ByteArray>()
        drawableNames.forEach { name ->
            resources[name] = readBytes("$path/$name")
        }
        this.resources[type] = resources
    }

    override fun getDrawable(name: String): ByteArray {
        return resources[ResProviderIF.Type.DRAWABLE]?.get("$name.png") ?: byteArrayOf()
    }

    override fun getString(name: String): String {
        return resources[ResProviderIF.Type.STRING]?.get(name)?.toString(Charsets.UTF_8) ?: ""
    }
}