package de.vette.idea.neos.lang.afx;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.xml.TagNameReference;
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is registered through plugin.xml as xml.elementDescriptorProvider - it allows clicking onto Fusion components
 * in AFX code, and jumping to their definition (provided we found only a single one).
 */
public class AfxFusionElementDescriptorProvider implements XmlElementDescriptorProvider {

    /**
     * This code currently only works for fully-qualified Fusion prototypes, having exactly one ":" between namespace and name.
     *
     * NOTE: {@link ResolveEngine#getPrototypeDefinitions(Project, String, String)} only returns a single PsiElement
     *       in the list in case it found a prototype definition (i.e. a line like "prototype(X) < prototype(Y)" when
     *       searching for "X"). We trigger "jump to definition" only in this case.
     *
     *       From the {@link XmlElementDescriptor}, we cannot return more than one "Declaration", because
     *       the {@link TagNameReference}, whose resolve() method calls the descriptor's getDeclaration() method,
     *       is NOT of type {@link PsiPolyVariantReference}.
     *
     *       How is the TagNameReference created/called? The {@link com.intellij.psi.impl.source.xml.XmlTagImpl#getReferences(PsiReferenceService.Hints)} creates
     *       the TagNameReference.
     *
     * IN CASE WE WANT TO RETURN MULTIPLE RESULTS LATER, we could provide our own TagNameReference also implementing
     * PsiPolyVariantReference; by registering an own xml.xmlExtension; and overriding createTagNameReference().
     *
     * Alternatively, we could implement a custom {@link com.intellij.psi.PsiReferenceContributor}, which would be able to add
     * multiple completions. However, this would still mark the Fusion XML tag "red" and as unknown XML element in the system.
     */
    @Nullable
    @Override
    public XmlElementDescriptor getDescriptor(XmlTag tag) {
        String key = tag.getName();
        String[] nameParts = tag.getName().split(":");

        if (nameParts.length == 2) {
            List<PsiElement> fusionPrototypes = ResolveEngine.getPrototypeDefinitions(tag.getProject(), nameParts[1], nameParts[0]);

            if (fusionPrototypes.size() == 1) {
                return new AfxXmlTagDescriptor(key, fusionPrototypes);
            }
        }

        return null;
    }


    /**
     * This class only implements getDeclaration() meaningfully.
     */
    public static class AfxXmlTagDescriptor implements XmlElementDescriptor {
        private final String myTagName;
        private final List<PsiElement> prototypes;


        public AfxXmlTagDescriptor(String tagName, List<PsiElement> prototypes) {
            myTagName = tagName;
            this.prototypes = new LinkedList<>(prototypes);
        }

        @Override
        public String getName(PsiElement context) {
            return myTagName;
        }

        @Override
        public String getName() {
            return myTagName;
        }

        @Override
        public void init(PsiElement element) {

        }

        /**
         * This method is used for "jump to source" in {@link TagNameReference#resolve()}.
         *
         * At this point, we know we have exactly one element in the prototypes list.
         */
        @Override
        public PsiElement getDeclaration() {
            return this.prototypes.get(0);
        }

        @Override
        public String getQualifiedName() {
            return null;
        }

        @Override
        public String getDefaultName() {
            return myTagName;
        }

        @Override
        public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
            return new XmlElementDescriptor[0];
        }

        @Nullable
        @Override
        public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
            return null;
        }

        @Override
        public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
            return new XmlAttributeDescriptor[0];
        }

        @Nullable
        @Override
        public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, @Nullable XmlTag context) {
            return null;
        }

        @Nullable
        @Override
        public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
            return null;
        }

        @Nullable
        @Override
        public XmlNSDescriptor getNSDescriptor() {
            return null;
        }

        @Nullable
        @Override
        public XmlElementsGroup getTopGroup() {
            return null;
        }

        @Override
        public int getContentType() {
            return 0;
        }

        @Nullable
        @Override
        public String getDefaultValue() {
            return null;
        }
    }
}
