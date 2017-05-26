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
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ProcessingContext;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PrototypeProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        Collection<String> keys = StubIndex.getInstance().getAllKeys(FusionPrototypeDeclarationIndex.KEY, parameters.getPosition().getProject());
        Project project = parameters.getPosition().getProject();

        for (String key : keys) {
            Collection<FusionPrototypeSignature> prototypes = StubIndex.getElements(FusionPrototypeDeclarationIndex.KEY, key, project, GlobalSearchScope.projectScope(project), FusionPrototypeSignature.class );
            for (FusionPrototypeSignature signature : prototypes) {
                if (signature.getType() != null) {
                    result.addElement(LookupElementBuilder.create(signature.getType().getText()).withIcon(FusionIcons.PROTOTYPE));
                }
            }
        }
    }


}
