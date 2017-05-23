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
package de.vette.idea.neos.lang.fusion.annotators;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.NeosIcons;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class NodeTypeLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        for (PsiElement el : elements) {
            if (!(el instanceof FusionPropertyBlock) && !(el instanceof FusionPrototypeInheritance)) {
                continue;
            }

            FusionType type = null;
            if (el instanceof FusionPropertyBlock) {
                FusionPath path = ((FusionPropertyBlock) el).getPath();
                if (path.getFirstChild() instanceof FusionPrototypeSignature
                        && (el.getLastChild() instanceof FusionBlock
                        || path.getLastChild() instanceof FusionPrototypeSignature)) {
                    FusionPrototypeSignature signature = (FusionPrototypeSignature) path.getFirstChild();
                    type = signature.getType();
                }
            } else {
                type = ((FusionPrototypeSignature)el.getFirstChild()).getType();
            }

            if (type != null) {
                Collection<PsiElement> targets = ResolveEngine.getNodeTypeDefinitions(el.getProject(), type);
                if (!targets.isEmpty()) {
                    RelatedItemLineMarkerInfo info = NavigationGutterIconBuilder
                            .create(NeosIcons.NODE_TYPE)
                            .setTargets(targets)
                            .setTooltipText("Go to node type definition")
                            .createLineMarkerInfo(el);
                    result.add(info);
                }
            }
        }
    }
}
