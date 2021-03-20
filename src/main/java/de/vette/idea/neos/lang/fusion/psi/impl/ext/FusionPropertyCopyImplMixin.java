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
import com.intellij.psi.util.PsiTreeUtil;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.vette.idea.neos.lang.fusion.psi.FusionTypes.*;

public class FusionPropertyCopyImplMixin extends FusionElementImpl implements FusionPropertyCopy {

    public FusionPropertyCopyImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    public boolean isPrototypeInheritance() {
        return getParent() instanceof FusionFile && getPath().isPrototypeSignature() && getCopiedPrototypeSignature() != null;
    }

    @Override
    @Nullable
    public FusionBlock getBlock() {
        return PsiTreeUtil.getChildOfType(this, FusionBlock.class);
    }

    @Override
    @NotNull
    public FusionPath getPath() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, FusionPath.class));
    }

    @Override
    @Nullable
    public FusionPrototypeInstance getPrototypeInstance() {
        return PsiTreeUtil.getChildOfType(this, FusionPrototypeInstance.class);
    }

    @Override
    @Nullable
    public FusionCopiedPrototypeSignature getCopiedPrototypeSignature() {
        return PsiTreeUtil.getChildOfType(this, FusionCopiedPrototypeSignature.class);
    }

    @Override
    @Nullable
    public FusionValueStringLine getValueStringLine() {
        return PsiTreeUtil.getChildOfType(this, FusionValueStringLine.class);
    }

    @Override
    @NotNull
    public PsiElement getCopyOperator() {
        return notNullChild(findChildByType(COPY_OPERATOR));
    }

    @Override
    @Nullable
    public PsiElement getValueBoolean() {
        return findChildByType(VALUE_BOOLEAN);
    }

    @Override
    @Nullable
    public PsiElement getValueNull() {
        return findChildByType(VALUE_NULL);
    }

    @Override
    @Nullable
    public PsiElement getValueNumber() {
        return findChildByType(VALUE_NUMBER);
    }
}
