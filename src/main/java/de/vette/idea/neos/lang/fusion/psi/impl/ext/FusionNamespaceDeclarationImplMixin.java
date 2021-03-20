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
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import de.vette.idea.neos.lang.fusion.psi.FusionNamespaceDeclaration;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionStubbedElementImpl;
import de.vette.idea.neos.lang.fusion.stubs.FusionNamespaceDeclarationStub;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FusionNamespaceDeclarationImplMixin extends FusionStubbedElementImpl<FusionNamespaceDeclarationStub> implements FusionNamespaceDeclaration {

    public FusionNamespaceDeclarationImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public FusionNamespaceDeclarationImplMixin(@NotNull FusionNamespaceDeclarationStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return null;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return getNamespace();
    }
}
