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

package de.vette.idea.neos.lang.eel.psi

import com.intellij.psi.tree.IElementType

/**
 * Creates the Eel token types with a readable name.
 *
 * The name of a token type is what the parser shows in its error messages, for example
 * "',' or ')' expected" instead of "VALUE_SEPARATOR or EEL_RIGHT_PAREN expected".
 */
object EelTokenTypeFactory {

    private val DISPLAY_NAMES = mapOf(
        "EEL_LEFT_PAREN" to "(",
        "EEL_RIGHT_PAREN" to ")",
        "EEL_LEFT_BRACKET" to "[",
        "EEL_RIGHT_BRACKET" to "]",
        "EEL_OBJECT_LEFT_BRACE" to "{",
        "EEL_OBJECT_RIGHT_BRACE" to "}",
        "VALUE_SEPARATOR" to ",",
        "EEL_DOT" to ".",
        "IF_KEYWORD" to "?",
        "IF_SEPARATOR" to ":",
        "EEL_ARROW" to "=>",
        "EEL_ADDITION_OPERATOR" to "+",
        "EEL_SUBTRACTION_OPERATOR" to "-",
        "EEL_MULTIPLICATION_OPERATOR" to "*",
        "EEL_DIVISION_OPERATOR" to "/",
        "EEL_MODULO_OPERATOR" to "%",
        "EEL_BOOLEAN_AND" to "&&",
        "EEL_BOOLEAN_OR" to "||",
        "EEL_NEGATION_OPERATOR" to "!",
        "EEL_COMPARISON_OPERATOR" to "comparison operator",
        "EEL_IDENTIFIER" to "identifier",
        "EEL_FUNCTION" to "function name",
        "VALUE_NUMBER" to "number",
        "VALUE_BOOLEAN" to "boolean",
        "VALUE_STRING_QUOTE" to "quote",
        "VALUE_STRING" to "string content",
        "VALUE_STRING_ESCAPED_QUOTE" to "escaped quote",
        "CRLF" to "line break",
    )

    @JvmStatic
    fun getTokenType(name: String): IElementType = EelTokenType(DISPLAY_NAMES[name] ?: name)
}
