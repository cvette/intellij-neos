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

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import de.vette.idea.neos.lang.fusion.psi.FusionCompositeElement;
import de.vette.idea.neos.lang.fusion.psi.FusionReferenceElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class FusionReferenceBase<T extends FusionReferenceElement> extends PsiPolyVariantReferenceBase<T> implements FusionReference {

    public FusionReferenceBase(T psiElement) {
        super(psiElement);
    }

    abstract List<FusionCompositeElement> resolveInner();

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return ResolveCache.getInstance(myElement.getProject())
                .resolveWithCaching(this, (r, incomplete) -> {
                    List<FusionCompositeElement> elements = r.resolveInner();
                    List<ResolveResult> resolveResults = new ArrayList<>();
                    ResolveResult[] result = new ResolveResult[elements.size()];
                    for (FusionCompositeElement e : elements) {
                        resolveResults.add(new PsiElementResolveResult(e));
                    }

                    return resolveResults.toArray(result);
                }, true, true);
    }

    @Nullable
    @Override
    public FusionCompositeElement resolve() {
        return (FusionCompositeElement) super.resolve();
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
