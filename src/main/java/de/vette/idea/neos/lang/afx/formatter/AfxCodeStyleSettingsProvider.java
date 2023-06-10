package de.vette.idea.neos.lang.afx.formatter;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfxCodeStyleSettingsProvider extends CodeStyleSettingsProvider {

    public Language getLanguage() {
        return AfxLanguage.INSTANCE;
    }

    @Override
    public @Nullable CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new AfxCodeStyleSettings(settings);
    }
}
