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

package de.vette.idea.neos.lang.fusion.editor;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.FusionBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionBreadcrumbsInfoProvider implements BreadcrumbsProvider {
    @Override
    public Language[] getLanguages() {
        return new Language[]{FusionLanguage.INSTANCE};
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement e) {
        return e instanceof de.vette.idea.neos.lang.fusion.psi.FusionBlock;
    }

    @NotNull
    @Override
    public String getElementInfo(@NotNull PsiElement e) {
        String elementInfo = "";
        if (e instanceof FusionBlock) {
            PsiElement currentElement = e;
            do {
                if (currentElement.getPrevSibling() == null) {
                    currentElement = currentElement.getParent();
                } else {
                    currentElement = currentElement.getPrevSibling();
                }
            } while (currentElement != null && !(currentElement instanceof FusionPath));

            if (currentElement != null) {
                elementInfo = currentElement.getText();

                if (currentElement.getFirstChild() instanceof FusionPrototypeSignature) {
                    FusionType type = ((FusionPrototypeSignature) currentElement.getFirstChild()).getType();
                    if (type != null) {
                        elementInfo = type.getText();
                    }
                }
            }
        }

        if (elementInfo.length() > 30) {
            elementInfo = "..." + elementInfo.substring(elementInfo.length() - 27, elementInfo.length());
        }
        return elementInfo;
    }

    @Nullable
    @Override
    public String getElementTooltip(@NotNull PsiElement e) {
        return "";
    }
}
