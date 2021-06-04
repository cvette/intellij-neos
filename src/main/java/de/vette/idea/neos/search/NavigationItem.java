package de.vette.idea.neos.search;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NavigationItem extends GoToSymbolProvider.BaseNavigationItem {

    protected NavigatablePsiElement psiElement;
    protected String label;
    protected Icon icon;

    /**
     * Creates a new display item.
     *
     * @param psiElement The PsiElement to navigate to.
     * @param text       Text to show for this element.
     * @param icon       Icon to show for this element.
     */
    public NavigationItem(@NotNull NavigatablePsiElement psiElement, @NotNull @NonNls String text, @Nullable Icon icon) {
        super(psiElement, text, icon);
        this.psiElement = psiElement;
        this.label = text;
        this.icon = icon;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new FlowSourceItemPresentation(psiElement.getContainingFile(), label);
    }

    public class FlowSourceItemPresentation implements ItemPresentation {

        private final PsiFile file;
        private final String label;

        /**
         * @param file          The file containing the navigation item
         * @param label         The text displayed in the navigation item
         */
        public FlowSourceItemPresentation(PsiFile file, String label) {
            this.file = file;
            this.label = label;
        }

        @Override
        public @NlsSafe String getPresentableText() {
            return label;
        }

        @Override
        public @NlsSafe  String getLocationString() {
            return SymbolPresentationUtil.getFilePathPresentation(file);
        }

        @Override
        public @Nullable Icon getIcon(boolean unused) {
            return icon;
        }
    }
}
