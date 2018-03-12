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

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class FusionCompletionContributor extends CompletionContributor
{
    public FusionCompletionContributor() {
        extend(
                CompletionType.BASIC,
                psiElement(FusionTypes.UNQUALIFIED_TYPE),
                new PrototypeProvider()
        );

        extend(
                CompletionType.BASIC,
                psiElement(FusionTypes.EEL_IDENTIFIER),
                new EelProvider()
        );
    }
}
