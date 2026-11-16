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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import de.vette.idea.neos.indexes.DefaultContextFileIndex
import de.vette.idea.neos.lang.eel.psi.EelCompositeIdentifier
import de.vette.idea.neos.lang.eel.psi.EelExpression
import de.vette.idea.neos.lang.eel.psi.EelMethodCall
import de.vette.idea.neos.lang.eel.psi.EelObjectPath
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.translation.TranslationId

/**
 * What a string argument of a translation helper call stands for.
 *
 * @property packageKey the package the label is looked up in, null if it is not a string literal
 * @property source the source in path notation, null if it is not a string literal
 * @property shorthandAllowed true if the id may be written as `Package:Source:id`
 */
class EelTranslationArgument(
    val string: EelValueStringLine,
    val role: Role,
    val packageKey: String?,
    val source: String?,
    val shorthandAllowed: Boolean,
) {
    enum class Role { ID, SOURCE, PACKAGE }
}

/**
 * Recognizes the calls of the translation helper of Flow:
 *
 * - `I18n.translate('Package:Source:id')`
 * - `I18n.translate(id, originalLabel, arguments, source, package, quantity, locale)`
 * - `I18n.id('id').package('Package').source('Source')`
 */
object EelTranslationUsage {
    const val DEFAULT_PACKAGE = "Neos.Flow"

    private const val TRANSLATION_HELPER = "\\TranslationHelper"
    private val HELPER_NAMES = setOf("I18n", "Translation")

    private const val TRANSLATE = "translate"
    private const val ID = "id"
    private const val VALUE = "value"
    private const val SOURCE = "source"
    private const val PACKAGE = "package"

    private const val SOURCE_ARGUMENT = 3
    private const val PACKAGE_ARGUMENT = 4

    @JvmStatic
    fun forString(string: EelValueStringLine): EelTranslationArgument? {
        val call = string.parent as? EelMethodCall ?: return null
        val path = call.parent as? EelObjectPath ?: return null
        if (!isTranslationHelper(path)) return null

        val arguments = call.expressionList
        val index = arguments.indexOf(string)

        return when (call.methodName.text) {
            TRANSLATE -> when (index) {
                0 -> EelTranslationArgument(
                    string, EelTranslationArgument.Role.ID,
                    packageKey = literal(arguments, PACKAGE_ARGUMENT, DEFAULT_PACKAGE),
                    source = literal(arguments, SOURCE_ARGUMENT, TranslationId.DEFAULT_SOURCE),
                    shorthandAllowed = arguments.size == 1,
                )
                SOURCE_ARGUMENT -> EelTranslationArgument(
                    string, EelTranslationArgument.Role.SOURCE,
                    packageKey = literal(arguments, PACKAGE_ARGUMENT, DEFAULT_PACKAGE),
                    source = content(string),
                    shorthandAllowed = false,
                )
                PACKAGE_ARGUMENT -> packageArgument(string)
                else -> null
            }
            ID -> if (index != 0) null else EelTranslationArgument(
                string, EelTranslationArgument.Role.ID,
                packageKey = fluent(path, PACKAGE, DEFAULT_PACKAGE),
                source = fluent(path, SOURCE, TranslationId.DEFAULT_SOURCE),
                shorthandAllowed = false,
            )
            SOURCE -> if (index != 0 || !isFluent(path)) null else EelTranslationArgument(
                string, EelTranslationArgument.Role.SOURCE,
                packageKey = fluent(path, PACKAGE, DEFAULT_PACKAGE),
                source = content(string),
                shorthandAllowed = false,
            )
            PACKAGE -> if (index != 0 || !isFluent(path)) null else packageArgument(string)
            else -> null
        }
    }

    /** The text between the quotes. */
    @JvmStatic
    fun content(string: EelValueStringLine): String = contentRange(string).substring(string.text)

    /** The range of the text between the quotes, relative to the string. */
    @JvmStatic
    fun contentRange(string: EelValueStringLine): TextRange {
        val text = string.text
        val hasClosingQuote = text.length >= 2 && text.last() == text.first()
        return TextRange(1.coerceAtMost(text.length), if (hasClosingQuote) text.length - 1 else text.length)
    }

    private fun packageArgument(string: EelValueStringLine) = EelTranslationArgument(
        string, EelTranslationArgument.Role.PACKAGE,
        packageKey = content(string),
        source = null,
        shorthandAllowed = false,
    )

    private fun isTranslationHelper(path: EelObjectPath): Boolean {
        val helper = (path.firstChild as? EelCompositeIdentifier)?.text ?: return false
        if (helper in HELPER_NAMES) return true

        return FileBasedIndex.getInstance()
            .getValues(DefaultContextFileIndex.KEY, helper, GlobalSearchScope.allScope(path.project))
            .any { it.trim('\'', '"').replace("\\\\", "\\").endsWith(TRANSLATION_HELPER) }
    }

    /** `I18n.id(...)` and `I18n.value(...)` start the fluent notation. */
    private fun isFluent(path: EelObjectPath): Boolean =
        path.methodCallList.any { it.methodName.text == ID || it.methodName.text == VALUE }

    /** The value of the n-th argument: the default if it is omitted, null if it is not a string literal. */
    private fun literal(arguments: List<EelExpression>, index: Int, default: String): String? {
        val argument = arguments.getOrNull(index) ?: return default
        if (argument.text == "null") return default
        return (argument as? EelValueStringLine)?.let(::content)
    }

    /** The argument of `.package('...')` or `.source('...')` in a fluent chain. */
    private fun fluent(path: EelObjectPath, method: String, default: String): String? {
        val call = path.methodCallList.firstOrNull { it.methodName.text == method } ?: return default
        return (call.expressionList.firstOrNull() as? EelValueStringLine)?.let(::content)
    }
}
