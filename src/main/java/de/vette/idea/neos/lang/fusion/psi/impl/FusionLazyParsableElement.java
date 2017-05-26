package de.vette.idea.neos.lang.fusion.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ILazyParseableElementTypeBase;
import org.jetbrains.annotations.NotNull;

public class FusionLazyParsableElement extends FusionCompositeElementImpl implements ILazyParseableElementTypeBase {

    public FusionLazyParsableElement(@NotNull ASTNode astNode) {
        super(astNode);
    }

    @Override
    public ASTNode parseContents(@NotNull ASTNode chameleon) {
        return null;
    }
}
