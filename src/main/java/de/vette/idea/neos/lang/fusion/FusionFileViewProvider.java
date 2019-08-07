package de.vette.idea.neos.lang.fusion;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class FusionFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements TemplateLanguageFileViewProvider {
    public static final IElementType FUSION_FRAGMENT = new IElementType("FusionFragmentElementType", FusionLanguage.INSTANCE);
    public static final IElementType TEMPLATE_DATA = new TemplateDataElementType("FusionTextElementType", FusionLanguage.INSTANCE, LazyParsableElementTypes.EXPRESSION_CONTENT, FUSION_FRAGMENT);

    private final Language myTemplateDataLanguage;

    public FusionFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled) {
        super(manager, virtualFile, eventSystemEnabled);

        this.myTemplateDataLanguage = EelLanguage.INSTANCE;
    }

    public FusionFileViewProvider(PsiManager psiManager, VirtualFile virtualFile, boolean physical, Language myTemplateDataLanguage) {
        super(psiManager, virtualFile, physical);

        this.myTemplateDataLanguage = myTemplateDataLanguage;
    }

    @NotNull
    @Override
    public Language getBaseLanguage() {
        return FusionLanguage.INSTANCE;
    }

    @NotNull
    @Override
    protected MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@NotNull VirtualFile fileCopy) {
        return new FusionFileViewProvider(getManager(), fileCopy, false, myTemplateDataLanguage);
    }

    @NotNull
    @Override
    public Language getTemplateDataLanguage() {
        return myTemplateDataLanguage;
    }

    @NotNull
    @Override
    public Set<Language> getLanguages() {
        Set<Language> languages = new HashSet<>(2);
        languages.add(getBaseLanguage());
        languages.add(EelLanguage.INSTANCE);

        return languages;
    }

    @Nullable
    @Override
    protected PsiFile createFile(@NotNull Language lang) {
        if (lang == myTemplateDataLanguage) {
            PsiFileImpl file = (PsiFileImpl) LanguageParserDefinitions.INSTANCE.forLanguage(lang).createFile(this);
            file.setContentElementType(TEMPLATE_DATA);
            return file;
        } else if (lang == FusionLanguage.INSTANCE) {
            return LanguageParserDefinitions.INSTANCE.forLanguage(lang).createFile(this);
        } else {
            return null;
        }
    }
}
