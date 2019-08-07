package de.vette.idea.neos.lang.fusion;

import com.intellij.psi.tree.ILazyParseableElementType;
import de.vette.idea.neos.lang.fusion.psi.FusionLazyParsableElementType;
import org.jetbrains.annotations.NotNull;

public class LazyParsableElementTypeFactory {
    @NotNull
    public static ILazyParseableElementType factory(@NotNull  String name) {
        if (name.equals("EXPRESSION_CONTENT")) {
            return new FusionLazyParsableElementType("EXPRESSION_CONTENT");
        }

        throw new RuntimeException("Unknown element type: " + name);
    }
}
