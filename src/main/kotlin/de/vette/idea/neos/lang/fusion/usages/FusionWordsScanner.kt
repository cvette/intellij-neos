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

package de.vette.idea.neos.lang.fusion.usages

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.VersionedWordsScanner
import com.intellij.lang.cacheBuilder.WordOccurrence
import com.intellij.psi.tree.TokenSet
import com.intellij.util.Processor
import de.vette.idea.neos.lang.fusion.FusionLexerAdapter
import de.vette.idea.neos.lang.fusion.psi.FusionTypes

/**
 * Feeds the word index with the words of Fusion files.
 *
 * Prototype names and comments are reported with their kind. In addition every word of the file is reported
 * without a kind, so that the text inside of Eel expressions, strings and AFX can be found through the index
 * as well, e.g. the ids of translation units.
 */
class FusionWordsScanner : VersionedWordsScanner() {

    private val tokens = DefaultWordsScanner(
        FusionLexerAdapter(),
        TokenSet.create(FusionTypes.TYPE),
        TokenSet.create(FusionTypes.SINGLE_LINE_COMMENT, FusionTypes.DOC_COMMENT),
        TokenSet.EMPTY
    )

    override fun getVersion(): Int = VERSION

    override fun processWords(fileText: CharSequence, processor: Processor<in WordOccurrence>) {
        tokens.processWords(fileText, processor)

        // the occurrence is reused, like the scanners of the platform do it
        val occurrence = WordOccurrence(fileText, 0, 0, null)
        var index = 0
        val length = fileText.length
        while (index < length) {
            while (index < length && !Character.isJavaIdentifierPart(fileText[index])) index++
            val start = index
            while (index < length && Character.isJavaIdentifierPart(fileText[index])) index++
            if (index > start) {
                occurrence.init(fileText, start, index, null)
                if (!processor.process(occurrence)) return
            }
        }
    }

    companion object {
        private const val VERSION = 1
    }
}
