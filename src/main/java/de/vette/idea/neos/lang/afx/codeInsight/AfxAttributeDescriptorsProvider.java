package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlAttributeDescriptorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds AFX attributes for tags whose element descriptor is not already an {@link AfxTagDescriptor}.
 * Resolved component tags get their attributes from that descriptor; HTML tags and unresolved
 * component tags get them here, so the two sources are not listed twice.
 */
public class AfxAttributeDescriptorsProvider implements XmlAttributeDescriptorsProvider, DumbAware {
    private static final XmlAttributeDescriptor[] EMPTY = new XmlAttributeDescriptor[0];

    @Override
    public XmlAttributeDescriptor @NotNull [] getAttributeDescriptors(@Nullable XmlTag context) {
        if (!AfxAttributes.isAfx(context) || suppliedByTagDescriptor(context)) {
            return EMPTY;
        }
        return AfxAttributes.forTag(context);
    }

    @Override
    public @Nullable XmlAttributeDescriptor getAttributeDescriptor(@NotNull String attributeName, @Nullable XmlTag context) {
        if (!AfxAttributes.isAfx(context) || suppliedByTagDescriptor(context)) {
            return null;
        }
        return AfxAttributes.find(context, attributeName);
    }

    /**
     * {@link AfxTagProvider} only builds a descriptor when exactly one prototype matches.
     * Asking the tag for its descriptor in dumb mode would touch the stub index.
     */
    private static boolean suppliedByTagDescriptor(@NotNull XmlTag context) {
        if (DumbService.isDumb(context.getProject())) {
            return false;
        }
        return context.getDescriptor() instanceof AfxTagDescriptor;
    }
}
