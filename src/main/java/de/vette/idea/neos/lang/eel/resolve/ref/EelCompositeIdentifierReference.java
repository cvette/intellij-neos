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

public class EelCompositeIdentifierReference extends EelReferenceBase<EelCompositeIdentifier> {
    public EelCompositeIdentifierReference(EelCompositeIdentifier psiElement) {
        super(psiElement);
    }

    @Override
    List<PsiElement> resolveInner() {
        return ResolveEngine.getEelHelpers(getElement().getProject(), getElement().getText());
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return new TextRange(getElement().getStartOffsetInParent(), getElement().getStartOffsetInParent() + getElement().getTextLength());
    }
}
