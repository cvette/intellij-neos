package de.vette.idea.neos.lang.fusion;

import com.intellij.psi.tree.ILazyParseableElementType;
import de.vette.idea.neos.lang.fusion.psi.FusionLazyParsableElementType;

public interface LazyParsableElementTypes {
    public ILazyParseableElementType EXPRESSION_CONTENT = new FusionLazyParsableElementType("EXPRESSION_CONTENT");
}
