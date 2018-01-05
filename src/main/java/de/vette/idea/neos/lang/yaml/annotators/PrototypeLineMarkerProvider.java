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
package de.vette.idea.neos.lang.yaml.annotators;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.NeosProjectComponent;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collection;
import java.util.List;

public class PrototypeLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        for (PsiElement el : elements) {
            if (!NeosProjectComponent.isEnabled(el)) {
                return;
            }

            if (!el.getContainingFile().getVirtualFile().getName().startsWith("NodeTypes.")) {
                continue;
            }

            if (!(el instanceof YAMLKeyValue)) {
                continue;
            }

            if (((YAMLKeyValue) el).getParentMapping().getParent() instanceof YAMLDocument) {
                String nodeType = ((YAMLKeyValue) el).getKeyText();
                String[] nodeTypeSplit = nodeType.split(":");

                if (nodeTypeSplit.length < 2) {
                    continue;
                }

                List<PsiElement> targets = ResolveEngine.getPrototypeDefinitions(el.getProject(), nodeTypeSplit[1], nodeTypeSplit[0]);
                if (!targets.isEmpty()) {
                    RelatedItemLineMarkerInfo info = NavigationGutterIconBuilder
                            .create(FusionIcons.PROTOTYPE)
                            .setTargets(targets)
                            .setTooltipText("Go to Fusion prototype")
                            .createLineMarkerInfo(el);
                    result.add(info);
                }
            }
        }
    }
}
