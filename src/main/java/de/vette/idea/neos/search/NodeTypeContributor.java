package de.vette.idea.neos.search;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import de.vette.idea.neos.NeosIcons;
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;


public class NodeTypeContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        assert project != null;
        var nodeTypeNames = FileBasedIndex.getInstance()
            .getAllKeys(NodeTypesYamlFileIndex.KEY, project);
        nodeTypeNames.forEach(processor::process);
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        var files = FileBasedIndex.getInstance()
            .getContainingFiles(NodeTypesYamlFileIndex.KEY, name, parameters.getSearchScope());
        var psiManager = PsiManager.getInstance(parameters.getProject());
        files.forEach(file -> {
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile instanceof YAMLFile) {
                var definitionTarget = YAMLUtil.getTopLevelKeys((YAMLFile) psiFile)
                    .stream().filter(yamlKeyValue -> yamlKeyValue.getKeyText().equals(name)).findFirst();
                definitionTarget.ifPresent(yamlKeyValue -> processor.process(new NodeTypeNavigationItem(yamlKeyValue, name)));
            }
        });
    }

    private static class NodeTypeNavigationItem extends de.vette.idea.neos.search.NavigationItem {
        /**
         * Creates a new display item.
         *
         * @param psiElement The PsiElement to navigate to.
         * @param text       Text to show for this element.
         */
        public NodeTypeNavigationItem(@NotNull NavigatablePsiElement psiElement, @NotNull String text) {
            super(psiElement, text, NeosIcons.NODE_TYPE);
        }
    }
}
