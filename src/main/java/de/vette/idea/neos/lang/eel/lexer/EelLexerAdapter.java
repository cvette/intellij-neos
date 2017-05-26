package de.vette.idea.neos.lang.eel.lexer;

import com.intellij.lexer.FlexAdapter;

public class EelLexerAdapter extends FlexAdapter {

    public EelLexerAdapter() {
        super(new EelLexer());
    }
}
