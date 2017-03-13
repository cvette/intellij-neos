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

package de.vette.idea.neos.lang.fusion.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class FusionTreeElement extends PsiTreeElementBase<PsiElement> {

    FusionTreeElement(PsiElement element) {
        super(element);
    }

    @Override
    public String getLocationString() {
        if (getElement() != null && getElement() instanceof FusionFile) {
            FusionFile fusionFile = (FusionFile) getElement();
            if (fusionFile.getContainingDirectory() != null) {
                if (fusionFile.getContainingDirectory().getPresentation() != null) {
                    return fusionFile.getContainingDirectory().getPresentation().getLocationString();
                }
            }
        }

        return "";
    }

    @NotNull
    @Override
    public Collection<StructureViewTreeElement> getChildrenBase() {
        Collection<StructureViewTreeElement> result = new ArrayList<StructureViewTreeElement>();

        if (getElement() == null) {
            return Collections.emptyList();
        }

        PsiElement blockElement = getBlockElement();
        if (blockElement == null) {
            return Collections.emptyList();
        }

        for (PsiElement element : blockElement.getChildren()) {
            if (element instanceof FusionPropertyAssignment
                    || element instanceof FusionPropertyBlock
                    || element instanceof FusionPropertyCopy
                    || element instanceof FusionPropertyDeletion
                    || element instanceof FusionPrototypeInheritance) {
                if (element.getFirstChild() instanceof FusionPath) {
                    result.add(new FusionTreeElement(element.getFirstChild()));
                }
            }
        }

        return result;
    }

    @Nullable
    private PsiElement getBlockElement() {
        PsiElement blockElement = null;
        if (getElement() instanceof FusionFile) {
            return getElement();
        } else if (getElement() instanceof FusionPath) {
            PsiElement currentSibling = getElement();
            do {
                currentSibling = currentSibling.getNextSibling();
            } while (currentSibling != null && !(currentSibling instanceof FusionBlock));

            blockElement = currentSibling;
        }

        return blockElement;
    }

    @Nullable
    @Override
    public String getPresentableText() {
        if (getElement() == null) {
            return "Path";
        }

        if (getElement() instanceof FusionFile) {
            return ((FusionFile) getElement()).getName();
        }

        if (getElement().getFirstChild() != null
                && getElement().getFirstChild() instanceof FusionPrototypeSignature) {
            return ((FusionNamedElement)getElement()).getName();
        }

        return getElement().getText();
    }

    @Override
    public Icon getIcon(boolean open) {
        if (getElement() == null) {
            return FusionIcons.PATH;
        }

        if (getElement() instanceof FusionFile) {
            return FusionIcons.FILE;
        }

        if (getElement() instanceof FusionPath) {
            if (getElement().getParent() != null && getElement().getParent() instanceof FusionPropertyDeletion) {
                return FusionIcons.UNSET;
            }

            for (PsiElement part : getElement().getChildren()) {
                if (part instanceof FusionMetaProperty) {
                    return FusionIcons.META;
                } else if (part instanceof FusionPrototypeSignature) {
                    return FusionIcons.PROTOTYPE;
                }
            }
        }

        if (getElement().getFirstChild() instanceof FusionPrototypeSignature) {
            return FusionIcons.PROTOTYPE;
        }

        return FusionIcons.PATH;
    }

    @Override
    public boolean canNavigate() {
        return true;
    }
}
