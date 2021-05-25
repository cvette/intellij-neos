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

package de.vette.idea.neos.lang.eel.highlighting;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.eel.EelLexerAdapter;
import de.vette.idea.neos.lang.eel.psi.EelTypes;
import de.vette.idea.neos.lang.fusion.highlighting.FusionHighlightingColors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EelSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final Map<IElementType, TextAttributesKey> ourMap1;

    static {
        ourMap1 = new HashMap<>();
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
        return new EelLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        return pack(ourMap1.get(tokenType));
    }
}
