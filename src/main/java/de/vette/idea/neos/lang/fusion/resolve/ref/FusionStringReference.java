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
import de.vette.idea.neos.lang.fusion.psi.FusionValueStringLine;
import de.vette.idea.neos.lang.fusion.psi.FusionValueStringLineContent;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;

import java.util.ArrayList;
import java.util.List;

public class FusionStringReference extends FusionReferenceBase<FusionValueStringLine> implements FusionReference{

    public FusionStringReference(FusionValueStringLine psiElement) {
        super(psiElement);
    }

    @Override
    List<PsiElement> resolveInner() {
        List<PsiElement> helpers = new ArrayList<>();
        if (getElement() != null && getElement().getValueStringLineContent() != null) {
            String[] splitText = getElement().getValueStringLineContent().getText().split(":");
            String instanceName;
            String instanceNs = null;

            if (splitText.length > 1) {
                instanceName = splitText[1];
                instanceNs = splitText[0];
            } else if (splitText.length > 0) {
                instanceName = splitText[0];
            } else {
                return helpers;
            }

            helpers = ResolveEngine.getPrototypeDefinitions(getElement().getProject(), instanceName, instanceNs);
        }

        return helpers;
    }

    @Override
    public TextRange getRangeInElement() {
        FusionValueStringLineContent content = getElement().getValueStringLineContent();
        if (content != null) {
            return new TextRange(content.getStartOffsetInParent(), content.getStartOffsetInParent() + content.getTextLength());
        }

        return getElement().getTextRange();
    }
}
