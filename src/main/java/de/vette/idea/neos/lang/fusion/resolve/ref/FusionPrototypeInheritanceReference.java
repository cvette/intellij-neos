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
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;

import java.util.List;

public class FusionPrototypeInheritanceReference extends FusionReferenceBase<FusionPrototypeSignature> {

    public FusionPrototypeInheritanceReference(FusionPrototypeSignature psiElement) {
        super(psiElement);
    }

    @Override
    List<PsiElement> resolveInner() {
        return ResolveEngine.getPrototypeDefinitions(getElement().getProject(), getElement().getType());
    }

    @Override
    public TextRange getRangeInElement() {
        if (myElement.getType() == null) {
            return myElement.getTextRange();
        }

        int startOffset = myElement.getType().getStartOffsetInParent();
        return new TextRange(startOffset, startOffset + myElement.getType().getTextLength());
    }
}
