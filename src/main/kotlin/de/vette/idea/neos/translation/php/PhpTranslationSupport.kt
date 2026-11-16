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

package de.vette.idea.neos.translation.php

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLookups
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationRole
import de.vette.idea.neos.translation.translationArgumentReferences

/**
 * What a string argument of a call of Flow's translator stands for.
 */
class PhpTranslationArgument(val role: TranslationRole, val packageKey: String?, val source: String?)

/**
 * Recognizes the calls of `Neos\Flow\I18n\Translator`:
 *
 * - `translateById($labelId, $arguments, $quantity, $locale, $sourceName, $packageKey)`
 * - `translateByOriginalLabel($originalLabel, $arguments, $quantity, $locale, $sourceName, $packageKey)`
 *
 * with positional and named arguments.
 */
object PhpTranslationUsage {
    private const val DEFAULT_PACKAGE = "Neos.Flow"
    private const val TRANSLATE_BY_ID = "translateById"
    private const val TRANSLATE_BY_ORIGINAL_LABEL = "translateByOriginalLabel"
    private const val TRANSLATOR = "\\Neos\\Flow\\I18n\\Translator"
    private const val MAX_CLASS_DEPTH = 10

    private const val LABEL_ID = "labelId"
    private const val SOURCE_NAME = "sourceName"
    private const val PACKAGE_KEY = "packageKey"
    private val PARAMETERS = listOf(LABEL_ID, "arguments", "quantity", "locale", SOURCE_NAME, PACKAGE_KEY)

    @JvmStatic
    fun forLiteral(literal: StringLiteralExpression): PhpTranslationArgument? {
        val parameterList = literal.parent as? ParameterList ?: return null
        val call = parameterList.parent as? MethodReference ?: return null
        val method = call.name
        if (method != TRANSLATE_BY_ID && method != TRANSLATE_BY_ORIGINAL_LABEL) return null
        if (!NeosProjectService.isEnabled(literal) || !isTranslator(call)) return null

        val arguments = arguments(parameterList)
        val name = arguments.entries.firstOrNull { it.value == literal }?.key ?: return null
        val packageKey = literal(arguments[PACKAGE_KEY], DEFAULT_PACKAGE)
        val source = literal(arguments[SOURCE_NAME], TranslationId.DEFAULT_SOURCE)

        return when (name) {
            // the first argument of translateByOriginalLabel() is a text, not an id
            LABEL_ID -> if (method == TRANSLATE_BY_ID) PhpTranslationArgument(TranslationRole.ID, packageKey, source) else null
            SOURCE_NAME -> PhpTranslationArgument(TranslationRole.SOURCE, packageKey, source)
            PACKAGE_KEY -> PhpTranslationArgument(TranslationRole.PACKAGE, packageKey, source)
            else -> null
        }
    }

    @JvmStatic
    fun references(literal: StringLiteralExpression): Array<PsiReference> {
        // a string with variables in it is not a constant id
        if (literal.firstPsiChild != null) return PsiReference.EMPTY_ARRAY
        val argument = forLiteral(literal) ?: return PsiReference.EMPTY_ARRAY
        return translationArgumentReferences(literal, literal.valueRange, argument.role, argument.packageKey, argument.source)
    }

    /**
     * The arguments by the name of their parameter, for positional and for named arguments (`sourceName: 'Main'`)
     */
    private fun arguments(parameterList: ParameterList): Map<String, PsiElement> {
        val arguments = LinkedHashMap<String, PsiElement>()
        parameterList.parameters.forEachIndexed { index, argument ->
            val colon = PsiTreeUtil.skipWhitespacesAndCommentsBackward(argument)
            val name = if (colon?.text == ":") {
                PsiTreeUtil.skipWhitespacesAndCommentsBackward(colon)?.text
            } else {
                PARAMETERS.getOrNull(index)
            }
            if (name != null) arguments[name] = argument
        }
        return arguments
    }

    /** The value of an argument: the default if it is omitted, null if it is not a plain string */
    private fun literal(argument: PsiElement?, default: String): String? {
        if (argument == null) return default
        val literal = argument as? StringLiteralExpression ?: return null
        return literal.contents.takeIf { literal.firstPsiChild == null }
    }

    /**
     * Other classes may have methods with these names, so the method has to be the one of Flow's translator or
     * of a class that extends it. If the method cannot be resolved, the name has to do.
     */
    private fun isTranslator(call: MethodReference): Boolean {
        val method = call.resolve() as? Method ?: return true
        return generateSequence(method.containingClass) { it.superClass }
            .take(MAX_CLASS_DEPTH)
            .any { it.fqn == TRANSLATOR }
    }
}

class PhpTranslationReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
                    PhpTranslationUsage.references(element as StringLiteralExpression)
            }
        )
    }
}

/**
 * Completes ids, sources and packages in the arguments of the translator.
 */
class PhpTranslationCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet,
            ) {
                val literal = PsiTreeUtil.getParentOfType(parameters.position, StringLiteralExpression::class.java, false)
                    ?: return
                val project = literal.project
                if (DumbService.isDumb(project)) return
                val argument = PhpTranslationUsage.forLiteral(literal) ?: return

                val valueStart = literal.textRange.startOffset + literal.valueRange.startOffset
                val content = literal.contents
                val prefix = content.take((parameters.offset - valueStart).coerceIn(0, content.length))
                TranslationLookups.addArgument(
                    TranslationResolver.getInstance(project), argument.role, argument.packageKey, argument.source, prefix, result
                )
            }
        })
    }
}
