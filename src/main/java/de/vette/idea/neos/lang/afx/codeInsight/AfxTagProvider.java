package de.vette.idea.neos.lang.afx.codeInsight;

import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.completion.XmlTagInsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.impl.source.xml.TagNameReference;
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.*;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * This class is registered through plugin.xml as xml.elementDescriptorProvider - it allows clicking onto Fusion components
 * in AFX code, and jumping to their definition (provided we found only a single one).
 */
public class AfxTagProvider implements XmlElementDescriptorProvider, XmlTagNameProvider {

    /**
     * This code currently only works for fully-qualified Fusion prototypes, having exactly one ":" between namespace and name.
     * <p>
     * NOTE: {@link ResolveEngine#getPrototypeDefinitions(Project, String, String)} only returns a single PsiElement
     * in the list in case it found a prototype definition (i.e. a line like "prototype(X) < prototype(Y)" when
     * searching for "X"). We trigger "jump to definition" only in this case.
     * <p>
     * From the {@link XmlElementDescriptor}, we cannot return more than one "Declaration", because
     * the {@link TagNameReference}, whose resolve() method calls the descriptor's getDeclaration() method,
     * is NOT of type {@link PsiPolyVariantReference}.
     * <p>
     * How is the TagNameReference created/called? The {@link com.intellij.psi.impl.source.xml.XmlTagImpl#getReferences(PsiReferenceService.Hints)} creates
     * the TagNameReference.
     * <p>
     * IN CASE WE WANT TO RETURN MULTIPLE RESULTS LATER, we could provide our own TagNameReference also implementing
     * PsiPolyVariantReference; by registering an own xml.xmlExtension; and overriding createTagNameReference().
     * <p>
     * Alternatively, we could implement a custom {@link com.intellij.psi.PsiReferenceContributor}, which would be able to add
     * multiple completions. However, this would still mark the Fusion XML tag "red" and as unknown XML element in the system.
     */
    @Nullable
    @Override
    public XmlElementDescriptor getDescriptor(XmlTag tag) {
        if (!(tag instanceof HtmlTag)) {
            return null;
        }

        if (!(tag.getContainingFile().getLanguage() instanceof AfxLanguage)) {
            return null;
        }

        String key = tag.getName();
        String[] nameParts = tag.getName().split(":");

        if (nameParts.length == 2) {
            List<PsiElement> fusionPrototypes = ResolveEngine.getPrototypeDefinitions(tag.getProject(), nameParts[1], nameParts[0]);

            if (fusionPrototypes.size() == 1) {
                return new AfxTagDescriptor(key, fusionPrototypes);
            }
        }

        return null;
    }

    @Override
    public void addTagNameVariants(List<LookupElement> elements, @NotNull XmlTag tag, String prefix) {
        if (!(tag instanceof HtmlTag)) {
            return;
        }

        if (!(tag.getContainingFile().getLanguage() instanceof AfxLanguage)) {
            return;
        }

        Project project = tag.getProject();
        Collection<String> keys = StubIndex.getInstance().getAllKeys(FusionPrototypeDeclarationIndex.KEY, project);

        for (String key : keys) {
            Collection<FusionPrototypeSignature> prototypes = StubIndex.getElements(FusionPrototypeDeclarationIndex.KEY, key, project, GlobalSearchScope.projectScope(project), FusionPrototypeSignature.class);
            for (FusionPrototypeSignature signature : prototypes) {
                if (signature.getType() != null) {
                    elements.add(PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(signature.getType().getText())
                                .withIcon(FusionIcons.PROTOTYPE)
                                .withBoldness(true)
                                .withInsertHandler(XmlTagInsertHandler.INSTANCE),100.0)
                    );
                }
            }
        }
    }
}
