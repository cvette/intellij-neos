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

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.lang.xliff.XliffIcons
import de.vette.idea.neos.translation.TranslationId
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
            Role.PACKAGE -> result.withPrefixMatcher(prefix).addAllElements(packages(resolver, suffix = false))
            Role.SOURCE -> {
                val packageKey = argument.packageKey ?: return
                result.withPrefixMatcher(prefix).addAllElements(sources(resolver, packageKey, shorthand = false))
            }
            Role.ID -> if (argument.shorthandAllowed) {
                val segments = prefix.split(TranslationId.SEPARATOR, limit = 3)
                val matcher = result.withPrefixMatcher(segments.last())
                when (segments.size) {
                    1 -> matcher.addAllElements(packages(resolver, suffix = true))
                    2 -> matcher.addAllElements(sources(resolver, segments[0], shorthand = true))
                    else -> matcher.addAllElements(ids(resolver, segments[0], TranslationId.normalizeSource(segments[1])))
                }
            } else {
                val packageKey = argument.packageKey ?: return
                val source = argument.source ?: return
                result.withPrefixMatcher(prefix)
                    .addAllElements(ids(resolver, packageKey, TranslationId.normalizeSource(source)))
            }
        }

        // words of the file and other suggestions make no sense in these strings
        result.stopHere()
    }

    private fun packages(resolver: TranslationResolver, suffix: Boolean): List<LookupElement> =
        resolver.packages().map { packageKey ->
            LookupElementBuilder.create(packageKey)
                .withIcon(AllIcons.Nodes.Package)
                .withTypeText("package", true)
                .withSeparator(suffix)
                .asTranslationLookup()
        }

    private fun sources(resolver: TranslationResolver, packageKey: String, shorthand: Boolean): List<LookupElement> =
        resolver.sources(packageKey).map { source ->
            LookupElementBuilder.create(if (shorthand) source.replace('/', '.') else source)
                .withIcon(XliffIcons.XLIFF_FILE)
                .withTypeText(packageKey, true)
                .withSeparator(shorthand)
                .asTranslationLookup()
        }

    private fun ids(resolver: TranslationResolver, packageKey: String, source: String): List<LookupElement> =
        resolver.ids(packageKey, source).map { id ->
            val unit = resolver.findUnits(TranslationId(packageKey, source, id)).firstOrNull()
            LookupElementBuilder.create(id)
                .withIcon(AllIcons.Nodes.ResourceBundle)
                .withTailText(unit?.let { "  ${it.data.text}" }, true)
                .withTypeText(unit?.locale, true)
                .asTranslationLookup()
        }

    private fun LookupElementBuilder.withSeparator(enabled: Boolean): LookupElementBuilder =
        if (enabled) withInsertHandler(SEPARATOR_INSERT_HANDLER) else this

    private fun LookupElementBuilder.asTranslationLookup(): LookupElement =
        also { it.putUserData(TRANSLATION_LOOKUP, true) }

    /**
     * Packages, sources and ids contain dots, slashes and dashes. Without this filter typing one of them
     * would select the first suggestion instead of narrowing the list down.
     */
    class TranslationCharFilter : CharFilter() {
        override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup): Result? {
            if (c !in PREFIX_CHARS) return null

            val isTranslation = lookup.currentItem?.getUserData(TRANSLATION_LOOKUP) == true
            return if (isTranslation) Result.ADD_TO_PREFIX else null
        }
    }

    companion object {
        private val TRANSLATION_LOOKUP = Key.create<Boolean>("neos.translationLookup")
        private const val PREFIX_CHARS = "./-_"

        /** Adds the separator after a package or source and offers the next segment right away. */
        private val SEPARATOR_INSERT_HANDLER = InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            val tail = context.tailOffset
            if (tail >= document.textLength || document.charsSequence[tail] != TranslationId.SEPARATOR) {
                document.insertString(tail, TranslationId.SEPARATOR.toString())
            }
            context.editor.caretModel.moveToOffset(tail + 1)
            AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
        }
    }
}
