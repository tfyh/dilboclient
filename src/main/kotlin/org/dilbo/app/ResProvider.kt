package org.dilbo.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfyh.util.ResProviderIF

class ResProvider: ResProviderIF {
    override suspend fun readBytes(path: String): ByteArray {
        return withContext(Dispatchers.IO) {
             this.javaClass.getResourceAsStream(path)?.readAllBytes() ?: byteArrayOf()
        }
    }
}