package de.vette.idea.neos.lang.yaml.references.nodeType;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

/**
 * Entry point for "go to definition" "cmd-click" on a node type name in NodeTypes.yaml to jump to its definition.
 */
public class NodeTypeReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(YAMLKeyValue.class),
                new KeyValueNodeTypeReferenceProvider()
        );
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(YAMLScalar.class)
                    .withAncestor(1, PlatformPatterns.psiElement(YAMLSequenceItem.class)),
                new SequenceItemNodeTypeReferenceProvider()
        );
    }

    private static abstract class AbstractNodeTypeReferenceProvider extends PsiReferenceProvider {

        /**
         * Helper functions to walk the yaml tree
         */
        protected static boolean parentKeyIs(YAMLKeyValue yamlKeyValue, String parentKey) {
            YAMLKeyValue key = getParentKey(yamlKeyValue);
            return (key != null && parentKey.equals(key.getKeyText()));
        }

        protected static boolean grandparentKeyIs(YAMLKeyValue yamlKeyValue, String grandparentKey) {
            YAMLKeyValue key = getParentKey(yamlKeyValue);
            if (key != null) {
                key = getParentKey(key);
                return (key != null && grandparentKey.equals(key.getKeyText()));
            }
            return false;
        }

        protected static boolean isOnRootLevel(YAMLKeyValue yamlKeyValue) {
            return getParentKey(yamlKeyValue) == null;
        }

        protected static <T extends YAMLPsiElement> T getParent(YAMLPsiElement yamlElement, Class<T> type) {
            return PsiTreeUtil.getParentOfType(yamlElement, type);
        }

        protected static YAMLKeyValue getParentKey(YAMLPsiElement yamlElement) {
            return getParent(yamlElement, YAMLKeyValue.class);
        }
    }


    /**
     * Provide references based on key-value entries, i.e. node-types as configuration keys or simple values
     */
    private static class KeyValueNodeTypeReferenceProvider extends AbstractNodeTypeReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            YAMLKeyValue yamlElement = (YAMLKeyValue) element;

            // we support the following cases:
            // - superTypes
            // - constraints.nodeTypes
            // - root level (to find other definitions on root)
            if (parentKeyIs(yamlElement, "superTypes")
                || (parentKeyIs(yamlElement, "nodeTypes") && grandparentKeyIs(yamlElement, "constraints"))
                || isOnRootLevel(yamlElement)) {
                return new PsiReference[]{
                    new NodeTypeReference(yamlElement)
                };
            }

            // we also support:
            // - childNodes.type (works for node-templates as well)
            if (yamlElement.getKeyText().equals("type") && grandparentKeyIs(yamlElement, "childNodes")) {
                return new PsiReference[]{
                    new NodeTypeReference(yamlElement, false)
                };
            }

            return PsiReference.EMPTY_ARRAY;
        }
    }

    /**
     * Provide references based on string lists, i.e. allowed node-types in editor-options
     */
    private static class SequenceItemNodeTypeReferenceProvider extends AbstractNodeTypeReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            YAMLSequenceItem yamlItem = getParent((YAMLPsiElement) element, YAMLSequenceItem.class);
            YAMLKeyValue yamlElement = getParentKey(yamlItem);

            // we support
            // - editorOptions.nodeTypes
            if (yamlElement.getKeyText().equals("nodeTypes") && parentKeyIs(yamlElement, "editorOptions")) {
                return new PsiReference[]{
                    new NodeTypeReference(yamlItem)
                };
            }

            return PsiReference.EMPTY_ARRAY;
        }
    }
}
