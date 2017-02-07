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

package de.vette.idea.neos.lang.fusion.references;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import de.vette.idea.neos.lang.fusion.psi.FusionCompositeIdentifier;
import de.vette.idea.neos.lang.fusion.psi.FusionIdentifier;
import de.vette.idea.neos.lang.fusion.psi.FusionMethodName;
import org.jetbrains.annotations.NotNull;

public class DefaultContextReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        PsiReferenceProvider defaultContextProvider = new DefaultContextReferenceProvider();
        PsiReferenceProvider defaultContextMethodProvider = new DefaultContextMethodReferenceProvider();

        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(FusionCompositeIdentifier.class),
                defaultContextProvider
        );

        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(FusionMethodName.class),
                defaultContextMethodProvider
        );
    }
}
