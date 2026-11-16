/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.translation

import com.intellij.openapi.util.TextRange

/**
 * Identifies a label the way Flow does: by package key, source (the path of the XLIFF file below the
 * locale directory, without extension) and the id of the translation unit.
 */
data class TranslationId(val packageKey: String, val source: String, val id: String) {

    /** Key of the XLIFF source this label lives in, see [TranslationSourceIndex]. */
    val sourceKey: String
        get() = sourceKey(packageKey, source)

    /** Key of the label, see [TranslationUnitIndex]. */
    val key: String
        get() = "$sourceKey$SEPARATOR$id"

    /** The notation used in Fusion and YAML, e.g. `Acme.Site:NodeTypes.Page:properties.title`. */
    fun toShorthand(): String = "$packageKey$SEPARATOR${source.replace('/', '.')}$SEPARATOR$id"

    companion object {
        const val DEFAULT_SOURCE = "Main"
        const val SEPARATOR = ':'

        private val PACKAGE_KEY = Regex("[A-Za-z0-9]+(\\.[A-Za-z0-9]+)+")
        private val SOURCE = Regex("[A-Za-z0-9._/-]+")

        @JvmStatic
        fun of(packageKey: String, source: String?, id: String): TranslationId =
            TranslationId(packageKey, normalizeSource(source), id)

        @JvmStatic
        fun sourceKey(packageKey: String, source: String): String = "$packageKey$SEPARATOR$source"

        /**
         * The shorthand notation separates the path segments of the source with dots, Flow replaces them by
         * slashes before it looks up the file. An empty source falls back to "Main".
         */
        @JvmStatic
        fun normalizeSource(source: String?): String {
            val trimmed = source?.trim('/', ' ').orEmpty()
            return if (trimmed.isEmpty()) DEFAULT_SOURCE else trimmed.replace('.', '/')
        }

        /**
         * Parses `Package.Key:Source.Path:id`. Returns null if the text is not a shorthand translation id.
         */
        @JvmStatic
        fun parseShorthand(text: String): ShorthandTranslationId? {
            val firstSeparator = text.indexOf(SEPARATOR)
            if (firstSeparator <= 0) return null
            val secondSeparator = text.indexOf(SEPARATOR, firstSeparator + 1)
            if (secondSeparator < 0) return null

            val packageRange = TextRange(0, firstSeparator)
            val sourceRange = TextRange(firstSeparator + 1, secondSeparator)
            val idRange = TextRange(secondSeparator + 1, text.length)

            val packageKey = packageRange.substring(text)
            val source = sourceRange.substring(text)
            val id = idRange.substring(text)
            if (!PACKAGE_KEY.matches(packageKey) || !SOURCE.matches(source) || id.isBlank()) return null

            return ShorthandTranslationId(of(packageKey, source, id), packageRange, sourceRange, idRange)
        }
    }
}

/**
 * A [TranslationId] written in shorthand notation, with the ranges of its segments relative to the parsed text.
 */
data class ShorthandTranslationId(
    val translationId: TranslationId,
    val packageRange: TextRange,
    val sourceRange: TextRange,
    val idRange: TextRange,
)
