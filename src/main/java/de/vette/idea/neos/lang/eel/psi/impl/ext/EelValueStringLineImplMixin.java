package de.vette.idea.neos.lang.eel.psi.impl.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine;
import de.vette.idea.neos.lang.eel.psi.impl.EelExpressionImpl;
import de.vette.idea.neos.translation.eel.EelTranslationReferences;
import org.jetbrains.annotations.NotNull;

public abstract class EelValueStringLineImplMixin extends EelExpressionImpl implements EelValueStringLine {
    public EelValueStringLineImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Strings that are passed to the translation helper refer to translation units, sources and packages
     */
    @Override
    public PsiReference @NotNull [] getReferences() {
        return EelTranslationReferences.forString(this);
    }
}
