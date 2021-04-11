package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.HtmlXmlExtension;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfxXmlExtension extends HtmlXmlExtension {
    @Override
    public boolean isAvailable(PsiFile file) {
        return file.getLanguage() instanceof AfxLanguage;
    }

    @Override
    public @NotNull AttributeValuePresentation getAttributeValuePresentation(@Nullable XmlTag tag, @NotNull String attributeName, @NotNull String defaultAttributeQuote) {
        return new AttributeValuePresentation() {
            @Override
            public @NotNull String getPrefix() {
                return "{";
            }

            @Override
            public @NotNull String getPostfix() {
                return "}";
            }
        };
    }
}
