package de.vette.idea.neos.lang.eel.resolve.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.lang.eel.psi.EelCompositeIdentifier;
import de.vette.idea.neos.lang.eel.psi.EelMethodCall;
import de.vette.idea.neos.lang.eel.psi.EelMethodName;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EelMethodNameReference extends EelReferenceBase<EelMethodName> {
    public EelMethodNameReference(EelMethodName psiElement) {
        super(psiElement);
    }

    @Override
    List<PsiElement> resolveInner() {
        PsiElement parentElement = getElement().getParent();
        if (parentElement instanceof EelMethodCall) {
            if (parentElement.getPrevSibling() != null && parentElement.getPrevSibling().getPrevSibling() != null) {
                PsiElement compositeElement = parentElement.getPrevSibling().getPrevSibling();
                if (compositeElement instanceof EelCompositeIdentifier) {
                    return ResolveEngine.getEelHelperMethods(getElement().getProject(), compositeElement.getText(), getElement().getText());
                }
            }
        }

        return new ArrayList<>();
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return new TextRange(getElement().getStartOffsetInParent(), getElement().getStartOffsetInParent() + getElement().getTextLength());
    }
}
