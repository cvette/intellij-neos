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

package de.vette.idea.neos.translation.eel

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.project.DumbService
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLookups
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.eel.EelTranslationArgument.Role

/**
 * Completes packages, sources and ids in the string arguments of the translation helper.
 * In shorthand notation each segment is completed on its own.
 */
class EelTranslationCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val string = PsiTreeUtil.getParentOfType(parameters.position, EelValueStringLine::class.java, false) ?: return
        val project = string.project
        if (DumbService.isDumb(project)) return

        val argument = EelTranslationUsage.forString(string) ?: return
        val resolver = TranslationResolver.getInstance(project)

        val contentStart = string.textRange.startOffset + EelTranslationUsage.contentRange(string).startOffset
        val content = EelTranslationUsage.content(string)
        val prefix = content.take((parameters.offset - contentStart).coerceIn(0, content.length))

        when (argument.role) {
            Role.PACKAGE -> result.withPrefixMatcher(prefix).addAllElements(TranslationLookups.packages(resolver))
            Role.SOURCE -> {
                val packageKey = argument.packageKey ?: return
                result.withPrefixMatcher(prefix).addAllElements(TranslationLookups.sources(resolver, packageKey))
            }
            Role.ID -> if (argument.shorthandAllowed) {
                TranslationLookups.addShorthand(resolver, prefix, result)
            } else {
                val packageKey = argument.packageKey ?: return
                val source = argument.source ?: return
                result.withPrefixMatcher(prefix)
                    .addAllElements(TranslationLookups.ids(resolver, packageKey, TranslationId.normalizeSource(source)))
            }
        }

        // words of the file and other suggestions make no sense in these strings
        result.stopHere()
    }

    /**
     * Packages, sources and ids contain dots, slashes and dashes. Without this filter typing one of them
     * would select the first suggestion instead of narrowing the list down.
     */
    class TranslationCharFilter : CharFilter() {
        override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup): Result? {
            if (c !in PREFIX_CHARS) return null

            return if (TranslationLookups.isTranslationLookup(lookup.currentItem)) Result.ADD_TO_PREFIX else null
        }
    }

    companion object {
        private const val PREFIX_CHARS = "./-_"
    }
}
