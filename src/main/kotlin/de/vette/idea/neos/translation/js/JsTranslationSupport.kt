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

package de.vette.idea.neos.translation.js

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.javascript.psi.JSArgumentList
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.ecma6.JSStringTemplateExpression
import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLookups
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationRole
import de.vette.idea.neos.translation.translationArgumentReferences
import de.vette.idea.neos.translation.translationShorthandReferences

/**
 * What a string at a place the Neos UI takes a label from stands for.
 *
 * @param role null for an id in shorthand notation (`Package:Source:id`)
 * @param both true if the string may be a plain id as well as a shorthand id, which matters for completion
 */
class JsTranslationArgument(
    val role: TranslationRole?,
    val packageKey: String?,
    val source: String?,
    val both: Boolean = false,
)

/**
 * Recognizes the places labels are used at in plugins for the Neos UI:
 *
 * - `i18nRegistry.translate(id, fallback, params, packageKey, sourceName)`, where the id may also be written
 *   in shorthand notation, which wins over the package and the source
 * - `translate('Package:Source:id', fallback)` of any other object or without one, with a shorthand id only,
 *   as functions with that name are common
 * - the component `<I18n id="..." sourceName="..." packageKey="..."/>`
 */
object JsTranslationUsage {
    private const val DEFAULT_PACKAGE = "Neos.Neos"
    private const val TRANSLATE = "translate"
    private const val REGISTRY = "i18n"
    private const val COMPONENT = "I18n"

    private const val ID_INDEX = 0
    private const val PACKAGE_INDEX = 3
    private const val SOURCE_INDEX = 4

    private const val ID = "id"
    private const val SOURCE_NAME = "sourceName"
    private const val PACKAGE_KEY = "packageKey"

    @JvmStatic
    fun forLiteral(literal: JSLiteralExpression): JsTranslationArgument? {
        if (!isPlainString(literal)) return null
        val argumentList = literal.parent as? JSArgumentList ?: return null
        val call = argumentList.parent as? JSCallExpression ?: return null
        val method = call.methodExpression as? JSReferenceExpression ?: return null
        if (method.referenceName != TRANSLATE || !NeosProjectService.isEnabled(literal)) return null

        val arguments = argumentList.arguments
        val registry = isRegistry(method)
        val idIsShorthand = constant(arguments.getOrNull(ID_INDEX), "")?.contains(TranslationId.SEPARATOR) == true
        val packageKey = constant(arguments.getOrNull(PACKAGE_INDEX), DEFAULT_PACKAGE)
        val source = constant(arguments.getOrNull(SOURCE_INDEX), TranslationId.DEFAULT_SOURCE)

        return when (arguments.indexOf(literal)) {
            ID_INDEX -> when {
                idIsShorthand || !registry -> JsTranslationArgument(null, null, null)
                else -> JsTranslationArgument(TranslationRole.ID, packageKey, source, both = true)
            }
            PACKAGE_INDEX -> JsTranslationArgument(TranslationRole.PACKAGE, packageKey, source)
                .takeIf { registry && !idIsShorthand }
            SOURCE_INDEX -> JsTranslationArgument(TranslationRole.SOURCE, packageKey, source)
                .takeIf { registry && !idIsShorthand }
            else -> null
        }
    }

    /** The role of the value of an attribute of the I18n component */
    @JvmStatic
    fun forAttribute(value: XmlAttributeValue): JsTranslationArgument? {
        val attribute = value.parent as? XmlAttribute ?: return null
        val tag = attribute.parent ?: return null
        if (tag.name != COMPONENT || !NeosProjectService.isEnabled(value)) return null

        val idIsShorthand = tag.getAttributeValue(ID)?.contains(TranslationId.SEPARATOR) == true
        val packageKey = constant(tag.getAttributeValue(PACKAGE_KEY), DEFAULT_PACKAGE)
        val source = constant(tag.getAttributeValue(SOURCE_NAME), TranslationId.DEFAULT_SOURCE)

        return when (attribute.name) {
            ID -> when {
                idIsShorthand -> JsTranslationArgument(null, null, null)
                else -> JsTranslationArgument(TranslationRole.ID, packageKey, source, both = true)
            }
            PACKAGE_KEY -> JsTranslationArgument(TranslationRole.PACKAGE, packageKey, source).takeIf { !idIsShorthand }
            SOURCE_NAME -> JsTranslationArgument(TranslationRole.SOURCE, packageKey, source).takeIf { !idIsShorthand }
            else -> null
        }
    }

