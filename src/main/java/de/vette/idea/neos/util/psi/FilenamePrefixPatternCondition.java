package de.vette.idea.neos.util.psi;/*
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

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilenamePrefixPatternCondition extends PatternCondition<PsiElement> {

    protected String prefix;

    public FilenamePrefixPatternCondition(@Nullable String prefix) {
        super("Filename prefix pattern");
        this.prefix = prefix;
    }

    @Override
    public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        return element.getContainingFile().getName().startsWith(prefix);
    }
}
