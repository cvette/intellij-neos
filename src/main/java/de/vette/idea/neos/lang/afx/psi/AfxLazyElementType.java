package de.vette.idea.neos.lang.afx.psi;

import com.intellij.lang.*;
import com.intellij.psi.tree.ILazyParseableElementType;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AfxLazyElementType extends ILazyParseableElementType {
    public AfxLazyElementType(@NotNull String debugName) {
        super(debugName, EelLanguage.INSTANCE);
    }

    @Override
    public @Nullable ASTNode createNode(CharSequence text) {
        if (text == null || text.length() == 0) {
            return null;
        }

        return new AfxLazyPsiElement(this, text);
    }
}
