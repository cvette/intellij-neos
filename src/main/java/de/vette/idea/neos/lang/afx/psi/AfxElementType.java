package de.vette.idea.neos.lang.afx.psi;

import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import org.jetbrains.annotations.NotNull;

public class AfxElementType extends IElementType {
    public AfxElementType(@NotNull String debugName) {
        super(debugName, AfxLanguage.INSTANCE);
    }
}
