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

package de.vette.idea.neos.lang.fusion.highlighting;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class FusionHighlightingColors {
    public static final TextAttributesKey DECLARATION =
            createTextAttributesKey("FUSION_INCLUDE", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey INCLUDE_PATH =
            createTextAttributesKey("FUSION_INCLUDE_PATH", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey META_PROPERTY =
            createTextAttributesKey("FUSION_META_PROPERTY", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey PROTOTYPE =
            createTextAttributesKey("FUSION_PROTOTYPE", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey VALUE =
            createTextAttributesKey("FUSION_VALUE", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey STRING =
            createTextAttributesKey("FUSION_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey STRING_ESCAPED =
            createTextAttributesKey("FUSION_STRING_ESCAPED", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    public static final TextAttributesKey ASSIGNMENT_OPERATOR =
            createTextAttributesKey("FUSION_ASSIGNMENT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey COPY_OPERATOR =
            createTextAttributesKey("FUSION_COPY_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey UNSET_OPERATOR =
            createTextAttributesKey("FUSION_UNSET_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("FUSION_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey OBJECT_TYPE =
            createTextAttributesKey("FUSION_OBJECT_TYPE", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey PATH =
            createTextAttributesKey("FUSION_PATH", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey SINGLE_LINE_COMMENT =
            createTextAttributesKey("FUSION_COMMENT_TYPE", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT =
            createTextAttributesKey("FUSION_COMMENT_TYPE", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey EEL_WRAPPER =
            createTextAttributesKey("FUSION_EEL_WRAPPER", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey EEL_IDENTIFIER =
            createTextAttributesKey("FUSION_EEL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey EEL_FUNCTION =
            createTextAttributesKey("FUSION_EEL_FUNCTION", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    public static final TextAttributesKey EEL_OPERATOR =
            createTextAttributesKey("FUSION_EEL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey BRACES =
            createTextAttributesKey("FUSION_EEL_OPERATOR", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey PARENTHESES  =
            createTextAttributesKey("FUSION_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
}
