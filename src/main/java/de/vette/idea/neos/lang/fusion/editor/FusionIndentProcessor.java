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

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

import de.vette.idea.neos.lang.fusion.psi.FusionTypes;

public class FusionIndentProcessor {

    public static final TokenSet TYPES_WHICH_DO_NOT_NEED_AN_BEGINNING_INDENT = TokenSet.create(
            FusionTypes.RIGHT_BRACE
    );
    public static final TokenSet TYPES_WHICH_CANNOT_GET_AN_INDENT = TokenSet.create(
            FusionTypes.VALUE_STRING
    );
    public static final TokenSet TYPES_WHICH_PROVOKE_AN_INDENT = TokenSet.create(
            FusionTypes.BLOCK
    );

    /**
     * @param node An ASTNode
     * @return The calculated indent for the given node
     */
    public static Indent getIndent(ASTNode node) {
        IElementType type = node.getElementType();
        Indent indent = Indent.getNoneIndent();
        ASTNode parent = node.getTreeParent();
        if (parent == null) {
            return indent;
        }
        if (TYPES_WHICH_PROVOKE_AN_INDENT.contains(parent.getElementType())) {
            indent = Indent.getIndent(Indent.Type.NORMAL, false, true);
        }
        if (TYPES_WHICH_DO_NOT_NEED_AN_BEGINNING_INDENT.contains(type)) {
            indent = Indent.getNoneIndent();
        }
        if (TYPES_WHICH_CANNOT_GET_AN_INDENT.contains(type)) {
            indent = Indent.getAbsoluteNoneIndent();
        }
        return indent;
    }
}
