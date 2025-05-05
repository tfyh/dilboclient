package org.dilbo.dilboclient.tfyh.util

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class LocalCache {

    actual companion object {
        private var localCache = LocalCache()
        actual fun getInstance() = localCache
    }

    private var items: MutableMap<String, String> = mutableMapOf()

    actual fun getItem(key: String) = items[key] ?: ""
    actual fun setItem(key: String, value: String) = items.set(key, value)
    actual fun removeItem(key: String) { items.remove(key) }
    actual fun clear() { items = mutableMapOf() }
    actual fun keys() = items.keys
    actual fun init(): String { return "android file location tbd." }
}