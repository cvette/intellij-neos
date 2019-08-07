package de.vette.idea.neos.lang.eel.lexer;

import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;

public class FusionEelLexer extends LayeredLexer {
    public FusionEelLexer(Lexer baseLexer) {
        super(baseLexer);
        this.registerLayer(new EelLexerAdapter());
    }
}
