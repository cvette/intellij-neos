package de.vette.idea.neos.lang.afx.psi;

import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfxLazyPsiElement extends LazyParseablePsiElement {
    public AfxLazyPsiElement(@NotNull IElementType type, @Nullable CharSequence buffer) {
        super(type, buffer);
    }
}