    @JvmStatic
    fun references(element: PsiElement): Array<PsiReference> {
        val argument = when (element) {
            is JSLiteralExpression -> forLiteral(element)
            // a value in braces is an expression
            is XmlAttributeValue -> forAttribute(element).takeIf { !element.value.contains('{') }
            else -> null
        } ?: return PsiReference.EMPTY_ARRAY

        val range = ElementManipulators.getValueTextRange(element)
        val role = argument.role ?: return translationShorthandReferences(element, range)
        return translationArgumentReferences(element, range, role, argument.packageKey, argument.source)
    }

    /** The object the labels of the Neos UI are asked from, usually `i18nRegistry` or `this.props.i18nRegistry` */
    private fun isRegistry(method: JSReferenceExpression): Boolean {
        val qualifier = method.qualifier ?: return false
        val name = (qualifier as? JSReferenceExpression)?.referenceName ?: qualifier.text
        return name.contains(REGISTRY, ignoreCase = true)
    }

    /** The value of an argument: the default if it is omitted, null if it is not a plain string */
    private fun constant(argument: JSExpression?, default: String): String? {
        if (argument == null) return default
        val literal = argument as? JSLiteralExpression ?: return null
        return literal.stringValue.takeIf { isPlainString(literal) }
    }

    /** A string in quotes; a template string may have expressions in it */
    private fun isPlainString(literal: JSLiteralExpression): Boolean =
        literal.isQuotedLiteral && literal !is JSStringTemplateExpression

    /** The value of an attribute: the default if it is omitted, null if it is an expression */
    private fun constant(value: String?, default: String): String? = when {
        value.isNullOrBlank() -> default
        value.contains('{') -> null
        else -> value
    }
}

class JsTranslationReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val provider = object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
                JsTranslationUsage.references(element)
        }
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(JSLiteralExpression::class.java), provider)
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlAttributeValue::class.java), provider)
    }
}

/**
 * Completes ids, sources and packages in the arguments of `translate()` and the attributes of `<I18n>`.
 */
class JsTranslationCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet,
            ) {
                val position = parameters.position
                if (DumbService.isDumb(position.project)) return

                // a tag of JSX is a literal as well, so the attribute value is looked at first
                val value = PsiTreeUtil.getParentOfType(position, XmlAttributeValue::class.java, false)
                val literal = PsiTreeUtil.getParentOfType(position, JSLiteralExpression::class.java, false)
                val (element, argument) = value?.let { JsTranslationUsage.forAttribute(it)?.let { argument -> it to argument } }
                    ?: literal?.let { JsTranslationUsage.forLiteral(it)?.let { argument -> it to argument } }
                    ?: return

                val valueRange = ElementManipulators.getValueTextRange(element).shiftRight(element.textRange.startOffset)
                if (parameters.offset !in valueRange.startOffset..valueRange.endOffset) return
                val prefix = parameters.editor.document.getText(valueRange.cutOut(
                    com.intellij.openapi.util.TextRange(0, parameters.offset - valueRange.startOffset)
                ))

                val resolver = TranslationResolver.getInstance(position.project)
                val role = argument.role
                if (role == null || (argument.both && prefix.contains(TranslationId.SEPARATOR))) {
                    TranslationLookups.addShorthand(resolver, prefix, result)
                    return
                }
                TranslationLookups.addArgument(resolver, role, argument.packageKey, argument.source, prefix, result)
                if (argument.both) TranslationLookups.addShorthand(resolver, prefix, result)
            }
        })
    }
}
