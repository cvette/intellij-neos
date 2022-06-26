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

package de.vette.idea.neos.lang.fusion.psi.impl.ext;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionStubbedElementImpl;
import de.vette.idea.neos.lang.fusion.stubs.FusionPropertyAssignmentStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FusionPropertyAssignmentImplMixin
        extends FusionStubbedElementImpl<FusionPropertyAssignmentStub>
        implements FusionPropertyAssignment {

    public FusionPropertyAssignmentImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    public FusionPropertyAssignmentImplMixin(@NotNull FusionPropertyAssignmentStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return this.getPath();
    }

    @Override
    public String getName() {
        return this.getPath().getText();
    }

    @Override
    public PsiElement setName(@NlsSafe @NotNull String name) throws IncorrectOperationException {
        return this;
    }

    @Override
    public boolean isSimpleProperty() {
        FusionPath path = getPath();
        return path.getNode().getChildren(TokenSet.ANY).length == 1;
    }
}
