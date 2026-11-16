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

import com.intellij.openapi.vfs.VirtualFile

/**
 * What the path of an XLIFF file tells about its content:
 * `<package>/Resources/Private/Translations/<locale>/<source>.xlf`.
 *
 * The package key and the source are only defaults, the `product-name` and `original` attributes of a
 * `<file>` element take precedence (see [XliffIndexing]). The locale always comes from the path.
 */
data class XliffLocation(val packageKey: String, val locale: String, val source: String) {

    companion object {
        const val TRANSLATIONS_PATH = "/Resources/Private/Translations/"
        private val EXTENSIONS = setOf("xlf", "xliff")

        @JvmStatic
        fun fromFile(file: VirtualFile): XliffLocation? = fromPath(file.path)

        @JvmStatic
        fun fromPath(path: String): XliffLocation? {
            val translationsStart = path.lastIndexOf(TRANSLATIONS_PATH)
            if (translationsStart <= 0) return null

            val packageKey = path.substring(0, translationsStart).substringAfterLast('/')
            val relativePath = path.substring(translationsStart + TRANSLATIONS_PATH.length)
            val localeEnd = relativePath.indexOf('/')
            if (packageKey.isEmpty() || localeEnd <= 0) return null

            val locale = relativePath.substring(0, localeEnd)
            val sourceFile = relativePath.substring(localeEnd + 1)
            if (sourceFile.substringAfterLast('.', "").lowercase() !in EXTENSIONS) return null

            val source = sourceFile.substringBeforeLast('.').trim('/')
            if (source.isEmpty()) return null

            return XliffLocation(packageKey, locale, source)
        }

        @JvmStatic
        fun isTranslationFile(file: VirtualFile): Boolean =
            file.extension?.lowercase() in EXTENSIONS && file.path.contains(TRANSLATIONS_PATH)
    }
}
