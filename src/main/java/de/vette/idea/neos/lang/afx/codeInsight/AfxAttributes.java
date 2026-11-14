package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.fusion.resolve.FusionPrototypeProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Attributes offered on an AFX tag.
 * <p>
 * Simple properties declared on the tag's prototype, and on prototypes it inherits, come from
 * {@link FusionPrototypeProperties}. {@code renderer} is omitted: almost every component assigns it
 * to an {@code afx} block, and it is not a prop callers pass. Names starting with {@code @} are Fusion
 * meta paths, not AFX props. The fixed AFX attributes {@code @path}, {@code @children} and {@code @key}
 * are always offered.
 */
public final class AfxAttributes {
    static final String[] META_ATTRIBUTES = {"@path", "@children", "@key"};

    private static final XmlAttributeDescriptor[] EMPTY = new XmlAttributeDescriptor[0];
    private static final Key<CachedValue<XmlAttributeDescriptor[]>> CACHE_KEY = Key.create("afx.attributeDescriptors");

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
        Project project = tag.getProject();
        if (!DumbService.isDumb(project)) {
            for (FusionPrototypeProperties.Property property : FusionPrototypeProperties.forPrototype(project, tag.getName()).values()) {
                if (isPassableProperty(property.getName())) {
                    descriptors.add(new AfxAttributeDescriptor(property.getName(), property.getDeclaration()));
                }
            }
        }

        for (String meta : META_ATTRIBUTES) {
            descriptors.add(new AfxAttributeDescriptor(meta, null));
        }
        return descriptors.toArray(EMPTY);
    }

    /**
     * {@code renderer} is the component body and not a prop a caller passes.
     */
    private static boolean isPassableProperty(String name) {
        return !"renderer".equals(name);
    }
}
