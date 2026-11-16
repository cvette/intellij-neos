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

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.Key
import de.vette.idea.neos.lang.xliff.XliffIcons

/**
 * The completion items for packages, sources and ids of translations, shared by all languages labels are used in.
 */
object TranslationLookups {
    private val TRANSLATION_LOOKUP = Key.create<Boolean>("neos.translationLookup")

    /**
     * Completes the segment of `Package:Source:id` the caret is in.
     *
     * @param prefix the text of the shorthand before the caret
     */
    fun addShorthand(resolver: TranslationResolver, prefix: String, result: CompletionResultSet) {
        val segments = prefix.split(TranslationId.SEPARATOR, limit = 3)
        val matcher = result.withPrefixMatcher(segments.last())
        when (segments.size) {
            1 -> matcher.addAllElements(packages(resolver, shorthand = true))
            2 -> matcher.addAllElements(sources(resolver, segments[0], shorthand = true))
            else -> matcher.addAllElements(ids(resolver, segments[0], TranslationId.normalizeSource(segments[1])))
        }
    }

    /**
     * @param shorthand true if the package is a segment of a shorthand id: the separator is added after it
     */
    fun packages(resolver: TranslationResolver, shorthand: Boolean = false): List<LookupElement> =
        resolver.packages().map { packageKey ->
            LookupElementBuilder.create(packageKey)
                .withIcon(AllIcons.Nodes.Package)
                .withTypeText("package", true)
                .withSeparator(shorthand)
                .asTranslationLookup()
        }

    /**
     * @param shorthand true if the source is a segment of a shorthand id: it is written with dots instead of
     *                  slashes and the separator is added after it
     */
    fun sources(resolver: TranslationResolver, packageKey: String, shorthand: Boolean = false): List<LookupElement> =
        resolver.sources(packageKey).map { source ->
            LookupElementBuilder.create(if (shorthand) source.replace('/', '.') else source)
                .withIcon(XliffIcons.XLIFF_FILE)
                .withTypeText(packageKey, true)
                .withSeparator(shorthand)
                .asTranslationLookup()
        }

    /**
     * @param source the source in path notation
     */
    fun ids(resolver: TranslationResolver, packageKey: String, source: String): List<LookupElement> =
        resolver.ids(packageKey, source).map { id ->
            val unit = resolver.findUnits(TranslationId(packageKey, source, id)).firstOrNull()
            LookupElementBuilder.create(id)
                .withIcon(AllIcons.Nodes.ResourceBundle)
                .withTailText(unit?.let { "  ${it.data.text}" }, true)
                .withTypeText(unit?.locale, true)
                .asTranslationLookup()
        }

    fun isTranslationLookup(element: LookupElement?): Boolean = element?.getUserData(TRANSLATION_LOOKUP) == true

    private fun LookupElementBuilder.withSeparator(enabled: Boolean): LookupElementBuilder =
        if (enabled) withInsertHandler(SEPARATOR_INSERT_HANDLER) else this

    private fun LookupElementBuilder.asTranslationLookup(): LookupElement =
        also { it.putUserData(TRANSLATION_LOOKUP, true) }

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
