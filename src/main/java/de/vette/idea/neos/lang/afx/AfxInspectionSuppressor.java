package de.vette.idea.neos.lang.afx;

import com.intellij.codeInspection.DefaultXmlSuppressionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeValue;
import de.vette.idea.neos.lang.afx.psi.AfxFile;
import de.vette.idea.neos.lang.afx.psi.AfxTag;
import org.jetbrains.annotations.NotNull;

public class AfxInspectionSuppressor extends DefaultXmlSuppressionProvider {
    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String inspectionId) {
        if (inspectionId.equals("XmlUnboundNsPrefix")) {
            return true;
        }

        if (inspectionId.equals("HtmlUnknownTarget") && element instanceof XmlAttributeValue) {
            if (element.getText().startsWith("{")) {
                return true;
            }
        }

        if (inspectionId.equals("HtmlUnknownAttribute")) {
            if (element.getText().startsWith("@")) {
                return true;
            }

            if (element.getParent().getParent() instanceof AfxTag) {
                AfxTag tag = (AfxTag) element.getParent().getParent();
                String name = tag.getName();
                if (Character.isUpperCase(name.charAt(0))) {
                    return true;
                }
            }
        }

        return super.isSuppressedFor(element, inspectionId);
    }

    @Override
    public boolean isProviderAvailable(@NotNull PsiFile file) {
        return file.getContainingFile() instanceof AfxFile;
    }
}
