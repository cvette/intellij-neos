package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;

public class InnerAfxLexer extends MergingLexerAdapter {
    private static final TokenSet TOKENS_TO_MERGE;

    public InnerAfxLexer() {
        super(new FlexAdapter(new _AfxLexer()), TOKENS_TO_MERGE);
    }

    static {
        TOKENS_TO_MERGE = TokenSet.create(
                XmlTokenType.XML_COMMENT_CHARACTERS, XmlTokenType.XML_WHITE_SPACE, XmlTokenType.XML_REAL_WHITE_SPACE,
                XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlTokenType.XML_DATA_CHARACTERS,
                XmlTokenType.XML_TAG_CHARACTERS, AfxElementTypes.AFX_EEL_VALUE);
    }
}
