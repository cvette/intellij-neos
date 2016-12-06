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

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FusionBlock extends AbstractBlock {
    private final Indent MY_INDENT;
    private final CommonCodeStyleSettings MY_SETTINGS;
    private final SpacingBuilder MY_SPACING_BUILDER;
    private List<Block> mySubBlocks;

    /**
     * Constructor
     */
    public FusionBlock(@NotNull ASTNode node,
                       @Nullable Alignment alignment,
                       @Nullable Wrap wrap,
                       @NotNull CommonCodeStyleSettings settings,
                       @NotNull SpacingBuilder spacingBuilder) {
        super(node, wrap, alignment);
        MY_SETTINGS = settings;
        MY_SPACING_BUILDER = spacingBuilder;
        MY_INDENT = FusionIndentProcessor.getIndent(node);
    }

    /**
     * @return The indent of this block
     */
    @Override
    public Indent getIndent() {
        return MY_INDENT;
    }

    /**
     * @return A copy of this subBlocks, if they are empty then they will be built
     */
    @NotNull
    @Override
    protected List<Block> buildChildren() {
        if (mySubBlocks == null) {
            mySubBlocks = buildSubBlocks();
        }
        return new ArrayList<Block>(mySubBlocks);
    }

    /**
     * @return An ArrayList of all child elements of this block.
     */
    private List<Block> buildSubBlocks() {
        List<Block> blocks = new ArrayList<Block>();
        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType childType = child.getElementType();
            if (child.getTextRange().getLength() == 0 || childType == TokenType.WHITE_SPACE ||
                    childType == FusionTypes.CRLF
                    ) {
                continue;
            }
            blocks.add(new FusionBlock(child, null, null, MY_SETTINGS, MY_SPACING_BUILDER));
        }
        return Collections.unmodifiableList(blocks);
    }

    /**
     * @param child1 Child block 1
     * @param child2 Child block 2
     * @return The spacing, which is defined in the spacing builder, of the two children.
     */
    @Override
    @Nullable
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return MY_SPACING_BUILDER.getSpacing(this, child1, child2);
    }

    /**
     * @return True, if the first child node is not null
     */
    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    /**
     * @param newChildIndex The index of the new child
     * @return The child attributes
     */
    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        Indent indent = Indent.getNoneIndent();
        if (FusionIndentProcessor.TYPES_WHICH_PROVOKE_AN_INDENT.contains(myNode.getElementType())) {
            indent = Indent.getNormalIndent();
        }
        return new ChildAttributes(indent, null);
    }
}
