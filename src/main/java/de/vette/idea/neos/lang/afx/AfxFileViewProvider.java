package de.vette.idea.neos.lang.afx;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.afx.psi.AfxElementType;
import de.vette.idea.neos.lang.afx.psi.AfxTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class AfxFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements TemplateLanguageFileViewProvider {

    public static AfxElementType OUTER_AFX = new AfxElementType("Outer AFX");
    private static IElementType templateDataElement = new TemplateDataElementType("Outer HTML in AFX", AfxLanguage.INSTANCE, AfxTypes.T_TEXT, OUTER_AFX);

    public AfxFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled) {
        super(manager, virtualFile, eventSystemEnabled);
    }

    @NotNull
    @Override
    public Language getBaseLanguage() {
        return AfxLanguage.INSTANCE;
    }

    @NotNull
    @Override
    protected MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@NotNull VirtualFile fileCopy) {
        return new AfxFileViewProvider(getManager(), fileCopy, false);
    }

    @NotNull
    @Override
    public Language getTemplateDataLanguage() {
        return HTMLLanguage.INSTANCE;
    }

    @NotNull
    @Override
    public Set<Language> getLanguages() {
        Set<Language> languages = new HashSet<>(3);
        languages.add(getBaseLanguage());
        languages.add(getTemplateDataLanguage());

        return languages;
    }

    @Nullable
    @Override
    protected PsiFile createFile(@NotNull Language lang) {
        ParserDefinition parser = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
        if (parser == null) {
            return null;
        }

        if (lang == HTMLLanguage.INSTANCE) {
            PsiFileImpl file = (PsiFileImpl) parser.createFile(this);
            file.setContentElementType(templateDataElement);
            return file;
        }

        return lang == this.getBaseLanguage() ? parser.createFile(this) : null;
    }
}
