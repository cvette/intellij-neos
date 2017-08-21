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

package de.vette.idea.neos.lang.yaml.references.value;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import de.vette.idea.neos.NeosProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLValue;

/**
 * Entry point for "go to definition" "cmd-click" on a php class name in yaml files to jump to its declaration.
 */
public class ValueReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(YAMLValue.class),
                new ValueReferenceProvider()
        );
    }

    private static class ValueReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            if(!NeosProjectComponent.isEnabled(element)) {
                return new PsiReference[0];
            }

            YAMLValue yamlElement = (YAMLValue) element;
                return new PsiReference[] {
                    new ValueReference(yamlElement)
                };
        }
    }
}
