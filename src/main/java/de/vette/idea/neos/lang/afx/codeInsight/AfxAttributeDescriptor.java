package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.ArrayUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import org.jetbrains.annotations.Nullable;

/**
 * An AFX attribute: either a Fusion prototype property or a fixed meta attribute such as {@code @path}.
 */
public class AfxAttributeDescriptor implements XmlAttributeDescriptor {
    private final String name;
    private final @Nullable PsiElement declaration;

    public AfxAttributeDescriptor(String name, @Nullable PsiElement declaration) {
        this.name = name;
        this.declaration = declaration;
    }

    @Override
    public @Nullable PsiElement getDeclaration() {
        return declaration;
    }

    @Override
    public String getName(PsiElement context) {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init(PsiElement element) {
    }

    @Override
    public Object[] getDependences() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isFixed() {
        return false;
    }

    @Override
    public boolean hasIdType() {
        return false;
    }

    @Override
    public boolean hasIdRefType() {
        return false;
    }

    @Override
    public @Nullable String getDefaultValue() {
        return null;
    }

    @Override
    public boolean isEnumerated() {
        return false;
    }

    @Override
    public String[] getEnumeratedValues() {
        return null;
    }

    @Override
    public @Nullable String validateValue(XmlElement context, String value) {
        return null;
    }
}
