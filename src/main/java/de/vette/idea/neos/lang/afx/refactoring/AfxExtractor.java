package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;

import java.util.ArrayList;
import java.util.List;

public class AfxExtractor {
    public static PsiElement[] getChildren(PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        boolean collectChildren = false;

        if (element instanceof XmlTag xmlTag) {
            for (PsiElement child : xmlTag.getChildren()) {
                if (child.getNode().getElementType() == XmlTokenType.XML_TAG_END) {
                    collectChildren = true;
                    continue;
                }
                if (child.getNode().getElementType() == XmlTokenType.XML_END_TAG_START) {
                    break;
                }
                if (collectChildren) {
                    children.add(child);
                }
            }
        }

        return children.toArray(new PsiElement[0]);
    }
}
