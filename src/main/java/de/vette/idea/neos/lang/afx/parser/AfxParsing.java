package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.html.HtmlParsing;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.parsing.XmlParserBundle;
import com.intellij.xml.psi.XmlPsiBundle;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;
import de.vette.idea.neos.lang.afx.psi.AfxLazyElementTypes;

public class AfxParsing extends HtmlParsing {
    public AfxParsing(PsiBuilder builder) {
        super(builder);
    }

    protected IElementType getHtmlTagElementType() {
        return AfxElementTypes.AFX_TAG;
    }

    protected void parseAfxTag() {
        getBuilder().advanceLexer(); // {
        PsiBuilder.Marker marker = getBuilder().mark();

        boolean hasContentExpression = false;
        while (!getBuilder().eof() && getBuilder().getTokenType() != AfxElementTypes.AFX_EEL_END_DELIMITER) {
            getBuilder().advanceLexer();
            hasContentExpression = true;
        }

        if (hasContentExpression) {
            marker.collapse(AfxLazyElementTypes.CONTENT_EXPRESSION);
        } else {
            marker.error("Expression expected");
        }

        if (getBuilder().getTokenType() == AfxElementTypes.AFX_EEL_END_DELIMITER) {
            getBuilder().advanceLexer(); // }
        } else {
            getBuilder().error("Missing }");
        }
    }

    @Override
    protected PsiBuilder.Marker parseCustomTagContent(PsiBuilder.Marker xmlText) {
        terminateText(xmlText);
        parseAfxTag();
        return null;
    }

    @Override
    protected PsiBuilder.Marker parseCustomTopLevelContent(PsiBuilder.Marker error) {
        flushError(error);
        parseAfxTag();
        return null;
    }

    @Override
    protected boolean hasCustomTagContent() {
        IElementType type = this.token();
        return type == AfxElementTypes.AFX_EEL_START_DELIMITER;
    }

    @Override
    protected boolean hasCustomTopLevelContent() {
        return hasCustomTagContent();
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
                    error.error(XmlParserBundle.message("xml.parsing.unescaped.ampersand.or.nonterminated.character.entity.reference"));
                } else if (tt == XmlTokenType.XML_ENTITY_REF_TOKEN) {
                    parseReference();
                } else {
                    advance();
                }
            }

            if (token() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
                advance();
            } else {
                error(XmlParserBundle.message("xml.parsing.unclosed.attribute.value"));
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
        if (token() == AfxElementTypes.AFX_EEL_VALUE) {
            getBuilder().remapCurrentToken(AfxLazyElementTypes.CONTENT_EXPRESSION);
            advance();
        }

        advance(); // }
    }
}
