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

package de.vette.idea.neos.config.nodeTypes;

import com.intellij.codeInsight.lookup.LookupElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodeTypesConfigLookupBuilder {
    public ArrayList<LookupElement> getRootItems() {
        return ListToElements(Arrays.asList("superTypes", "abstract", "aggregate", "ui", "label", "constraints", "childNodes", "properties"));
    }

    public ArrayList<LookupElement> ListToElements(List<String> items) {
        return this.ListToYmlElements(items);
    }

    public ArrayList<LookupElement> ListToYmlElements(List<String> items) {
        ArrayList<LookupElement> lookups = new ArrayList<>();
        for (String answer : items) {
            lookups.add(new NodeTypesConfigLookup(answer));
        }

        return lookups;

    }
}
