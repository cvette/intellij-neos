package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lang.xml.XmlASTFactory;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.afx.psi.AfxAttribute;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;
import de.vette.idea.neos.lang.afx.psi.AfxTag;
import org.jetbrains.annotations.NotNull;

public class AfxASTFactory extends XmlASTFactory {
    @Override
    public CompositeElement createComposite(@NotNull IElementType type) {
        if (type == AfxElementTypes.AFX_TAG) {
            return new AfxTag();
        }

        if (type == AfxElementTypes.AFX_ATTRIBUTE) {
            return new AfxAttribute();
        }

        return super.createComposite(type);
    }
}
