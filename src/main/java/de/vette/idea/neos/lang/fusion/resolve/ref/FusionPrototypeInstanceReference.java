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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionNamespaceDeclarationIndex;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FusionPrototypeInstanceReference extends FusionReferenceBase<FusionPrototypeInstance> implements FusionReference {

    public FusionPrototypeInstanceReference(FusionPrototypeInstance psiElement) {
        super(psiElement);
    }

    @Override
    List<FusionCompositeElement> resolveInner() {
        FusionType type = getElement().getType();
        if (type.getUnqualifiedType() == null) return new ArrayList<>();

        String instanceNs = null;
        String instanceName = type.getUnqualifiedType().getText();
        String instanceAliasNamespace = null;
        if (type.getObjectTypeNamespace() != null) {
            instanceNs = type.getObjectTypeNamespace().getText();
            instanceAliasNamespace = findNamespaceByAlias(instanceNs);
        }

        // find all prototypes that have the name of this instance
        Project project = getElement().getProject();
        List<FusionCompositeElement> result = new ArrayList<>();
        Collection<FusionPrototypeSignature> possiblePrototypes = StubIndex.getElements(
                FusionPrototypeDeclarationIndex.KEY,
                instanceName,
                project,
                GlobalSearchScope.projectScope(project),
                FusionPrototypeSignature.class);

        // check for each prototype if the namespace matches by resolving aliases
        for (FusionPrototypeSignature possiblePrototype : possiblePrototypes) {
            FusionType prototypeType = possiblePrototype.getType();
            if (prototypeType != null) {
                PsiElement prototypeNamespace = prototypeType.getObjectTypeNamespace();
                if (prototypeNamespace != null) {
                    // check if prototype has default namespace
                    if (instanceNs == null) {
                        if (prototypeNamespace.getText().equals("TYPO3.Neos")
                            || prototypeNamespace.getText().equals("Neos.Neos")) {
                            result.add(possiblePrototype);
                        }
                        continue;
                    }

                    String prototypeNs = prototypeType.getObjectTypeNamespace().getText();
                    if (prototypeNs.equals(instanceNs) || prototypeNs.equals(instanceAliasNamespace)) {
                        result.add(possiblePrototype);
                    } else {
                        prototypeNs = findNamespaceByAlias(prototypeNs);
                        if (instanceNs.equals(prototypeNs)) {
                            result.add(possiblePrototype);
                        }
                    }
                } else if (instanceNs == null
                        || (instanceNs.equals("TYPO3.Neos")
                        || instanceNs.equals("Neos.Neos"))) {

                    result.add(possiblePrototype);
                }
            }
        }

        return result;
    }

    @Override
    public TextRange getRangeInElement() {
        return new TextRange(myElement.getType().getStartOffsetInParent(), myElement.getType().getTextLength());
    }

    @Nullable
    private String findNamespaceByAlias(String alias) {
        Collection<FusionNamespaceDeclaration> namespaces = StubIndex.getElements(
                FusionNamespaceDeclarationIndex.KEY,
                alias,
                getElement().getProject(),
                GlobalSearchScope.projectScope(getElement().getProject()),
                FusionNamespaceDeclaration.class);

        if (!namespaces.isEmpty()) {
            FusionNamespace namespace = namespaces.iterator().next().getNamespace();
            if (namespace != null) {
                return namespace.getText();
            }
        }

        return null;
    }
}
