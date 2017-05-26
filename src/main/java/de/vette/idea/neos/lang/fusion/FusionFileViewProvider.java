package de.vette.idea.neos.lang.fusion;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class FusionFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider {
    public FusionFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile virtualFile, boolean eventSystemEnabled) {
        super(manager, virtualFile, eventSystemEnabled);
    }

    @NotNull
    @Override
    public Language getBaseLanguage() {
        return FusionLanguage.INSTANCE;
    }

    @NotNull
    @Override
    protected MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@NotNull VirtualFile fileCopy) {
        return new FusionFileViewProvider(getManager(), fileCopy, false);
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
        ParserDefinition parser = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
        if (parser == null) {
            return null;
        }

        if (lang == EelLanguage.INSTANCE) {
            PsiFileImpl file = (PsiFileImpl) parser.createFile(this);

            /* @TODO: This element needs to extend LazyParseableElement
             * file.setContentElementType(FusionTypes.EEL_CONTENT);
             */

            return file;
        }

        if (lang == FusionLanguage.INSTANCE) {
            return parser.createFile(this);
        }

        return null;
    }
}
