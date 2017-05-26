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
package de.vette.idea.neos.lang.eel.resolve.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine;
import de.vette.idea.neos.lang.eel.psi.EelValueStringLineContent;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import de.vette.idea.neos.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.List;

public class EelStringReference extends EelReferenceBase<EelValueStringLine> implements EelReference{

    public EelStringReference(EelValueStringLine psiElement) {
        super(psiElement);
    }

    @Override
    List<PsiElement> resolveInner() {
        List<PsiElement> result = new ArrayList<>();
        if (getElement() != null && getElement().getValueStringLineContent() != null) {
            // find classes
            String value = getElement().getValueStringLineContent().getText();
            result = PhpElementsUtil.getClassInterfaceElements(getElement().getProject(), value.replace("\\\\", "\\"));
            if (!result.isEmpty()) {
                return result;
            }

            // find prototypes
            String[] splitText = getElement().getValueStringLineContent().getText().split(":");
            String instanceName;
            String instanceNs = null;

            if (splitText.length > 1) {
                instanceName = splitText[1];
                instanceNs = splitText[0];
            } else if (splitText.length > 0) {
                instanceName = splitText[0];
            } else {
                return result;
            }

            result = ResolveEngine.getPrototypeDefinitions(getElement().getProject(), instanceName, instanceNs);
        }

        return result;
    }

    @Override
    public TextRange getRangeInElement() {
        EelValueStringLineContent content = getElement().getValueStringLineContent();
        if (content != null) {
            return new TextRange(content.getStartOffsetInParent(), content.getStartOffsetInParent() + content.getTextLength());
        }

        return getElement().getTextRange();
    }
}
