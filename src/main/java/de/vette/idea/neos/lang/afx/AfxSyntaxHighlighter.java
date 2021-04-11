package de.vette.idea.neos.lang.afx;

import com.intellij.ide.highlighter.HtmlFileHighlighter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.afx.parser.AfxHighlightingLexer;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;
import de.vette.idea.neos.lang.fusion.highlighting.FusionHighlightingColors;
import org.jetbrains.annotations.NotNull;

public class AfxSyntaxHighlighter extends HtmlFileHighlighter {

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new AfxHighlightingLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == AfxElementTypes.AFX_EEL_START_DELIMITER
                || tokenType == AfxElementTypes.AFX_EEL_END_DELIMITER
                || tokenType == AfxElementTypes.AFX_EEL_VALUE) {
            return pack(TextAttributesKey.createTextAttributesKey("AFX_KEYWORD", FusionHighlightingColors.EEL_WRAPPER));
        }

        return super.getTokenHighlights(tokenType);
    }
}
