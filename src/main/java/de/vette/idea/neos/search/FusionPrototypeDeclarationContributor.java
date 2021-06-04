package de.vette.idea.neos.search;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class FusionPrototypeDeclarationContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        assert project != null;
        var prototypeNames = StubIndex.getInstance()
            .getAllKeys(FusionPrototypeDeclarationIndex.KEY, project);
        prototypeNames.forEach(prototypeName -> {
            var prototypeSignatures = StubIndex
                .getElements(FusionPrototypeDeclarationIndex.KEY, prototypeName, project, scope, FusionPrototypeSignature.class);
            prototypeSignatures.forEach(signature -> {
                if (signature.getType() != null) {
                    processor.process(signature.getType().getText());
                }
            });
        });
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        var nameParts = name.split(":");
        var indexedName = nameParts.length > 1 ? nameParts[1] : name;
        Collection<FusionPrototypeSignature> prototypes = StubIndex.getElements(FusionPrototypeDeclarationIndex.KEY, indexedName, parameters.getProject(), parameters.getSearchScope(), FusionPrototypeSignature.class);
        prototypes.forEach(prototypeSignature -> {
            if (prototypeSignature.getType() != null) {
                var navigationItem = new FusionPrototypeNavigationItem(
                    prototypeSignature,
                    prototypeSignature.getType().getText()
                );
                processor.process(navigationItem);
            }
        });
    }

    private static class FusionPrototypeNavigationItem extends de.vette.idea.neos.search.NavigationItem {
        /**
         * Creates a new display item.
         *
         * @param psiElement The PsiElement to navigate to.
         * @param text       Text to show for this element.
         */
        public FusionPrototypeNavigationItem(@NotNull NavigatablePsiElement psiElement, @NotNull String text) {
            super(psiElement, text, FusionIcons.PROTOTYPE);
        }
    }
}
