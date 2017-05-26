package de.vette.idea.neos.lang.fusion.lexer;

import com.intellij.lexer.FlexAdapter;

public class FusionLexerAdapter extends FlexAdapter {

    public FusionLexerAdapter() {
        super(new FusionLexer());
    }
}
