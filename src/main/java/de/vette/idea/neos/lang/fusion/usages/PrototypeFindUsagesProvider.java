/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2021  Christian Vette
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

package de.vette.idea.neos.lang.fusion.usages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import de.vette.idea.neos.lang.fusion.FusionLexerAdapter;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Prototype Find Usages Provider
 */
public class PrototypeFindUsagesProvider implements FindUsagesProvider {

    @Override
    public @Nullable WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(new FusionLexerAdapter(),
                TokenSet.create(FusionTypes.TYPE),
                TokenSet.create(FusionTypes.SINGLE_LINE_COMMENT, FusionTypes.DOC_COMMENT),
                TokenSet.EMPTY
        );
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof FusionPrototypeSignature;
    }

    @Override
    public @Nullable
    @NonNls String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public @Nls @NotNull String getType(@NotNull PsiElement element) {
        return "prototype";
    }

    @Override
    public @Nls @NotNull String getDescriptiveName(@NotNull PsiElement element) {
        if (element instanceof FusionPrototypeSignature) {
            FusionType type = ((FusionPrototypeSignature) element).getType();
            if (type != null && type.getText() != null) {
                return type.getText();
            }
        }

        return "";
    }

    @Override
    public @Nls @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (element instanceof FusionPrototypeSignature) {
            FusionType type = ((FusionPrototypeSignature) element).getType();
            if (type != null && type.getText() != null) {
                return type.getText();
            }
        }

        return "";
    }
}
