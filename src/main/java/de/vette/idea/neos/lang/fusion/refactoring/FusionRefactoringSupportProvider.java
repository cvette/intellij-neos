package de.vette.idea.neos.lang.fusion.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fusion Refactoring Support Provider
 */
public class FusionRefactoringSupportProvider extends RefactoringSupportProvider {

    @Override
    public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
        return (element instanceof FusionPrototypeSignature);
    }
}
