package de.vette.idea.neos.lang.fusion.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class TokenTypeFactory {
    public static IElementType getTokenType(@NotNull String name) {
        if (name.equals("EEL_VALUE")) {
            return FusionLazyElementTypes.EXPRESSION;
        }

        return new FusionTokenType(name);
    }
}
