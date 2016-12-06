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

package de.vette.idea.neos.util.psi;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.lang.reflect.Array;

public class ParentKeysPatternCondition extends PatternCondition<PsiElement> {

    @NotNull
    private final String[] keys;
    private final Class<?>[] rootTypes = {
            YAMLMapping.class,
            YAMLKeyValue.class,
            YAMLMapping.class,
            YAMLDocument.class,
            YAMLFile.class
    };

    public ParentKeysPatternCondition(@NotNull String... keys) {
        super("Parent path pattern");
        this.keys = keys;
    }

    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
        PsiElement currentElement = psiElement.getParent();
        String expectedKey;
        int i = 0;
        int j = 0;

        while (currentElement != null) {
            if (i < Array.getLength(this.keys)) {
                if (YAMLKeyValue.class.isInstance(currentElement)) {
                    expectedKey = this.keys[i++];
                    if (!expectedKey.equals("*") && !expectedKey.equals(((YAMLKeyValue) currentElement).getKeyText())) {
                        return false;
                    }
                }
            } else if (!this.rootTypes[j++].isInstance(currentElement)) {
                return false;
            }

            currentElement = currentElement.getParent();
        }

        return !(i < Array.getLength(this.keys) || j < Array.getLength(this.rootTypes));
    }
}
