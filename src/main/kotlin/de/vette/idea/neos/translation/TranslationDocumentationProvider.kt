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

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag

/**
 * Shows the text of a label in all locales, both where the label is used and on the id of a translation unit.
 */
class TranslationDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (contextElement == null || DumbService.isDumb(file.project)) return null

        unitIdValue(contextElement)?.let { return it }

        // a label resolves to one unit per locale, the platform shows documentation for a single target only
        var element: PsiElement? = contextElement
        repeat(MAX_REFERENCE_DEPTH) {
            val current = element ?: return null
            val reference = current.references.filterIsInstance<TranslationUnitReference>().firstOrNull()
            if (reference != null) {
                return reference.multiResolve(false).firstOrNull()?.element
            }
            element = current.parent
        }
        return null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val id = translationId(element) ?: return null
        val project = element!!.project
        if (DumbService.isDumb(project)) return null

        val resolver = TranslationResolver.getInstance(project)
        val units = resolver.findUnits(id)
        val locales = LinkedHashSet(resolver.locales()).apply { addAll(units.map { it.locale }) }

        val html = StringBuilder()
        html.append(DocumentationMarkup.DEFINITION_START)
        html.append(StringUtil.escapeXmlEntities(id.toShorthand()))
        html.append(DocumentationMarkup.DEFINITION_END)
        html.append(DocumentationMarkup.SECTIONS_START)
        for (locale in locales) {
            val texts = units.filter { it.locale == locale }
            html.append(DocumentationMarkup.SECTION_HEADER_START)
            html.append(StringUtil.escapeXmlEntities(locale)).append(":")
            html.append(DocumentationMarkup.SECTION_SEPARATOR)
            if (texts.isEmpty()) {
                html.append(DocumentationMarkup.GRAYED_START).append("missing").append(DocumentationMarkup.GRAYED_END)
            } else {
                html.append(texts.joinToString("<br>") { unit ->
                    StringUtil.escapeXmlEntities(unit.data.text) + if (unit.data.plural) PLURAL_HINT else ""
                })
            }
            html.append(DocumentationMarkup.SECTION_END)
        }
        html.append(DocumentationMarkup.SECTIONS_END)
        return html.toString()
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val id = translationId(element) ?: return null
        val project = element!!.project
        if (DumbService.isDumb(project)) return null

        val text = TranslationResolver.getInstance(project).findUnits(id).firstOrNull()?.data?.text
        return StringUtil.escapeXmlEntities(id.toShorthand()) +
                (text?.let { "<br>" + StringUtil.escapeXmlEntities(it) } ?: "")
    }

    private fun translationId(element: PsiElement?): TranslationId? {
        val tag = when (element) {
            is XmlTag -> element
            is XmlAttributeValue -> unitIdValue(element)?.let { PsiTreeUtil.getParentOfType(it, XmlTag::class.java) }
            else -> null
        } ?: return null

        return XliffIndexing.translationIdOf(tag)
    }

    /** The value of the `id` attribute of a `<trans-unit>` the element is part of. */
    private fun unitIdValue(element: PsiElement): XmlAttributeValue? {
        val value = PsiTreeUtil.getParentOfType(element, XmlAttributeValue::class.java, false) ?: return null
        val attribute = value.parent as? XmlAttribute ?: return null
        return value.takeIf { attribute.name == "id" && attribute.parent?.localName == "trans-unit" }
    }

    companion object {
        private const val MAX_REFERENCE_DEPTH = 3
        private const val PLURAL_HINT = " <i>(plural)</i>"
    }
}
