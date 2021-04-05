package de.vette.idea.neos.lang.afx;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfxLanguage extends HTMLLanguage {

    public static final AfxLanguage INSTANCE = new AfxLanguage();

    public AfxLanguage() {
        super(HTMLLanguage.INSTANCE, "NeosAfx");
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Neos Afx";
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public @Nullable LanguageFileType getAssociatedFileType() {
        return AfxFileType.INSTANCE;
    }
}
