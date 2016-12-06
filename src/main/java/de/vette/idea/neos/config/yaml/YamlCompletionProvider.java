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

package de.vette.idea.neos.config.yaml;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    private List<LookupElement> lookupList;
    private Map<String, String> lookupMap;
    private String[] lookupArray;

    public YamlCompletionProvider(List<LookupElement> lookups) {
        this.lookupList = lookups;
    }

    public YamlCompletionProvider(Map<String, String> lookups) {
        this.lookupMap = lookups;
    }

    public YamlCompletionProvider(String[] lookups) {
        this.lookupArray = lookups;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        if(this.lookupList != null) {
            completionResultSet.addAllElements(this.lookupList);
        } else if(lookupMap != null) {
            for (Map.Entry<String, String> lookup : lookupMap.entrySet()) {
                LookupElementBuilder lookupElement = LookupElementBuilder.create(lookup.getKey()).withTypeText(lookup.getValue(), true).withIcon(FusionIcons.FILE);
                if(lookup.getValue() != null && lookup.getValue().contains("deprecated")) {
                    lookupElement = lookupElement.withStrikeoutness(true);
                }

                completionResultSet.addElement(lookupElement);
            }
        } else if(this.lookupArray != null) {
            for (String lookup : this.lookupArray) {
                completionResultSet.addElement(LookupElementBuilder.create(lookup).withIcon(FusionIcons.FILE));
            }
        }

    }
}
