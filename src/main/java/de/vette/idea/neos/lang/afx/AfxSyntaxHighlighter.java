package de.vette.idea.neos.lang.afx;

import com.intellij.ide.highlighter.HtmlFileHighlighter;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.afx.parser.AfxHighlightingLexer;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;
import de.vette.idea.neos.lang.eel.EelLexerAdapter;
import de.vette.idea.neos.lang.eel.highlighting.EelSyntaxHighlighter;
import de.vette.idea.neos.lang.fusion.highlighting.FusionHighlightingColors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AfxSyntaxHighlighter extends HtmlFileHighlighter {

    public static final Map<IElementType, TextAttributesKey> ourMap1;

    static {
        ourMap1 = new HashMap<>();
        ourMap1.put(AfxElementTypes.AFX_EEL_START_DELIMITER, FusionHighlightingColors.EEL_WRAPPER);
        ourMap1.put(AfxElementTypes.AFX_EEL_END_DELIMITER, FusionHighlightingColors.EEL_WRAPPER);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        LayeredLexer lexer = new LayeredLexer(new AfxHighlightingLexer());
        lexer.registerLayer(new EelLexerAdapter(), AfxElementTypes.AFX_EEL_VALUE);
        return lexer;
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (ourMap1.containsKey(tokenType)) {
            return pack(ourMap1.get(tokenType));
        }

        if (EelSyntaxHighlighter.ourMap1.containsKey(tokenType)) {
            return pack(EelSyntaxHighlighter.ourMap1.get(tokenType));
        }

        return super.getTokenHighlights(tokenType);
    }
}
