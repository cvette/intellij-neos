package de.vette.idea.neos.lang.eel.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyAssignment;
import de.vette.idea.neos.lang.fusion.psi.FusionValueDslContent;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPropertyAssignmentIndex;
import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class EelPathsCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        Project project = parameters.getPosition().getProject();
        PsiElement element = PsiTreeUtil.getContextOfType(parameters.getPosition(), FusionValueDslContent.class);
        if (element == null) {
            element = parameters.getPosition().getOriginalElement();
        }

        if (element == null) {
            return;
        }

        Collection<FusionPropertyAssignment> paths = new Vector<>();
        List<String> prototypes = NeosUtil.getPrototypeNames(element);
        for (String prototype : prototypes) {
            paths.addAll(StubIndex.getElements(FusionPropertyAssignmentIndex.KEY, prototype, project, GlobalSearchScope.projectScope(project), FusionPropertyAssignment.class));
        }

        for (FusionPropertyAssignment assignment : paths) {
            String completionText = "this." + assignment.getPath().getText();
            String completionText2 = "props." + assignment.getPath().getText();

            result.addElement(LookupElementBuilder.create(completionText).withIcon(FusionIcons.PATH));
            result.addElement(LookupElementBuilder.create(completionText2).withIcon(FusionIcons.PATH));
        }
    }
}
