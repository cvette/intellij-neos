package de.vette.idea.neos.lang.yaml.references.nodeType;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import java.util.Collection;

/**
 * Resolving the Node Type when it is clicked
 */
public class NodeTypeReference extends PsiPolyVariantReferenceBase<YAMLKeyValue> {
    private final YAMLKeyValue yamlElement;

    NodeTypeReference(YAMLKeyValue yamlElement) {
        // the "textRange" is used for highlighting the source element
        super(yamlElement, new TextRange(0, yamlElement.getKey().getTextLength() - 1));
        this.yamlElement = yamlElement;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        // TODO: fill for autocompletion!
        return new Object[0];
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String nodeTypeNameToFindReferenceFor = yamlElement.getKeyText();

        // files which contain the NodeType definition
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(NodeTypesYamlFileIndex.KEY, nodeTypeNameToFindReferenceFor, GlobalSearchScope.allScope(yamlElement.getProject()));

        return files
                .stream()
                // get the PSI for each file
                .map(file -> PsiManager.getInstance(yamlElement.getProject()).findFile(file))
                // ensure we only have YAML files
                .filter(psiFile -> psiFile instanceof YAMLFile)
                .map(psiFile -> (YAMLFile) psiFile)
                // get all YAML keys in these files
                .flatMap(yamlFile -> YAMLUtil.getTopLevelKeys(yamlFile).stream())
                // get the correct YAML key
                .filter(yamlKeyValue -> yamlKeyValue.getKeyText().equals(nodeTypeNameToFindReferenceFor))
                // build up the result object
                .map(yamlKeyValue -> new PsiElementResolveResult(yamlKeyValue, true))
                .toArray(PsiElementResolveResult[]::new);
    }
}
