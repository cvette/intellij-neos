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

    private val INLINE_START = Regex("""[\w.]+:(?:backend\.)?translate\(""")

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
        val references = ArrayList<PsiReference>()
        for (arguments in inlineCalls(text)) {
            val packageKey = constant(arguments[PACKAGE]?.value, packageOf(element.containingFile))
            val source = constant(arguments[SOURCE]?.value, TranslationId.DEFAULT_SOURCE)
            for ((name, argument) in arguments) {
                // a value with an expression in it is not a constant id
                if (argument.value.contains('{')) continue
                val role = role(name) ?: continue
                references += translationArgumentReferences(element, argument.range.shiftRight(offset), role, packageKey, source)
            }
        }
        return references
    }

    /**
     * The argument of an inline view helper call the offset is in, for completion
     *
     * @param text the text of the template
     */
    @JvmStatic
    fun inlineArgumentAt(file: PsiFile, text: String, offset: Int): Pair<FluidTranslationArgument, InlineArgument>? {
        for (arguments in inlineCalls(text)) {
            val (name, argument) = arguments.entries.firstOrNull { it.value.range.containsOffset(offset) } ?: continue
            val role = role(name) ?: return null
            return FluidTranslationArgument(
                role,
                packageKey = constant(arguments[PACKAGE]?.value, packageOf(file)),
                source = constant(arguments[SOURCE]?.value, TranslationId.DEFAULT_SOURCE),
            ) to argument
        }
        return null
    }

    /**
     * The string arguments of the calls of the translate view helper in inline notation, by their name.
     * The ranges are the ones of the values without quotes, relative to the text.
     */
    @JvmStatic
    fun inlineCalls(text: String): List<Map<String, InlineArgument>> {
        if (!text.contains("translate(")) return emptyList()

        return INLINE_START.findAll(text).map { inlineArguments(text, it.range.last + 1) }.toList()
    }

    /**
     * Reads `name: 'value'` pairs up to the parenthesis that closes the call. Values that are not strings are
     * skipped, with the arrays, calls and strings nested in them: `arguments: {0: item.name, 1: '(new)'}`.
     */
    private fun inlineArguments(text: String, start: Int): Map<String, InlineArgument> {
        val arguments = LinkedHashMap<String, InlineArgument>()
        var index = start
        while (index < text.length) {
            index = skipWhitespace(text, index)
            if (index >= text.length || text[index] == ')') break

            // the name of the argument and the colon
            val nameStart = index
            while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '_')) index++
            val name = text.substring(nameStart, index)
            index = skipWhitespace(text, index)
            if (name.isNotEmpty() && index < text.length && text[index] == ':') {
                index = skipWhitespace(text, index + 1)
                if (index < text.length && isQuote(text[index])) {
                    val end = stringEnd(text, index)
                    val range = TextRange(index + 1, end)
                    arguments.putIfAbsent(name, InlineArgument(range.substring(text), range))
                    index = minOf(end + 1, text.length)
                }
            }

            // the rest of the value, up to the comma or the end of the call
            var depth = 0
            while (index < text.length) {
                val char = text[index]
                when {
                    isQuote(char) -> index = stringEnd(text, index)
                    char == '(' || char == '{' || char == '[' -> depth++
                    char == ')' && depth == 0 -> return arguments
                    char == ')' || char == '}' || char == ']' -> depth--
                    char == ',' && depth == 0 -> break
                }
                index++
            }
            index++
        }
        return arguments
    }

    /** The offset of the quote that closes the string starting at the offset, or the end of the text */
    private fun stringEnd(text: String, start: Int): Int {
        val quote = text[start]
        var index = start + 1
        while (index < text.length && text[index] != quote) {
            if (text[index] == '\\') index++
            index++
        }
        return minOf(index, text.length)
    }

    private fun skipWhitespace(text: String, start: Int): Int {
        var index = start
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }

    private fun isQuote(char: Char): Boolean = char == '\'' || char == '"'

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

/** A string argument of a view helper call in inline notation: its value and where it is written */
class InlineArgument(val value: String, val range: TextRange)

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
                val file = parameters.originalFile
                val project = file.project
                if (DumbService.isDumb(project) || !FluidTranslationUsage.isTemplate(file)) return
                val resolver = TranslationResolver.getInstance(project)

                val value = PsiTreeUtil.getParentOfType(parameters.position, XmlAttributeValue::class.java, false)
                val argument = value?.let(FluidTranslationUsage::tagArgument)
                if (value != null && argument != null) {
                    val valueStart = value.textRange.startOffset + ElementManipulators.getValueTextRange(value).startOffset
                    val content = value.value
                    val prefix = content.take((parameters.offset - valueStart).coerceIn(0, content.length))
                    TranslationLookups.addArgument(resolver, argument.role, argument.packageKey, argument.source, prefix, result)
                    return
                }

                // the inline notation, in a text or in an attribute of any tag
                val text = file.text
                val (inline, written) = FluidTranslationUsage.inlineArgumentAt(file, text, parameters.offset) ?: return
                val prefix = text.substring(written.range.startOffset, parameters.offset)
                TranslationLookups.addArgument(resolver, inline.role, inline.packageKey, inline.source, prefix, result)
            }
        })
    }
}
