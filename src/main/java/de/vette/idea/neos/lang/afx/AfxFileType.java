package de.vette.idea.neos.lang.afx;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import de.vette.idea.neos.NeosIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AfxFileType extends LanguageFileType {
    public static final AfxFileType INSTANCE = new AfxFileType();

    public AfxFileType() {
        super(AfxLanguage.INSTANCE);
    }

    @Override
    public @NonNls
    @NotNull String getName() {
        return "Neos Afx";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Neos Afx";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "afx";
    }

    @Override
    public @Nullable Icon getIcon() {
        return NeosIcons.NODE_TYPE;
    }
}
