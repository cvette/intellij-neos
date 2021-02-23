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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import de.vette.idea.neos.indexes.DefaultContextFileIndex;
import de.vette.idea.neos.lang.fusion.psi.FusionCompositeIdentifier;
import de.vette.idea.neos.lang.fusion.psi.FusionMethodCall;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EelHelperMethodAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof FusionMethodCall) {
            FusionMethodCall methodCall = (FusionMethodCall) element;
            if (methodCall.getPrevSibling() != null && methodCall.getPrevSibling().getPrevSibling() instanceof FusionCompositeIdentifier) {
                FusionCompositeIdentifier compositeId = (FusionCompositeIdentifier) methodCall.getPrevSibling().getPrevSibling();
                List<String> helpers = FileBasedIndex.getInstance().getValues(DefaultContextFileIndex.KEY, compositeId.getText(), GlobalSearchScope.allScope(element.getProject()));
                if (!helpers.isEmpty()) {
                    for (String helper : helpers) {
                         if (PhpElementsUtil.getClassMethod(element.getProject(), helper, methodCall.getMethodName().getText()) != null) {
                             return;
                         }
                    }

                    holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved EEL helper method").create();
                }
            }
        }
    }
}
