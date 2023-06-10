package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.HtmlXmlExtension;
import com.intellij.xml.XmlExtension;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.afx.formatter.AfxCodeStyleSettings;
import de.vette.idea.neos.lang.afx.formatter.AfxAttributeValuePresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfxXmlExtension extends HtmlXmlExtension {

    private static final XmlExtension.AttributeValuePresentation BRACES_PRESENTATION = new XmlExtension.AttributeValuePresentation() {
        public @NotNull String getPrefix() {
            return "{";
        }

        public @NotNull String getPostfix() {
            return "}";
        }

        public boolean showAutoPopup() {
            return false;
        }
    };

    private static final XmlExtension.AttributeValuePresentation EMPTY_VALUE_PRESENTATION = new XmlExtension.AttributeValuePresentation() {
        public @NotNull String getPrefix() {
            return "";
        }

        public @NotNull String getPostfix() {
            return "";
        }

        public boolean showAutoPopup() {
            return false;
        }
    };

    @Override
    public boolean isAvailable(PsiFile file) {
        return file.getLanguage() instanceof AfxLanguage;
    }

    @Override
    public @NotNull AttributeValuePresentation getAttributeValuePresentation(@Nullable XmlTag tag, @NotNull String attributeName, @NotNull String defaultAttributeQuote) {
        if (tag == null) {
            return super.getAttributeValuePresentation(tag, attributeName, defaultAttributeQuote);
        }

        AfxAttributeValuePresentation afxQuoteStyle = AfxCodeStyleSettings.getAfxAttributeValue(tag);

        if (afxQuoteStyle == AfxAttributeValuePresentation.NONE) {
            return EMPTY_VALUE_PRESENTATION;
        }


        if (afxQuoteStyle == AfxAttributeValuePresentation.BRACES) {
            return BRACES_PRESENTATION;
        }

        return super.getAttributeValuePresentation(tag, attributeName, defaultAttributeQuote);
    }
}
