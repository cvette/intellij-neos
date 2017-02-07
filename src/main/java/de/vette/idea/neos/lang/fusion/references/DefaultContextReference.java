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

package de.vette.idea.neos.lang.fusion.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DefaultContextReference extends PsiPolyVariantReferenceBase<PsiElement> {

    protected String contextName;
    protected String className;

    public DefaultContextReference(PsiElement psiElement, String className) {
        super(psiElement);
        this.contextName = psiElement.getText();
        this.className = className;
    }

    @Override
    @NotNull
    public ResolveResult[] multiResolve(boolean b) {
        List<ResolveResult> resolveResults = new ArrayList<ResolveResult>();
        resolveResults.addAll(PhpElementsUtil.getClassInterfaceResolveResult(getElement().getOriginalElement().getProject(), this.className));
        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getElement().getTextLength());
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        return super.isReferenceTo(element);
    }

    @Override
    public PsiElement getElement() {
        return super.getElement();
    }

    @Override
    protected TextRange calculateDefaultRangeInElement() {
        return new TextRange(0, myElement.getText().length());
    }
}
