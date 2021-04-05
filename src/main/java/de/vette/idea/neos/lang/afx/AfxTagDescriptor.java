package de.vette.idea.neos.lang.afx;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.TagNameReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class AfxTagDescriptor implements XmlElementDescriptor {
    private final String myTagName;
    private final List<PsiElement> prototypes;


    public AfxTagDescriptor(String tagName, List<PsiElement> prototypes) {
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
