package de.vette.idea.neos.lang.fusion.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ILazyParseableElementType;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class FusionLazyElementType extends ILazyParseableElementType {
    public FusionLazyElementType(@NotNull String debugName) {
        super(debugName, EelLanguage.INSTANCE);
    }

    @Override
    public @Nullable ASTNode createNode(CharSequence text) {
        if (text == null || text.length() == 0) {
            return null;
        }

        return new FusionLazyPsiElement(this, text);
    }
}
