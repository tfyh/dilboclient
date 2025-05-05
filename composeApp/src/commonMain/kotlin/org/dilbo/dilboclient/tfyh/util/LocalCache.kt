package org.dilbo.dilboclient.tfyh.util

/**
 * The local storage provides a container for String items like the JavaScript window.localStorage,
 * but provides no persistence in Web and mobile.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class LocalCache private constructor() {
    companion object {
        fun getInstance(): LocalCache
    }
    fun getItem(key: String): String
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
    fun clear()
    fun keys(): MutableSet<String>
    fun init(): String
}