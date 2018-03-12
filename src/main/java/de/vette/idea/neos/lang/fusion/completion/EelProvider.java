/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.lang.fusion.completion;

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

public class EelProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
        Project project = parameters.getPosition().getProject();
        Collection<String> contexts = FileBasedIndex.getInstance().getAllKeys(DefaultContextFileIndex.KEY, project);

        for (String eelHelper : contexts) {
            List<String> helpers = FileBasedIndex.getInstance().getValues(DefaultContextFileIndex.KEY, eelHelper, GlobalSearchScope.allScope(project));
            if (!helpers.isEmpty()) {
                for (String helper : helpers) {
                    Collection<PhpClass> classes = PhpIndex.getInstance(project).getClassesByFQN(helper);
                    for (PhpClass phpClass : classes) {
                        for (Method method : phpClass.getMethods()) {
                            String completionText = eelHelper + "." + method.getName() + "()";
                            result.addElement(LookupElementBuilder.create(completionText).withIcon(PhpIcons.METHOD_ICON));
                        }
                    }
                }
            }
        }
    }
}
