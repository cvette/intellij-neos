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

package de.vette.idea.neos.lang.fusion.pages;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.psi.codeStyle.DisplayPriority;
import com.intellij.psi.codeStyle.DisplayPrioritySortable;
import de.vette.idea.neos.lang.fusion.highlighting.FusionHighlightingColors;
import de.vette.idea.neos.lang.fusion.highlighting.FusionSyntaxHighlighter;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class FusionColorSettingsPage implements ColorSettingsPage, DisplayPrioritySortable {

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Single Line Comment", FusionHighlightingColors.SINGLE_LINE_COMMENT),
            new AttributesDescriptor("Block Comment", FusionHighlightingColors.BLOCK_COMMENT),
            new AttributesDescriptor("Declaration", FusionHighlightingColors.DECLARATION),
            new AttributesDescriptor("Include Path", FusionHighlightingColors.INCLUDE_PATH),
            new AttributesDescriptor("Object Type", FusionHighlightingColors.OBJECT_TYPE),
            new AttributesDescriptor("Meta Property", FusionHighlightingColors.META_PROPERTY),
            new AttributesDescriptor("Number", FusionHighlightingColors.NUMBER),
            new AttributesDescriptor("String", FusionHighlightingColors.STRING),
            new AttributesDescriptor("Escaped String", FusionHighlightingColors.STRING_ESCAPED),
            new AttributesDescriptor("Boolean and Null Values", FusionHighlightingColors.VALUE),
            new AttributesDescriptor("Prototype", FusionHighlightingColors.PROTOTYPE),
            new AttributesDescriptor("Path", FusionHighlightingColors.PATH),
            new AttributesDescriptor("Braces", FusionHighlightingColors.BRACES),
            new AttributesDescriptor("Parentheses", FusionHighlightingColors.PARENTHESES),
            new AttributesDescriptor("Assignment Operator", FusionHighlightingColors.ASSIGNMENT_OPERATOR),
            new AttributesDescriptor("Unset Operator", FusionHighlightingColors.UNSET_OPERATOR),
            new AttributesDescriptor("Copy Operator", FusionHighlightingColors.COPY_OPERATOR),
            new AttributesDescriptor("EEL Wrapper", FusionHighlightingColors.EEL_WRAPPER),
            new AttributesDescriptor("EEL Identifier", FusionHighlightingColors.EEL_IDENTIFIER),
            new AttributesDescriptor("EEL Function", FusionHighlightingColors.EEL_FUNCTION),
            new AttributesDescriptor("EEL Operator", FusionHighlightingColors.EEL_OPERATOR)
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return FusionIcons.FILE;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        this.getAdditionalHighlightingTagToDescriptorMap();
        return new FusionSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "include: NodeTypes/**/*\n" +
                "namespace: ts=TYPO3.TypoScript\n" +
                "\n" +
                "/**\n" +
                " * Change default page\n" +
                " **/\n" +
                "prototype(Neos.NodeTypes:Page.Document) < prototype(My.Package:DefaultPage) {\n" +
                "    body {\n" +
                "        content = ts:Template {\n" +
                "            templatePath = 'resource://My.Package/Private/Templates/TypoScript/PageContent.html'\n" +
                "\n" +
                "            // The default content section\n" +
                "            main = PrimaryContent {\n" +
                "                nodePath = 'main'\n" +
                "            }\n" +
                "\n" +
                "            sidebar = Neos:ContentCollection {\n" +
                "                nodePath = 'sidebar'\n" +
                "                @context.node = ${Neos.Node.nearestContentCollection(q(site).get(0), this.nodePath)}\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return new ColorDescriptor[0];
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Neos Fusion";
    }

    @Override
    public DisplayPriority getPriority() {
        return DisplayPriority.LANGUAGE_SETTINGS;
    }
}
