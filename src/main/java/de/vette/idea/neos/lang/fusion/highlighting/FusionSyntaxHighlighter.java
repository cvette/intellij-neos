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

package de.vette.idea.neos.lang.fusion.highlighting;

import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.eel.EelLexerAdapter;
import de.vette.idea.neos.lang.eel.highlighting.EelSyntaxHighlighter;
import de.vette.idea.neos.lang.eel.psi.EelTypes;
import de.vette.idea.neos.lang.fusion.FusionLexerAdapter;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FusionSyntaxHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> ourMap1;

    static {
        ourMap1 = new HashMap<>();
        ourMap1.put(FusionTypes.SINGLE_LINE_COMMENT, FusionHighlightingColors.SINGLE_LINE_COMMENT);
        ourMap1.put(FusionTypes.C_STYLE_COMMENT, FusionHighlightingColors.BLOCK_COMMENT);
        ourMap1.put(FusionTypes.DOC_COMMENT, FusionHighlightingColors.BLOCK_COMMENT);

        ourMap1.put(FusionTypes.META_PROPERTY_KEYWORD, FusionHighlightingColors.META_PROPERTY);
        ourMap1.put(FusionTypes.META_PROPERTY_NAME, FusionHighlightingColors.META_PROPERTY);
        ourMap1.put(FusionTypes.PATH_SEPARATOR, FusionHighlightingColors.PATH);
        ourMap1.put(FusionTypes.PATH_PART, FusionHighlightingColors.PATH);

        ourMap1.put(FusionTypes.INCLUDE_KEYWORD, FusionHighlightingColors.DECLARATION);
        ourMap1.put(FusionTypes.INCLUDE_SEPARATOR, FusionHighlightingColors.DECLARATION);
        ourMap1.put(FusionTypes.NAMESPACE_KEYWORD, FusionHighlightingColors.DECLARATION);
        ourMap1.put(FusionTypes.NAMESPACE_SEPARATOR, FusionHighlightingColors.DECLARATION);
        ourMap1.put(FusionTypes.INCLUDE_PATH, FusionHighlightingColors.INCLUDE_PATH);

        ourMap1.put(FusionTypes.VALUE_STRING, FusionHighlightingColors.STRING);
        ourMap1.put(FusionTypes.VALUE_STRING_QUOTE, FusionHighlightingColors.STRING);
        ourMap1.put(FusionTypes.VALUE_STRING_ESCAPED_QUOTE, FusionHighlightingColors.STRING_ESCAPED);
        ourMap1.put(FusionTypes.VALUE_BOOLEAN, FusionHighlightingColors.VALUE);
        ourMap1.put(FusionTypes.VALUE_NULL, FusionHighlightingColors.VALUE);
        ourMap1.put(FusionTypes.VALUE_NUMBER, FusionHighlightingColors.NUMBER);

        ourMap1.put(FusionTypes.ASSIGNMENT_OPERATOR, FusionHighlightingColors.ASSIGNMENT_OPERATOR);
        ourMap1.put(FusionTypes.COPY_OPERATOR, FusionHighlightingColors.COPY_OPERATOR);
        ourMap1.put(FusionTypes.UNSET_OPERATOR, FusionHighlightingColors.UNSET_OPERATOR);

        ourMap1.put(FusionTypes.EXPRESSION_KEYWORD, FusionHighlightingColors.EEL_WRAPPER);
        ourMap1.put(FusionTypes.EEL_START_DELIMITER, FusionHighlightingColors.EEL_WRAPPER);
        ourMap1.put(FusionTypes.EEL_END_DELIMITER, FusionHighlightingColors.EEL_WRAPPER);

        ourMap1.put(FusionTypes.PROTOTYPE_KEYWORD, FusionHighlightingColors.PROTOTYPE);
        ourMap1.put(FusionTypes.OBJECT_TYPE_NAMESPACE, FusionHighlightingColors.OBJECT_TYPE);
        ourMap1.put(FusionTypes.OBJECT_TYPE_SEPARATOR, FusionHighlightingColors.OBJECT_TYPE);
        ourMap1.put(FusionTypes.UNQUALIFIED_TYPE, FusionHighlightingColors.OBJECT_TYPE);
        ourMap1.put(FusionTypes.NAMESPACE_ALIAS, FusionHighlightingColors.OBJECT_TYPE);
        ourMap1.put(FusionTypes.PACKAGE_KEY, FusionHighlightingColors.OBJECT_TYPE);
        ourMap1.put(FusionTypes.DSL_IDENTIFIER, FusionHighlightingColors.DSL_IDENTIFIER);

        ourMap1.put(FusionTypes.LEFT_BRACE, FusionHighlightingColors.BRACES);
        ourMap1.put(FusionTypes.RIGHT_BRACE, FusionHighlightingColors.BRACES);

        ourMap1.put(FusionTypes.LEFT_PAREN, FusionHighlightingColors.PARENTHESES);
        ourMap1.put(FusionTypes.RIGHT_PAREN, FusionHighlightingColors.PARENTHESES);

        ourMap1.put(EelTypes.VALUE_STRING, FusionHighlightingColors.STRING);
        ourMap1.put(EelTypes.VALUE_STRING_QUOTE, FusionHighlightingColors.STRING);
        ourMap1.put(EelTypes.VALUE_STRING_ESCAPED_QUOTE, FusionHighlightingColors.STRING_ESCAPED);
        ourMap1.put(EelTypes.VALUE_BOOLEAN, FusionHighlightingColors.VALUE);
        ourMap1.put(EelTypes.VALUE_NUMBER, FusionHighlightingColors.NUMBER);

        ourMap1.put(EelTypes.EEL_FUNCTION, FusionHighlightingColors.EEL_FUNCTION);
        ourMap1.put(EelTypes.EEL_IDENTIFIER, FusionHighlightingColors.EEL_IDENTIFIER);
        ourMap1.put(EelTypes.EEL_BOOLEAN_AND, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_BOOLEAN_OR, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_ADDITION_OPERATOR, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_SUBTRACTION_OPERATOR, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_COMPARISON_OPERATOR, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_DIVISION_OPERATOR, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_MULTIPLICATION_OPERATOR, FusionHighlightingColors.EEL_OPERATOR);
        ourMap1.put(EelTypes.EEL_MODULO_OPERATOR, FusionHighlightingColors.EEL_OPERATOR);
    }

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        LayeredLexer lexer = new LayeredLexer(new FusionLexerAdapter());
        lexer.registerLayer(new EelLexerAdapter(), FusionTypes.EEL_VALUE);
        return lexer;
    }

    @NotNull
    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (EelSyntaxHighlighter.ourMap1.containsKey(tokenType)) {
            return pack(EelSyntaxHighlighter.ourMap1.get(tokenType));
        }

        return pack(ourMap1.get(tokenType));
    }
}
