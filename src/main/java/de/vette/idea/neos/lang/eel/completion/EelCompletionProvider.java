package de.vette.idea.neos.lang.eel.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.vette.idea.neos.indexes.DefaultContextFileIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class EelCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        Project project = parameters.getPosition().getProject();
        Collection<String> contexts = FileBasedIndex.getInstance().getAllKeys(DefaultContextFileIndex.KEY, project);

        for (String eelHelper : contexts) {
            List<String> helpers = FileBasedIndex.getInstance().getValues(DefaultContextFileIndex.KEY, eelHelper, GlobalSearchScope.allScope(project));
            if (!helpers.isEmpty()) {
                for (String helper : helpers) {
                    Collection<PhpClass> classes = PhpIndex.getInstance(project).getClassesByFQN(helper);
                    for (PhpClass phpClass : classes) {
                        for (Method method : phpClass.getMethods()) {
                            if (!method.getAccess().isPublic()) {
                                continue;
                            }
                            if (method.getName().equals("allowsCallOfMethod")) {
                                continue;
                            }

                            String completionText = eelHelper + "." + method.getName() + "()";
                            result.addElement(LookupElementBuilder.create(completionText).withIcon(PhpIcons.METHOD_ICON));
                        }
                    }
                }
            }
        }
    }
}
