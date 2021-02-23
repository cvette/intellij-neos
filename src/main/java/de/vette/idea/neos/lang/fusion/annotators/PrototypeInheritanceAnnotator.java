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
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyCopy;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import org.jetbrains.annotations.NotNull;

public class PrototypeInheritanceAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof FusionPropertyCopy) {
            FusionPropertyCopy copy = (FusionPropertyCopy) element;
            if (copy.isPrototypeInheritance()
                    && !(element.getParent() instanceof FusionFile)
                    || !copy.isPrototypeInheritance()
                    && copy.getPath().getLastChild() instanceof FusionPrototypeSignature
                    && copy.getPrototypeSignature() != null) {

                holder.newAnnotation(HighlightSeverity.ERROR, "Prototype inheritance can only be defined globally").create();
            }
        }
    }
}
