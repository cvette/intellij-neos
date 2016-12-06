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

package de.vette.idea.neos.lang.fusion.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Neos Fusion Brace Matcher
 */
public class FusionBraceMatcher implements PairedBraceMatcher {

    /**
     * Constructor
     */
    private static final BracePair[] PAIRS = {
            new BracePair(FusionTypes.LEFT_BRACE, FusionTypes.RIGHT_BRACE, true),
            new BracePair(FusionTypes.LEFT_PAREN, FusionTypes.RIGHT_PAREN, false),
            new BracePair(FusionTypes.EEL_LEFT_BRACE, FusionTypes.EEL_RIGHT_BRACE, false),
            new BracePair(FusionTypes.EEL_LEFT_PAREN, FusionTypes.EEL_RIGHT_PAREN, false),
            new BracePair(FusionTypes.EEL_LEFT_BRACKET, FusionTypes.EEL_RIGHT_BRACKET, false),
    };

    /**
     * @return The defined type pairs
     */
    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }

    /**
     * @param elementType IElementType 1
     * @param elementType2 IElementType 2
     * @return True
     */
    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType elementType, @Nullable IElementType elementType2) {
        return elementType2 == FusionTypes.CRLF;
    }

    /**
     * @param file PsiFile
     * @param openingBraceOffset int
     * @return openingBraceOffset
     */
    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
