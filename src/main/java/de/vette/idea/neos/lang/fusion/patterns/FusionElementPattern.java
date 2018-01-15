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

package de.vette.idea.neos.lang.fusion.patterns;

import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class FusionElementPattern<T extends PsiElement,Self extends FusionElementPattern<T,Self>> extends PsiElementPattern<T,Self> {

    public FusionElementPattern(Class<T> aClass) {
        super(aClass);
    }

    public FusionElementPattern(@NotNull InitialPatternCondition<T> condition) {
        super(condition);
    }

    public static class Capture<T extends PsiElement> extends FusionElementPattern<T, FusionElementPattern.Capture<T>> {
        public Capture(Class<T> aClass) {
            super(aClass);
        }

        public Capture(@NotNull InitialPatternCondition<T> condition) {
            super(condition);
        }
    }
}
