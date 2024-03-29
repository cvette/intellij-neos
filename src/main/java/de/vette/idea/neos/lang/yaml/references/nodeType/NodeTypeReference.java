package de.vette.idea.neos.lang.yaml.references.nodeType;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.util.Collection;

/**
 * Resolving the Node Type when it is clicked
 */
public class NodeTypeReference extends PsiPolyVariantReferenceBase<YAMLPsiElement> {
    private final YAMLPsiElement yamlElement;
    private final String nodeTypeName;

    private static TextRange textRangeFromElement(YAMLPsiElement yamlElement, boolean isKey) {
        int sourceStart = 0;
        int sourceLength = 0;

        if (isKey && yamlElement instanceof YAMLKeyValue) {
            PsiElement key = ((YAMLKeyValue) yamlElement).getKey();
            if (key != null) {
                sourceLength = key.getTextLength();
            }
        } else if (yamlElement instanceof YAMLKeyValue) {
            PsiElement value = ((YAMLKeyValue) yamlElement).getValue();
            if (value != null) {
                sourceStart = value.getStartOffsetInParent();
                sourceLength = value.getTextLength();
            }
        } else if (yamlElement instanceof YAMLSequenceItem) {
            PsiElement value = ((YAMLSequenceItem) yamlElement).getValue();
            if (value != null) {
                sourceLength = value.getTextLength();
            }
        } else {
            return yamlElement.getTextRange();
        }

        return new TextRange(sourceStart, sourceStart + sourceLength);
    }

    public NodeTypeReference(YAMLKeyValue yamlElement, boolean isKey) {
        // the "textRange" is used for highlighting the source element
        super(yamlElement, textRangeFromElement(yamlElement, isKey));
        this.yamlElement = yamlElement;
        if (isKey) {
            this.nodeTypeName = yamlElement.getKeyText();
        } else {
            this.nodeTypeName = yamlElement.getValueText();
        }
    }

    NodeTypeReference(YAMLKeyValue yamlElement) {
        this(yamlElement, true);
    }

    NodeTypeReference(YAMLSequenceItem yamlElement) {
        super(yamlElement.getValue() == null ? yamlElement : yamlElement.getValue(), textRangeFromElement(yamlElement, false));
        this.yamlElement = yamlElement;
        var textValue = yamlElement.getValue().getText();
        if (yamlElement.getValue() instanceof YAMLQuotedText) {
            this.nodeTypeName = textValue.substring(1, textValue.length() - 1);
        } else {
            this.nodeTypeName = textValue;
        }
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        // files which contain the NodeType definition
        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(NodeTypesYamlFileIndex.KEY, nodeTypeName, GlobalSearchScope.allScope(yamlElement.getProject()));

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
                .filter(yamlKeyValue -> yamlKeyValue.getKeyText().equals(nodeTypeName))
                // remove "current" element if it exists
                .filter(yamlKeyValue -> yamlElement != yamlKeyValue)
                // build up the result object
                .map(yamlKeyValue -> new PsiElementResolveResult(yamlKeyValue, true))
                .toArray(PsiElementResolveResult[]::new);
    }
}
