package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lexer.HtmlLexer;

public class AfxLexer extends HtmlLexer {
    public AfxLexer() {
        super(new InnerAfxLexer(), false);
    }
}
