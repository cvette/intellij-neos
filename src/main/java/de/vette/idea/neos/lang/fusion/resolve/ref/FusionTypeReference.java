/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.vette.idea.neos.lang.fusion.resolve.ref;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionTypeImpl;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FusionTypeReference extends FusionReferenceBase<FusionType> implements FusionReference {

    public FusionTypeReference(FusionType psiElement) {
        super(psiElement);
    }

    @Override
    List<PsiElement> resolveInner() {
        return ResolveEngine.getPrototypeDefinitions(getElement().getProject(), getElement());
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return new TextRange(0, myElement.getTextLength());
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        FusionTypeImpl type = (FusionTypeImpl)FusionElementFactory.createType(myElement.getProject(), newElementName);
        ASTNode node = myElement.getNode();
        ASTNode newTypeNode = type.getNode();

        node.getTreeParent().replaceChild(node, newTypeNode);
        return myElement;
    }
}
