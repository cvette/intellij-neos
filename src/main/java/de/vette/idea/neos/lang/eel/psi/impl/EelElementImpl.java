package de.vette.idea.neos.lang.eel.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import de.vette.idea.neos.lang.eel.psi.EelElement;
import org.jetbrains.annotations.NotNull;

public class EelElementImpl extends ASTWrapperPsiElement implements EelElement {

    public EelElementImpl(@NotNull ASTNode node) {
        super(node);
    }
}
