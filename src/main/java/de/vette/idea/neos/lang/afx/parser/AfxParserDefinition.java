package de.vette.idea.neos.lang.afx.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lang.html.HTMLParser;
import com.intellij.lang.html.HTMLParserDefinition;
import com.intellij.lang.html.HtmlParsing;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.xml.HtmlLanguageStubVersionUtil;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.afx.psi.AfxFile;
import org.jetbrains.annotations.NotNull;

public class AfxParserDefinition extends HTMLParserDefinition {

    public static IFileElementType FILE = new AfxFileElementType();

    public AfxParserDefinition() {
    }

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new AfxLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new HTMLParser() {
            @Override
            protected @NotNull HtmlParsing createHtmlParsing(@NotNull PsiBuilder builder) {
                return new AfxParsing(builder);
            }
        };
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new AfxFile(viewProvider);
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return super.createElement(node);
    }

    // based on HtmlFileElementType
    static class AfxFileElementType extends IStubFileElementType<PsiFileStub<?>> {
        public AfxFileElementType() {
            super(AfxLanguage.INSTANCE);
        }

        @Override
        public int getStubVersion() {
            return HtmlLanguageStubVersionUtil.getHtmlStubVersion();
        }
    }
}
