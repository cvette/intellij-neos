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
import com.intellij.psi.PsiReference;
import com.intellij.psi.stubs.IStubElementType;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionStubbedElementImpl;
import de.vette.idea.neos.lang.fusion.resolve.ref.FusionPrototypeInheritanceReference;
import de.vette.idea.neos.lang.fusion.resolve.ref.FusionReference;
import de.vette.idea.neos.lang.fusion.stubs.FusionPrototypeSignatureStub;
import org.jetbrains.annotations.NotNull;

public abstract class FusionPrototypeSignatureImplMixin
        extends FusionStubbedElementImpl<FusionPrototypeSignatureStub>
        implements FusionPrototypeSignature {

    public FusionPrototypeSignatureImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    public FusionPrototypeSignatureImplMixin(@NotNull FusionPrototypeSignatureStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public FusionReference getReference() {
        if (!isInheritedInDefinition()) {
            return null;
        }

        return new FusionPrototypeInheritanceReference(this);
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return super.getReferences();
    }

    public boolean isInheritedInDefinition() {
        return (getParent() instanceof FusionPropertyCopy);
    }

    public boolean isDefinition() {
        return isSingleLineDefinition()
                || (isSingleElementInPathAtFileRoot()
                    && (getParent().getParent() instanceof FusionPropertyBlock
                    || getParent().getParent() instanceof FusionPropertyCopy));
    }

    public boolean isInheritanceDefinition() {
        return isSingleElementInPathAtFileRoot()
                && getParent().getParent() instanceof FusionPropertyCopy;

    }

    protected boolean isSingleLineDefinition() {
        return getParent() instanceof FusionPath
                && ((FusionPath) getParent()).isPrototypeClassProperty()
                && getParent().getParent() instanceof FusionPropertyAssignment
                && getParent().getParent().getParent() instanceof FusionFile;
    }

    protected boolean isSingleElementInPathAtFileRoot() {
        return getParent() instanceof FusionPath
                && ((FusionPath) getParent()).isPrototypeSignature()
                && getParent().getParent().getParent() instanceof FusionFile;
    }
}
