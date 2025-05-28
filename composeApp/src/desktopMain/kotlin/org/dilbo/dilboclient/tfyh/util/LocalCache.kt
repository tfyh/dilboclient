package org.dilbo.dilboclient.tfyh.util

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URI
import java.nio.charset.StandardCharsets

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class LocalCache {

    actual companion object {
        private var localCache = LocalCache()
        actual fun getInstance() = localCache
    }

    actual fun getItem(key: String) =
        JavaFileAccess.getContents(lsPath + File.separator + key) ?: ""
    actual fun setItem(key: String, value: String) =
        JavaFileAccess.setContents(lsPath + File.separator + key, value)
    actual fun removeItem(key: String) =
        JavaFileAccess.delete(lsPath + File.separator + key)
    actual fun clear() =
        JavaFileAccess.deleteAllFiles(lsPath)
    actual fun keys() =
        JavaFileAccess.fileNames(lsPath).toMutableSet()

    private val jarPath = File(
        LocalCache::class.java.protectionDomain?.codeSource?.location?.toURI() ?: URI(".")
    ).path
    private var lsPath = jarPath.substring(0, jarPath.lastIndexOf(File.separator)) + File.separator + "localStorage"

    actual fun init(): String {
        if (!JavaFileAccess.dirExists(lsPath)) {
            var created = JavaFileAccess.mkDirs(lsPath)
            if (!created) {
                val userHome = System.getProperty("user.home")
                lsPath = userHome + File.separator + "dilboFiles"
                created = JavaFileAccess.mkDirs(lsPath)
                if (!created)
                    return "Could not create local storage directory = $lsPath"
            }
        }
        return "locale storage directory = $lsPath"
    }

    /**
     * Java io file access
     */
    object JavaFileAccess {
        /**
         * Check whether a specific directory exists
         */
        fun dirExists(path: String): Boolean {
            return (File(path).exists()) && (File(path).isDirectory)
        }

        /**
         * Creates a directory path in the file system
         */
        fun mkDirs(path: String): Boolean {
            return File(path).mkdirs()
        }

        /**
         * Creates a directory path in the file system
         */
        fun delete(path: String) {
            File(path).delete()
        }

        /**
         * Removes all dirs and files recursively from the
         */
        fun deleteAllFiles(path: String) {
            val files = File(path).listFiles { file -> !file.isDirectory }
            if (files != null)
                for (file in files)
                    file.delete()
            val dirs = File(path).listFiles { file -> file.isDirectory }
            if (dirs != null)
                for (dir in dirs) {
                    deleteAllFiles(dir.path)
                    dir.delete()
                }
        }

        /**
         * Get all relative file paths. Set root must not have a trailing file separator.
         */
        fun fileNames(root: String, path: String = root): Array<String> {
            val filePaths = mutableListOf<String>()
            val relPath = if (path == root) "" else path.substring(root.length + 1)
            val files = File(path).listFiles { file -> !file.isDirectory }
            if (files != null)
                for (file in files) { filePaths += relPath + File.separator + file.name }
            val dirs = File(path).listFiles { file -> file.isDirectory }
            if (dirs != null)
                for (dir in dirs) { filePaths.addAll(fileNames(root, dir.path)) }
            return filePaths.toTypedArray()
        }

        /**
         * Fetch the entire contents of a text file, and return it in a String. This
         * style of implementation does not throw Exceptions to the caller, but
         * returns a null String, when hitting an IOException.
         * Snippet from [java practices](http://www.javapractices.com/topic/TopicAction.do?Id=42)
         * modified. Returns false, if not successful.
         */
        fun getContents(path: String): String? {
            val aFile = File(path)
            if (!aFile.exists()) return ""
            // ...checks on aFile are omitted
            val contents = StringBuilder()
            try {
                // use buffering, reading one line at a time
                // FileReader always assumes default encoding is OK!
                BufferedReader(
                    InputStreamReader(
                        FileInputStream(aFile), StandardCharsets.UTF_8
                    )
                ).use { input ->
                    var line: String? // not declared within while loop
                    /* readLine is a bit quirky : it returns the content of a line
                     * MINUS the newline. it returns null only for the END of the
                     * stream. it returns an empty String if two newlines appear in
                     * a row.
                     */
                    while ((input.readLine().also { line = it }) != null) {
                        contents.append("\n")
                        contents.append(line)
                    }
                }
            } catch (ex: IOException) {
                return null
            }
            return if (contents.isEmpty()) "" else contents.toString().substring(1)
        }

        /**
         * Change the contents of text file in its entirety, overwriting any
         * existing text.
         *
         * Snippet from [java practices](http://www.javapractices.com/topic/TopicAction.do?Id=42)
         * modified. Returns false, if not successful.
         *
         * @param path path to the text file which is File to be read
         * @param aContents the String contents to be written
         * @return true on success
         */
        fun setContents(path: String, aContents: String) {
            val aFile = File(path)
            if (aFile.parent == null)
                return
            if (!aFile.exists()) {
                val p = File(aFile.parent)
                if (!p.exists())
                    p.mkdirs()
            }
            // use buffering
            try {
                val output = BufferedWriter(
                    OutputStreamWriter(
                        FileOutputStream(aFile), StandardCharsets.UTF_8
                    )
                )
                output.write(aContents)
                output.close()
            } catch (ignored: IOException) {
            }
        }
    }
}