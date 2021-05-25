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

package de.vette.idea.neos.lang.eel.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.DefaultInjectedLanguageBlockBuilder;
import de.vette.idea.neos.lang.eel.EelLanguage;
import de.vette.idea.neos.lang.eel.psi.EelTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EelFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        SpacingBuilder spacingBuilder = createSpaceBuilder(settings);
        final EelBlock block = new EelBlock(formattingContext.getPsiElement().getNode(), null, null, formattingContext.getCodeStyleSettings(), spacingBuilder, new DefaultInjectedLanguageBlockBuilder(settings));
        return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.getPsiElement().getContainingFile(), block, settings);
    }

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        SpacingBuilder spacingBuilder = new SpacingBuilder(settings, EelLanguage.INSTANCE);
        final CommonCodeStyleSettings commonSettings = settings.getCommonSettings(EelLanguage.INSTANCE);

        if (commonSettings.SPACE_AFTER_COMMA) {
            spacingBuilder.after(EelTypes.VALUE_SEPARATOR).spaces(1);
        } else {
            spacingBuilder.after(EelTypes.VALUE_SEPARATOR).none();
        }

        if (commonSettings.SPACE_BEFORE_COMMA) {
            spacingBuilder.before(EelTypes.VALUE_SEPARATOR).spaces(1);
        } else {
            spacingBuilder.before(EelTypes.VALUE_SEPARATOR).none();
        }

        spacingBuilder.around(EelTypes.EEL_ADDITION_OPERATOR).spaces(1)
                .before(EelTypes.EEL_DOT).none()
                .after(EelTypes.EEL_DOT).none()
                .around(EelTypes.EEL_SUBTRACTION_OPERATOR).spaces(1)
                .around(EelTypes.EEL_MULTIPLICATION_OPERATOR).spaces(1)
                .around(EelTypes.EEL_DIVISION_OPERATOR).spaces(1)
                .around(EelTypes.EEL_MODULO_OPERATOR).spaces(1)
                .around(EelTypes.EEL_COMPARISON_OPERATOR).spaces(1)
                .around(EelTypes.EEL_ARROW).spaces(1);

        return spacingBuilder;
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode) {
        return null;
    }
}
