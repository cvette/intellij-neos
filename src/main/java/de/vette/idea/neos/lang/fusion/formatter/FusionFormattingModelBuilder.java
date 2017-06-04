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

package de.vette.idea.neos.lang.fusion.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.editor.FusionBlock;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionFormattingModelBuilder implements FormattingModelBuilder {

    @NotNull
    @Override
    public FormattingModel createModel(PsiElement psiElement, CodeStyleSettings settings) {
        SpacingBuilder spacingBuilder = createSpaceBuilder(settings);
        final FusionBlock block = new FusionBlock(psiElement.getNode(), null, null, settings, spacingBuilder);
        return FormattingModelProvider.createFormattingModelForPsiFile(psiElement.getContainingFile(), block, settings);
    }

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        final FusionCodeStyleSettings fusionSettings = settings.getCustomSettings(FusionCodeStyleSettings.class);
        final CommonCodeStyleSettings commonSettings = settings.getCommonSettings(FusionLanguage.INSTANCE);

        SpacingBuilder spacingBuilder = new SpacingBuilder(settings, FusionLanguage.INSTANCE);
                spacingBuilder.before(FusionTypes.BLOCK).spaces(1);

                if (settings.SPACE_AFTER_COMMA) {
                    spacingBuilder.after(FusionTypes.VALUE_SEPARATOR).spaces(1);
                } else {
                    spacingBuilder.after(FusionTypes.VALUE_SEPARATOR).none();
                }

                if (settings.SPACE_AROUND_ASSIGNMENT_OPERATORS) {
                    spacingBuilder.around(FusionTypes.ASSIGNMENT_OPERATOR).spaces(1);
                } else {
                    spacingBuilder.around(FusionTypes.VALUE_SEPARATOR).none();
                }


                spacingBuilder.before(FusionTypes.UNSET_OPERATOR).spaces(1)
                .around(FusionTypes.COPY_OPERATOR).spaces(1)
                .around(FusionTypes.EEL_ADDITION_OPERATOR).spaces(1)
                .around(FusionTypes.EEL_SUBTRACTION_OPERATOR).spaces(1)
                .around(FusionTypes.EEL_MULTIPLICATION_OPERATOR).spaces(1)
                .around(FusionTypes.EEL_DIVISION_OPERATOR).spaces(1)
                .around(FusionTypes.EEL_MODULO_OPERATOR).spaces(1)
                .around(FusionTypes.EEL_COMPARISON_OPERATOR).spaces(1)
                .around(FusionTypes.NAMESPACE_ALIAS_SEPARATOR).spaces(1)
                .before(FusionTypes.NAMESPACE_SEPARATOR).none()
                .after(FusionTypes.NAMESPACE_SEPARATOR).spaces(1)
                .before(FusionTypes.INCLUDE_SEPARATOR).none()
                .after(FusionTypes.INCLUDE_SEPARATOR).spaces(1)
                .after(FusionTypes.EEL_LEFT_BRACE).none()
                .before(FusionTypes.EEL_RIGHT_BRACE).none()
                .between(FusionTypes.BLOCK, FusionTypes.PATH).blankLines(1);

        return spacingBuilder;
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode) {
        return null;
    }
}
