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

import com.intellij.json.psi.JsonFile;
import com.intellij.openapi.project.Project;
import static com.intellij.openapi.project.ProjectUtil.guessProjectDir;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.indexing.FileBasedIndex;
import de.vette.idea.neos.indexes.DefaultContextFileIndex;
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionNamespaceDeclarationIndex;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import de.vette.idea.neos.util.ComposerUtil;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jetbrains.php.lang.psi.elements.Method;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResolveEngine {
    protected final static Pattern RESOURCE_PATTERN =
            Pattern.compile("^resource://([^/]+)/(.*)");

    public static Collection<PsiElement> getNodeTypeDefinitions(Project project, String name, @Nullable String namespace) {
        String fqn = "Neos.Neos" + ":" + name;
        if (namespace != null) {
            String aliasNamespace = findNamespaceByAlias(project, namespace);
            if (aliasNamespace != null) {
                fqn = aliasNamespace + ":" + name;
            } else {
                fqn = namespace + ":" + name;
            }
        }

        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(NodeTypesYamlFileIndex.KEY, fqn, GlobalSearchScope.allScope(project));

        final String finalFqn = fqn;
        return files
                .stream()
                // get the PSI for each file
                .map(file -> PsiManager.getInstance(project).findFile(file))
                // ensure we only have YAML files
                .filter(psiFile -> psiFile instanceof YAMLFile)
                .map(psiFile -> (YAMLFile) psiFile)
                // get all YAML keys in these files
                .flatMap(yamlFile -> YAMLUtil.getTopLevelKeys(yamlFile).stream())
                // get the correct YAML key
                .filter(yamlKeyValue -> yamlKeyValue.getKeyText().equals(finalFqn))
                .collect(Collectors.toList());
    }

    @NotNull
    public static Collection<PsiElement> getNodeTypeDefinitions(Project project, FusionType type) {
        String instanceName = type.getUnqualifiedType().getText();
        String instanceNs = null;
        if (type.getObjectTypeNamespace() != null) {
            instanceNs = type.getObjectTypeNamespace().getText();
        }

        return ResolveEngine.getNodeTypeDefinitions(project, instanceName, instanceNs);
    }

    public static List<PsiElement> getPrototypeDefinitions(Project project, String name, @Nullable String namespace) {
        String instanceAliasNamespace = null;
        if (namespace != null) {
            instanceAliasNamespace = findNamespaceByAlias(project, namespace);
        }

        // find all prototypes that have the name of this instance
        List<PsiElement> result = new ArrayList<>();
        Collection<FusionPrototypeSignature> possiblePrototypes = StubIndex.getElements(
                FusionPrototypeDeclarationIndex.KEY,
                name,
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
                    if (namespace == null) {
                        if (prototypeNamespace.getText().equals("Neos.Neos")) {
                            result.add(possiblePrototype);
                        }
                        continue;
                    }

                    String prototypeNs = prototypeType.getObjectTypeNamespace().getText();
                    if (prototypeNs.equals(namespace) || prototypeNs.equals(instanceAliasNamespace)) {
                        result.add(possiblePrototype);
                    } else {
                        prototypeNs = findNamespaceByAlias(project, prototypeNs);
                        if (namespace.equals(prototypeNs)) {
                            result.add(possiblePrototype);
                        }
                    }
                } else if (namespace == null || namespace.equals("Neos.Neos")) {
                    result.add(possiblePrototype);
                }
            }
        }

        // If one of the results is a prototype inheritance, return it as the only result
        for (PsiElement resultPrototype : result) {
            if (resultPrototype instanceof FusionPrototypeSignature
                    && ((FusionPrototypeSignature)resultPrototype).isInheritanceDefinition()) {
                result.clear();
                result.add(resultPrototype);
                return result;
            }
        }

        return result;
    }

    @NotNull
    public static List<PsiElement> getPrototypeDefinitions(Project project, FusionType type) {
        String instanceName = type.getUnqualifiedType().getText();
        String instanceNs = null;
        if (type.getObjectTypeNamespace() != null) {
            instanceNs = type.getObjectTypeNamespace().getText();
        }

        return ResolveEngine.getPrototypeDefinitions(project, instanceName, instanceNs);
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

    @Nullable
    public static VirtualFile findResource(PsiFile file, String resourcePath) {
        Matcher m = RESOURCE_PATTERN.matcher(resourcePath);
        if (m.matches()) {
            resourcePath = "Resources/" + m.group(2);
            VirtualFile packagesDir = findPackagesDirectory(file);
            if (packagesDir == null) {
                VirtualFile projectDir = guessProjectDir(file.getProject());
                if (projectDir == null) {
                    return null;
                }

                return projectDir.findFileByRelativePath(resourcePath);
            } else {
                VirtualFile packageDir = findPackageDirectory(packagesDir, m.group(1));
                if (packageDir != null) {
                    return packageDir.findFileByRelativePath(resourcePath);
                }
            }
        }

        return null;
    }

    @Nullable
    protected static VirtualFile findPackageDirectory(VirtualFile packagesDirectory, String packageName) {
        VirtualFile packageDir;
        for (VirtualFile packageChild : packagesDirectory.getChildren()) {
            if (packageChild.isDirectory()) {
                packageDir = packageChild.findChild(packageName);
                if (packageDir != null) {
                    return packageDir;
                }
            }
        }

        return null;
    }

    @Nullable
    protected static VirtualFile findPackagesDirectory(PsiFile file) {
        VirtualFile projectDir = guessProjectDir(file.getProject());
        VirtualFile currentFile = file.getVirtualFile();
        while (currentFile != null && !currentFile.equals(projectDir)) {
            currentFile = currentFile.getParent();
            if (currentFile != null && currentFile.isDirectory() && currentFile.getName().equals("Packages")) {
                return currentFile;
            }
        }

        return null;
    }

    public static List<PsiElement> getEelHelpers(Project project, String name) {
        List<PsiElement> result = new ArrayList<>();
        List<String> helpers = FileBasedIndex.getInstance().getValues(DefaultContextFileIndex.KEY, name, GlobalSearchScope.allScope(project));
        for (String helper : helpers) {
            result.addAll(PhpElementsUtil.getClassInterfaceElements(project, helper));
        }

        return result;
    }

    public static List<PsiElement> getEelHelperMethods(Project project, String helperName, String methodName) {
        List<String> helpers = FileBasedIndex.getInstance().getValues(DefaultContextFileIndex.KEY, helperName, GlobalSearchScope.allScope(project));
        List<PsiElement> methods = new ArrayList<>();
        for (String helper : helpers) {
            Method method = PhpElementsUtil.getClassMethod(project, helper, methodName);
            if (method != null) {
                methods.add(method);
            }
        }

        return methods;
    }

    /**
     * Find template for controller action
     *
     * @param method Controller action method
     * @return VirtualFile
     */
    public static VirtualFile findTemplate(Method method) {
        PsiFile file = method.getContainingFile();
        if (method.getContainingClass() != null) {
            String actionName = method.getName();
            if (!actionName.endsWith("Action")) {
                return null;
            }

            actionName = actionName.replace("Action", "");
            if (actionName.length() < 2) {
                return null;
            }

            actionName = actionName.substring(0, 1).toUpperCase() + actionName.substring(1);

            String controllerName = method.getContainingClass().getName();
            controllerName = controllerName.replace("Controller", "");

            JsonFile composerFile = ComposerUtil.getComposerManifest(file.getContainingDirectory());
            if (composerFile != null) {
                String namespace = method.getNamespaceName();
                namespace = namespace.substring(1);

                Map<String, String> namespaceMappings = ComposerUtil.getNamespaceMappings(composerFile);
                for(String key : namespaceMappings.keySet()) {
                    if (namespace.startsWith(key)) {
                        namespace = namespace.replace(key, "")
                            .replace("\\", "/");

                        if (namespace.startsWith("/") && namespace.length() > 1) {
                            namespace = namespace.substring(1);
                        }

                        namespace = namespace.replace("Controller/", "");
                        break;
                    }
                }

                String resourceFile = "Resources/Private/Templates/" + namespace + controllerName + "/" + actionName + ".html";
                return composerFile.getContainingDirectory().getVirtualFile().findFileByRelativePath(resourceFile);
            }
        }

        return null;
    }
}
