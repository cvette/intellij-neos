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

package de.vette.idea.neos.lang.fusion;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import de.vette.idea.neos.lang.fusion.parser.FusionParser;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;
import de.vette.idea.neos.lang.fusion.stubs.FusionFileStub;
import org.jetbrains.annotations.NotNull;

public class FusionParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE, FusionTypes.CRLF);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new FusionLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new FusionParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FusionFileStub.TYPE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.create(FusionTypes.SINGLE_LINE_COMMENT, FusionTypes.C_STYLE_COMMENT, FusionTypes.DOC_COMMENT);
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.create(FusionTypes.VALUE_STRING, FusionTypes.VALUE_STRING_QUOTE, FusionTypes.VALUE_STRING_ESCAPED_QUOTE);
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode astNode) {
        return FusionTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new FusionFile(fileViewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MAY;
    }
}
