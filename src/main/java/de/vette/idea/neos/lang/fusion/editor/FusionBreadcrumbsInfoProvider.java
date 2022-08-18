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
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.FusionBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FusionBreadcrumbsInfoProvider implements BreadcrumbsProvider {
    @Override
    public Language[] getLanguages() {
        return new Language[]{FusionLanguage.INSTANCE};
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement e) {
        return e instanceof FusionSinglePath || e instanceof FusionMetaProperty || e instanceof FusionPrototypeSignature;
    }

    @Override
    public @Nullable PsiElement getParent(@NotNull PsiElement e) {
        if (e instanceof FusionBlock && e.getParent() instanceof FusionPropertyBlock) {
            FusionPath path = ((FusionPropertyBlock) e.getParent()).getPath();
            return path.getLastChild();
        }

        if (e instanceof FusionBlock && e.getParent() instanceof FusionPropertyCopy) {
            FusionPath path = ((FusionPropertyCopy) e.getParent()).getPath();
            return path.getLastChild();
        }

        if (e instanceof FusionAssignmentValue) {
            FusionPath path = ((FusionPropertyAssignment) e.getParent()).getPath();
            return path.getLastChild();
        }

        if (e instanceof FusionSinglePath || e instanceof FusionMetaProperty || e instanceof FusionPrototypeSignature) {
            if (e.getPrevSibling() != null
                    && (e.getPrevSibling().getPrevSibling() instanceof FusionSinglePath
                    || e.getPrevSibling().getPrevSibling() instanceof FusionMetaProperty
                    || e.getPrevSibling().getPrevSibling() instanceof FusionPrototypeSignature)) {
                return e.getPrevSibling().getPrevSibling();
            }
        }

        return e.getParent();
    }

    @Override
    public @Nullable Icon getElementIcon(@NotNull PsiElement e) {
        if (e instanceof FusionMetaProperty) {
            return FusionIcons.META;
        }

        if (e instanceof FusionPrototypeSignature) {
            return FusionIcons.PROTOTYPE;
        }

        return null;
    }

    @NotNull
    @Override
    public String getElementInfo(@NotNull PsiElement e) {
        return getElementName(e, true);
    }

    @NotNull
    @Override
    public String getElementTooltip(@NotNull PsiElement e) {
        return getElementName(e, false);
    }

    private String getElementName(@NotNull PsiElement e, boolean truncate) {
        String elementInfo = e.getText();

        if (e instanceof FusionPrototypeSignature) {
            if (truncate) {
                FusionType type = ((FusionPrototypeSignature) e).getType();
                if (type != null) {
                    elementInfo = type.getText();
                }
            }
        }

        if (truncate && elementInfo.length() > 30) {
            elementInfo = "..." + elementInfo.substring(elementInfo.length() - 27);
        }

        return elementInfo;
    }

    @Override
    public @NotNull List<? extends Action> getContextActions(@NotNull PsiElement element) {
        List<String> parts = new ArrayList<>();
        var current = element;
        do {
            if (!acceptElement(current)) {
                current = getParent(current);
                continue;
            }

            var part = getElementName(current, false);
            if (!part.isEmpty()) {
                parts.add(part);
            }

            current = getParent(current);
        } while (current != null);

        Collections.reverse(parts);
        String path = String.join(".", parts);

        return Collections.singletonList(new AbstractAction("Copy Fusion path") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(path));
            }
        });
    }
}
