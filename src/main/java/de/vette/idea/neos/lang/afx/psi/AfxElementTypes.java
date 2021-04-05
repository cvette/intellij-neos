package de.vette.idea.neos.lang.afx.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.IXmlTagElementType;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import org.jetbrains.annotations.NotNull;

public class AfxElementTypes {
    public static AfxTagElementType AFX_TAG = new AfxTagElementType("AFX_TAG");
    public static AfxAttributeElementType AFX_ATTRIBUTE = new AfxAttributeElementType("AFX_ATTRIBUTE");

    public static AfxElementType AFX_EEL_START_DELIMITER = new AfxElementType("AFX_EEL_START_DELIMITER");
    public static AfxElementType AFX_EEL_END_DELIMITER = new AfxElementType("AFX_EEL_END_DELIMITER");
    public static AfxElementType AFX_EEL_VALUE = new AfxElementType("AFX_EEL_VALUE");

    static class AfxElementType extends IElementType {
        public AfxElementType(@NotNull String debugName) {
            super(debugName, AfxLanguage.INSTANCE);
        }
    }

    static class AfxTagElementType extends IElementType implements IXmlTagElementType {
        public AfxTagElementType(@NotNull String debugName) {
            super(debugName, AfxLanguage.INSTANCE);
        }
    }

    static class AfxAttributeElementType extends IElementType implements IXmlTagElementType {
        public AfxAttributeElementType(@NotNull String debugName) {
            super(debugName, AfxLanguage.INSTANCE);
        }
    }
}
