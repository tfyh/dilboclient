/**
 *
 *       dilbo - digital logbook for Rowing and Canoeing
 *       -----------------------------------------------
 *       https://www.dilbo.org
 *
 * Copyright  2023-2024  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dilbo.app

import org.tfyh.data.LocalCacheIF

class LocalCache private constructor() : LocalCacheIF {
    val items = mutableMapOf<String, String>()
    companion object {
        private val instance: LocalCache = LocalCache()
        fun getInstance() = instance
    }
    override fun getItem(key: String) : String = items[key] ?: ""
    override fun setItem(key: String, value: String) { items[key] = value}
    override fun removeItem(key: String) { items.remove(key) }
    override fun clear() { items.clear() }
    override fun keys(): MutableSet<String> = items.keys
    override fun init(): String = "java file location tbd."
    override fun localhostUrl(): String {
        // this replaces the localhost for emulators
        // TODO("Not yet (or better: no more) implemented")
        return "http://10.0.2.2"
    }

}