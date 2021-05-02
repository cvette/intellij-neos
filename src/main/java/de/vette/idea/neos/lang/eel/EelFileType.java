package de.vette.idea.neos.lang.eel;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import de.vette.idea.neos.NeosIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EelFileType extends LanguageFileType {
    public static final EelFileType INSTANCE = new EelFileType();

    public EelFileType() {
        super(EelLanguage.INSTANCE);
    }

    @Override
    public @NonNls
    @NotNull String getName() {
        return "Neos Eel";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Neos Eel";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "eel";
    }

    @Override
    public @Nullable Icon getIcon() {
        return NeosIcons.NODE_TYPE;
    }
}
