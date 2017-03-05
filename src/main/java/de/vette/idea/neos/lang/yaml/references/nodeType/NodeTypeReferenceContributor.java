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

/**
 * Entry point for "go to definition" "cmd-click" on a node type name in NodeTypes.yaml to jump to its definition.
 */
public class NodeTypeReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(YAMLKeyValue.class),
                new NodeTypeReferenceProvider()
        );
    }

    private static class NodeTypeReferenceProvider extends PsiReferenceProvider {
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

            return PsiReference.EMPTY_ARRAY;
        }

        /**
         * Helper functions to walk the yaml tree
         */
        private static boolean parentKeyIs(YAMLKeyValue yamlKeyValue, String parentKey) {
            YAMLKeyValue key = getParentKey(yamlKeyValue);
            return (key != null && parentKey.equals(key.getKeyText()));
        }

        private static boolean grandparentKeyIs(YAMLKeyValue yamlKeyValue, String grandparentKey) {
            YAMLKeyValue key = getParentKey(yamlKeyValue);
            if (key != null) {
                key = getParentKey(key);
                return (key != null && grandparentKey.equals(key.getKeyText()));
            }
            return false;
        }

        private static boolean isOnRootLevel(YAMLKeyValue yamlKeyValue) {
            return getParentKey(yamlKeyValue) == null;
        }

        private static YAMLKeyValue getParentKey(YAMLKeyValue yamlKeyValue) {
            return PsiTreeUtil.getParentOfType(yamlKeyValue, YAMLKeyValue.class);
        }
    }
}
