package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.fusion.psi.FusionCopiedPrototypeSignature;
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyAssignment;
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyCopy;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import de.vette.idea.neos.lang.fusion.psi.FusionType;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPropertyAssignmentIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attributes offered on an AFX tag.
 * <p>
 * Simple properties declared on the tag's prototype, and on prototypes it inherits, come from
 * {@link FusionPropertyAssignmentIndex}. {@code renderer} is omitted: almost every component assigns it
 * to an {@code afx} block, and it is not a prop callers pass. Names starting with {@code @} are Fusion
 * meta paths, not AFX props. The fixed AFX attributes {@code @path}, {@code @children} and {@code @key}
 * are always offered.
 */
public final class AfxAttributes {
    static final String[] META_ATTRIBUTES = {"@path", "@children", "@key"};

    private static final XmlAttributeDescriptor[] EMPTY = new XmlAttributeDescriptor[0];
    private static final Key<CachedValue<XmlAttributeDescriptor[]>> CACHE_KEY = Key.create("afx.attributeDescriptors");
    private static final int MAX_INHERITANCE_DEPTH = 16;

    private AfxAttributes() {
    }

    static boolean isAfx(@Nullable XmlTag tag) {
        return tag != null && tag.getContainingFile().getLanguage() instanceof AfxLanguage;
    }

    static XmlAttributeDescriptor @NotNull [] forTag(@NotNull XmlTag tag) {
        return CachedValuesManager.getManager(tag.getProject()).getCachedValue(tag, CACHE_KEY, () -> {
            XmlAttributeDescriptor[] descriptors = compute(tag);
            return CachedValueProvider.Result.create(descriptors, PsiModificationTracker.MODIFICATION_COUNT);
        }, false);
    }

    @Nullable
    static XmlAttributeDescriptor find(@NotNull XmlTag tag, @NotNull String attributeName) {
        for (XmlAttributeDescriptor descriptor : forTag(tag)) {
            if (attributeName.equals(descriptor.getName())) {
                return descriptor;
            }
        }
        return null;
    }

    private static XmlAttributeDescriptor[] compute(@NotNull XmlTag tag) {
        List<XmlAttributeDescriptor> descriptors = new ArrayList<>();
        Map<String, PsiElement> properties = new LinkedHashMap<>();
        Project project = tag.getProject();
        if (!DumbService.isDumb(project)) {
            addIndexedProperties(project, tag.getName(), properties);
            String[] parts = tag.getName().split(":", 2);
            if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                collectPrototypeProperties(project, parts[0], parts[1], new HashSet<>(), properties, 0);
            }
        }

        for (Map.Entry<String, PsiElement> entry : properties.entrySet()) {
            descriptors.add(new AfxAttributeDescriptor(entry.getKey(), entry.getValue()));
        }
        for (String meta : META_ATTRIBUTES) {
            descriptors.add(new AfxAttributeDescriptor(meta, null));
        }
        return descriptors.toArray(EMPTY);
    }

    private static void collectPrototypeProperties(
            Project project,
            @Nullable String namespace,
            @NotNull String name,
            Set<String> visited,
            Map<String, PsiElement> properties,
            int depth
    ) {
        if (depth > MAX_INHERITANCE_DEPTH || name.isEmpty()) {
            return;
        }

        List<PsiElement> definitions = ResolveEngine.getPrototypeDefinitions(project, name, namespace);
        for (PsiElement element : definitions) {
            if (!(element instanceof FusionPrototypeSignature signature)) {
                continue;
            }
            FusionType type = signature.getType();
            if (type == null || !visited.add(type.getText())) {
                continue;
            }

            addIndexedProperties(project, type.getText(), properties);

            FusionType parent = parentPrototype(signature);
            if (parent == null) {
                continue;
            }
            String parentNamespace = parent.getObjectTypeNamespace() != null ? parent.getObjectTypeNamespace().getText() : null;
            collectPrototypeProperties(project, parentNamespace, parent.getUnqualifiedType().getText(), visited, properties, depth + 1);
        }
    }

    /**
     * The prototype on the right-hand side of {@code prototype(Child) < prototype(Parent)}.
     */
    @Nullable
    private static FusionType parentPrototype(FusionPrototypeSignature signature) {
        if (!signature.isInheritanceDefinition()) {
            return null;
        }
        PsiElement path = signature.getParent();
        if (path == null || !(path.getParent() instanceof FusionPropertyCopy copy)) {
            return null;
        }
        FusionCopiedPrototypeSignature copied = copy.getCopiedPrototypeSignature();
        if (copied == null) {
            return null;
        }
        return copied.getType();
    }

    private static void addIndexedProperties(Project project, @Nullable String prototypeName, Map<String, PsiElement> properties) {
        if (prototypeName == null || prototypeName.isEmpty()) {
            return;
        }

        Collection<FusionPropertyAssignment> assignments = StubIndex.getElements(
                FusionPropertyAssignmentIndex.KEY,
                prototypeName,
                project,
                GlobalSearchScope.projectScope(project),
                FusionPropertyAssignment.class
        );
        for (FusionPropertyAssignment assignment : assignments) {
            String attributeName = assignment.getName();
            if (!isPassableProperty(attributeName) || properties.containsKey(attributeName)) {
                continue;
            }
            PsiElement declaration = assignment.getPath();
            properties.put(attributeName, declaration != null ? declaration : assignment);
        }
    }

    /**
     * Public props a caller can set as an AFX attribute.
     * Nested paths are not stored in the property index, and {@code renderer} is the component body.
     */
    private static boolean isPassableProperty(@Nullable String name) {
        if (name == null || name.isEmpty() || name.charAt(0) == '@') {
            return false;
        }
        if ("renderer".equals(name)) {
            return false;
        }
        return name.indexOf('.') < 0 && name.indexOf(' ') < 0 && name.indexOf('<') < 0;
    }
}
