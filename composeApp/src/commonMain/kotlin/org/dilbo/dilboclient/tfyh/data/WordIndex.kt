package org.dilbo.dilboclient.tfyh.data

class WordIndex {

    companion object {
        /**
         * In order to separate the words and create an index of the text we need to parse the text, separate the
         * words and transcribe the characters to ASCII to avoid sorting and misspelling problems. Characters which
         * are not listed are used as in-word characters and replaced by an underscore '_'.
         */
        private val charBlocks: Map<String, String> = mapOf("stay" to "abcdefghijklmnopqrstuvwxyz0123456789_-",
        // characters which stay one character
        "from_1" to "ABCDEFGHIJKLMNOPQRSTUVWXYZÁÀÂÄÇÉÈÊËÍÌÎÏÑÓÒÔÚÙÛŸáàâçéèêëíìîïñóòôúùûÿ",
        "to_1" to "abcdefghijklmnopqrstuvwxyzaaaaceeeeiiiinooouuuyaaaceeeeiiiinooouuuy",
        // special characters which become two characters
        "from_2" to "ÅÄÆØÖŒÜåäæøöœüß", "to_2" to "AaAeAeOeOeOeUeaaaeaeoeoeoeuess",
        // word separator characters
        "separate" to " ',;.:#+*/=§$%&@€|<>(){}[]?!`\"\u000c\n\r\t\u000b\\" // \u000b = vertical tab = \v; \u000c = \f
        )

        /**
         * Transform a word into its ASCII representation by replacing special characters like ä, é with ae, e and
         * putting the whole word into lower case.
         */
        fun toLowerAscii(word: String): String {
            val len = word.length
            var wordAscii = ""
            for (i in 0..< len) {
                val c = word.substring(i, i + 1)
                // check character. THose which need no handling first.
                if ((charBlocks["stay"]?.indexOf(c) ?: -1) >= 0)
                    wordAscii += c
                else {
                    // check character. Special characters which are extended to two characters
                    val posFrom2 = charBlocks["from_2"]?.indexOf(c) ?: -1
                    if (posFrom2 >= 0)
                        wordAscii += charBlocks["to_2"]?.substring(posFrom2 * 2, posFrom2 * 2 + 2) ?: ""
                    else {
                        // check character. Special characters which are replaced by a single character
                        val posFrom1 = charBlocks["from_1"]?.indexOf(c) ?: -1
                        if (posFrom1 >= 0)
                            wordAscii += charBlocks["to_1"]?.substring(posFrom1, posFrom1 + 1) ?: ""
                        else
                        // not in the characters list of characters with known handling. replace by
                        // underscore.
                            wordAscii += "_"
                    }
                }
            }
            return wordAscii
        }
    }

    // TODO word index functions await implementation...
}