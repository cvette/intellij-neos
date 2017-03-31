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

package de.vette.idea.neos.lang.fusion.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionNamespaceDeclarationIndex;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResolveEngine {
    protected final static Pattern RESOURCE_PATTERN =
            Pattern.compile("^resource://([^/]+)/(.*)");


    @NotNull
    public static List<PsiElement> getPrototypeDefinitions(Project project, FusionType type) {
        if (type.getUnqualifiedType() == null) return new ArrayList<>();

        String instanceNs = null;
        String instanceName = type.getUnqualifiedType().getText();
        String instanceAliasNamespace = null;
        if (type.getObjectTypeNamespace() != null) {
            instanceNs = type.getObjectTypeNamespace().getText();
            instanceAliasNamespace = findNamespaceByAlias(project, instanceNs);
        }

        // find all prototypes that have the name of this instance
        List<PsiElement> result = new ArrayList<>();
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
                        prototypeNs = findNamespaceByAlias(project, prototypeNs);
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

        // If one of the results is a prototype inheritance, return it as the only result
        for (PsiElement resultPrototype : result) {
            if (resultPrototype.getParent() != null && resultPrototype.getParent() instanceof FusionPrototypeInheritance) {
                result.clear();
                result.add(resultPrototype);
                return result;
            }
        }

        return result;
    }

    @Nullable
    private static String findNamespaceByAlias(Project project, String alias) {
        Collection<FusionNamespaceDeclaration> namespaces = StubIndex.getElements(
                FusionNamespaceDeclarationIndex.KEY,
                alias,
                project,
                GlobalSearchScope.projectScope(project),
                FusionNamespaceDeclaration.class);

        if (!namespaces.isEmpty()) {
            FusionNamespace namespace = namespaces.iterator().next().getNamespace();
            if (namespace != null) {
                return namespace.getText();
            }
        }

        return null;
    }

    public static VirtualFile findResource(Project project, String resourcePath) {
        Matcher m = RESOURCE_PATTERN.matcher(resourcePath);
        if (m.matches()) {
            Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, m.group(1), GlobalSearchScope.projectScope(project));
            VirtualFile baseDir = project.getBaseDir().findChild("Packages");
            if (baseDir == null) {
                return null;
            }

            resourcePath = "Resources/" + m.group(2);
            for (VirtualFile file : files) {
                if (file.getPath().startsWith(baseDir.getPath())) {
                    return file.findFileByRelativePath(resourcePath);
                }
            }
        }

        return null;
    }
}
