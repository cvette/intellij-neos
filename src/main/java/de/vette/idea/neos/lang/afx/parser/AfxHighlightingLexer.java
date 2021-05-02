package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lexer.HtmlHighlightingLexer;
import com.intellij.lexer.LayeredLexer;

public class AfxHighlightingLexer extends LayeredLexer {
    public AfxHighlightingLexer() {
        super(new BaseAfxHighlightingLexer());
    }
}

class BaseAfxHighlightingLexer extends HtmlHighlightingLexer {
    public BaseAfxHighlightingLexer() {
        super(new InnerAfxLexer(), true, null);
    }

    protected boolean isHtmlTagState(int state) {
        return state == _AfxLexer.START_TAG_NAME || state == _AfxLexer.END_TAG_NAME;
    }
}