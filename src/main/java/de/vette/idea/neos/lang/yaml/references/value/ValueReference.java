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

package de.vette.idea.neos.lang.yaml.references.value;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.List;

public class ValueReference extends PsiPolyVariantReferenceBase<YAMLValue> {
    private final YAMLValue yamlElement;


    public ValueReference(YAMLValue yamlElement) {
        // the "textRange" is used for highlighting the source element
        super(yamlElement, new TextRange(0, yamlElement.getTextLength()));
        this.yamlElement = yamlElement;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String value;
        value = yamlElement.getText().replaceAll("^\"|\"$", "");
        value = value.replaceAll("^\'|\'$", "");

        if (value.startsWith("resource://")) {
            VirtualFile resourceFile = ResolveEngine.findResource(yamlElement.getContainingFile(), value);
            if (resourceFile != null && !resourceFile.isDirectory()) {
                PsiFile resourcePsiFile = PsiManager.getInstance(getElement().getProject()).findFile(resourceFile);
                if (resourcePsiFile != null) {
                    return new PsiElementResolveResult[] {
                            new PsiElementResolveResult(resourcePsiFile)
                    };
                }
            }
        }

        if (value.contains("\\")) {
            List<PsiElement> phpClassDefinitions = PhpElementsUtil.getClassInterfaceElements(yamlElement.getProject(), yamlElement.getText());
            return phpClassDefinitions
                    .stream()
                    .map(classDefinition -> new PsiElementResolveResult(classDefinition, true))
                    .toArray(PsiElementResolveResult[]::new);
        }

        return new ResolveResult[0];
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
