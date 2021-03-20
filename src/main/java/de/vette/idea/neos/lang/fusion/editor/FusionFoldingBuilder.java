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

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FusionFoldingBuilder implements FoldingBuilder{

    /**
     * @param node     Block node
     * @param document Current document
     * @return The folding regions of the given document, for the given node.
     */
    @NotNull
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
        List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();
        appendDescriptors(node, document, descriptors);
        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    /**
     * This function appends new FoldingDescriptors to the given one.
     *
     * @param node        Block node
     * @param document    Current document
     * @param descriptors List<FoldingDescriptor>
     * @return The given node
     */
    private static ASTNode appendDescriptors(final ASTNode node, final Document document, final List<FoldingDescriptor> descriptors) {
        IElementType elementType = node.getElementType();
        if (isFoldable(elementType)) {
            TextRange nodeTextRange = node.getTextRange();
            int newEndLine = document.getLineNumber(nodeTextRange.getEndOffset());
            int endLineOffset = document.getLineEndOffset(newEndLine);
            descriptors.add(new FoldingDescriptor(node, new TextRange(nodeTextRange.getStartOffset(), endLineOffset)));
        }
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            child = appendDescriptors(child, document, descriptors).getTreeNext();
        }
        return node;
    }

    /**
     * @param node Block node
     * @return The placeholder text for the given node, if it is foldable.
     */
    public String getPlaceholderText(@NotNull ASTNode node) {
        IElementType elementType = node.getElementType();
        if (!isFoldable(elementType)) {
            return null;
        }
        String placeholderText = "";
        if (elementType == FusionTypes.BLOCK) {
            placeholderText = "{...}";
        } else if (elementType == FusionTypes.DOC_COMMENT) {
            placeholderText = "/*...*/";
        }
        return placeholderText;
    }

    /**
     * If this is true, then the foldable elements are collapsed by default.
     *
     * @param node Block node
     * @return false
     */
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }

    /**
     * @param elementType The IElementType, which should be checked
     * @return True if the given type is foldable
     */
    private static boolean isFoldable(IElementType elementType) {
        return elementType == FusionTypes.BLOCK || elementType == FusionTypes.DOC_COMMENT;
    }
}
