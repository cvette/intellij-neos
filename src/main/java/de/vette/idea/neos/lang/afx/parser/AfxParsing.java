package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.html.HtmlParsing;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.psi.XmlPsiBundle;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;

public class AfxParsing extends HtmlParsing {
    public AfxParsing(PsiBuilder builder) {
        super(builder);
    }

    protected IElementType getHtmlTagElementType() {
        return AfxElementTypes.AFX_TAG;
    }

    @Override
    protected boolean hasCustomTagContent() {
        return this.token() == AfxElementTypes.AFX_EEL_START_DELIMITER;
    }

    @Override
    protected void parseAttribute() {
        assert token() == XmlTokenType.XML_NAME;
        final PsiBuilder.Marker att = mark();
        advance();
        if (token() == XmlTokenType.XML_EQ) {
            advance();
            parseAttributeValue();
        }
        att.done(AfxElementTypes.AFX_ATTRIBUTE);
    }

    protected void parseAttributeValue() {
        final PsiBuilder.Marker attValue = mark();
        if (token() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
            while (true) {
                final IElementType tt = token();
                if (tt == null
                        || tt == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER
                        || tt == XmlTokenType.XML_END_TAG_START
                        || tt == XmlTokenType.XML_EMPTY_ELEMENT_END
                        || tt == XmlTokenType.XML_START_TAG_START) {
                    break;
                }

                if (tt == XmlTokenType.XML_BAD_CHARACTER) {
                    final PsiBuilder.Marker error = mark();
                    advance();
                    error.error(XmlPsiBundle.message("xml.parsing.unescaped.ampersand.or.nonterminated.character.entity.reference"));
                } else if (tt == XmlTokenType.XML_ENTITY_REF_TOKEN) {
                    parseReference();
                } else {
                    advance();
                }
            }

            if (token() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
                advance();
            } else {
                error(XmlPsiBundle.message("xml.parsing.unclosed.attribute.value"));
            }
        } else {
            while(token() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || token() == AfxElementTypes.AFX_EEL_START_DELIMITER) {
                if (token() == AfxElementTypes.AFX_EEL_START_DELIMITER) {
                    parseAttributeExpression();
                } else {
                    advance(); // Single token att value
                }
            }
        }

        attValue.done(XmlElementType.XML_ATTRIBUTE_VALUE);
    }

    private void parseAttributeExpression() {
        advance(); // {

        // Guard against empty expressions
        if (token() == AfxElementTypes.AFX_EEL_VALUE) advance();

        advance(); // }
    }
}
