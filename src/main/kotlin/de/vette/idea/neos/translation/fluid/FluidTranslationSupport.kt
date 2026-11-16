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

package de.vette.idea.neos.translation.fluid

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.util.ProcessingContext
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLookups
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationRole
import de.vette.idea.neos.translation.translationArgumentReferences
import de.vette.idea.neos.util.ComposerUtil

/**
 * Recognizes the translate view helpers in Fluid templates:
 *
 * - the tag `<f:translate id="..." source="..." package="..."/>`, also `neos:backend.translate`
 * - the inline notation `{f:translate(id: '...', source: '...', package: '...')}`
 *
 * Without a package the labels are looked up in the package of the template.
 */
object FluidTranslationUsage {
    private const val RESOURCES = "/Resources/Private/"
    private const val ID = "id"
    private const val SOURCE = "source"
    private const val PACKAGE = "package"

    private val INLINE = Regex("""[\w.]+:(?:backend\.)?translate\(([^)]*)\)""")
    private val INLINE_ARGUMENT = Regex("""(\w+)\s*:\s*(['"])(.*?)\2""")

    /** Fluid templates are HTML files in the private resources of a package */
    @JvmStatic
    fun isTemplate(file: PsiFile?): Boolean {
        val virtualFile = file?.originalFile?.virtualFile ?: return false
        return virtualFile.extension.equals("html", ignoreCase = true) &&
                virtualFile.path.contains(RESOURCES) && NeosProjectService.isEnabled(file)
    }

    @JvmStatic
    fun references(element: PsiElement): Array<PsiReference> {
        if (!isTemplate(element.containingFile)) return PsiReference.EMPTY_ARRAY

        val references = when (element) {
            is XmlAttributeValue -> tagAttributeReferences(element) + inlineReferences(element, element.text, 0)
            // texts do not take references of their own, so the tag around them holds the references
            is XmlTag -> element.children.filterIsInstance<XmlText>()
                .flatMap { inlineReferences(element, it.text, it.startOffsetInParent) }
            else -> emptyList()
        }
        return references.toTypedArray()
    }

    /** The role of the value of an attribute of the translate tag */
    @JvmStatic
    fun tagArgument(value: XmlAttributeValue): FluidTranslationArgument? {
        val attribute = value.parent as? XmlAttribute ?: return null
        val tag = attribute.parent ?: return null
        if (!isTranslateTag(tag.name)) return null

        val role = role(attribute.name) ?: return null
        return FluidTranslationArgument(
            role,
            packageKey = constant(tag.getAttributeValue(PACKAGE), packageOf(value.containingFile)),
            source = constant(tag.getAttributeValue(SOURCE), TranslationId.DEFAULT_SOURCE),
        )
    }

    private fun tagAttributeReferences(value: XmlAttributeValue): List<PsiReference> {
        val argument = tagArgument(value) ?: return emptyList()
        if (value.value.contains('{')) return emptyList()

        val range = ElementManipulators.getValueTextRange(value)
        return translationArgumentReferences(value, range, argument.role, argument.packageKey, argument.source).toList()
    }

    /**
     * View helper calls inside of a text or an attribute value
     *
     * @param text the text to look at, a part of the element
     * @param offset where the text starts in the element
     */
    private fun inlineReferences(element: PsiElement, text: String, offset: Int): List<PsiReference> {
        if (!text.contains("translate(")) return emptyList()

        val references = ArrayList<PsiReference>()
        for (call in INLINE.findAll(text)) {
            val argumentsStart = offset + call.groups[1]!!.range.first
            val arguments = INLINE_ARGUMENT.findAll(call.groupValues[1]).associate { match ->
                val valueGroup = match.groups[3]!!
                match.groupValues[1] to (valueGroup.value to TextRange(
                    argumentsStart + valueGroup.range.first, argumentsStart + valueGroup.range.last + 1
                ))
            }

            val packageKey = arguments[PACKAGE]?.first ?: packageOf(element.containingFile)
            val source = arguments[SOURCE]?.first ?: TranslationId.DEFAULT_SOURCE
            for ((name, valueAndRange) in arguments) {
                val role = role(name) ?: continue
                references += translationArgumentReferences(element, valueAndRange.second, role, packageKey, source)
            }
        }
        return references
    }

    private fun isTranslateTag(name: String): Boolean = name.endsWith(":translate") || name.endsWith(":backend.translate")

    private fun role(attribute: String): TranslationRole? = when (attribute) {
        ID -> TranslationRole.ID
        SOURCE -> TranslationRole.SOURCE
        PACKAGE -> TranslationRole.PACKAGE
        else -> null
    }

    /** The value of an attribute: the default if it is omitted, null if it contains an expression */
    private fun constant(value: String?, default: String?): String? = when {
        value.isNullOrBlank() -> default
        value.contains('{') -> null
        else -> value
    }

    /** The key of the package the template is part of */
    private fun packageOf(file: PsiFile): String? {
        val original = file.originalFile
        original.containingDirectory?.let(ComposerUtil::getComposerManifest)?.let(ComposerUtil::getPackageKey)
            ?.takeIf { it.isNotBlank() }?.let { return it }

        val path = original.virtualFile?.path ?: return null
        val resources = path.lastIndexOf(RESOURCES)
        return if (resources > 0) path.substring(0, resources).substringAfterLast('/') else null
    }
}

class FluidTranslationArgument(val role: TranslationRole, val packageKey: String?, val source: String?)

class FluidTranslationReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val provider = object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
                FluidTranslationUsage.references(element)
        }
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlAttributeValue::class.java), provider)
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlTag::class.java), provider)
    }
}

/**
 * Completes ids, sources and packages in the attributes of the translate tag.
 */
class FluidTranslationCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet,
            ) {
                val value = PsiTreeUtil.getParentOfType(parameters.position, XmlAttributeValue::class.java, false) ?: return
                val project = value.project
                if (DumbService.isDumb(project) || !FluidTranslationUsage.isTemplate(parameters.originalFile)) return
                val argument = FluidTranslationUsage.tagArgument(value) ?: return

                val valueStart = value.textRange.startOffset + ElementManipulators.getValueTextRange(value).startOffset
                val content = value.value
                val prefix = content.take((parameters.offset - valueStart).coerceIn(0, content.length))
                val resolver = TranslationResolver.getInstance(project)

                val elements = when (argument.role) {
                    TranslationRole.PACKAGE -> TranslationLookups.packages(resolver)
                    TranslationRole.SOURCE -> TranslationLookups.sources(resolver, argument.packageKey ?: return)
                    TranslationRole.ID -> TranslationLookups.ids(
                        resolver,
                        argument.packageKey ?: return,
                        TranslationId.normalizeSource(argument.source ?: return)
                    )
                }
                result.withPrefixMatcher(prefix).addAllElements(elements)
            }
        })
    }
}
