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
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MetaPathProvider extends CompletionProvider<CompletionParameters> {

    /**
     * Fusion meta properties understood by the runtime and built-in objects.
     * AFX-only attributes ({@code @path}, {@code @children}, {@code @key}) are omitted:
     * they are transpiled and are not valid Fusion paths.
     */
    private static final List<LookupElement> META_PROPERTIES = List.of(
            meta("if", "condition"),
            meta("process", "processor"),
            meta("position", "ordering"),
            meta("context", "context"),
            meta("apply", "property spread"),
            meta("cache", "caching"),
            meta("exceptionHandler", "exception handler"),
            meta("class", "implementation class"),
            meta("glue", "Join and Loop"),
            meta("private", "component props"),
            meta("ignoreProperties", "skip properties"),
            meta("sortProperties", "property order")
    );

    private static LookupElement meta(String name, String description) {
        return LookupElementBuilder.create(name).withTypeText(description, true);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
        result.addAllElements(META_PROPERTIES);
    }
}
