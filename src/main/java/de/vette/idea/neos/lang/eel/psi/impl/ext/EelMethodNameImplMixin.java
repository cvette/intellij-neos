package de.vette.idea.neos.lang.eel.psi.impl.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.lang.eel.psi.EelMethodName;
import de.vette.idea.neos.lang.eel.psi.impl.EelElementImpl;
import de.vette.idea.neos.lang.eel.resolve.ref.EelMethodNameReference;
import de.vette.idea.neos.lang.eel.resolve.ref.EelReference;
import org.jetbrains.annotations.NotNull;

public class EelMethodNameImplMixin extends EelElementImpl implements EelMethodName {

    public EelMethodNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public EelReference getReference() {
        return new EelMethodNameReference(this);
    }

    @NotNull
    @Override
    public PsiElement getEelFunction() {
        return null;
    }
}
