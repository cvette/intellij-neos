package de.vette.idea.neos.lang.afx.formatter;

import com.intellij.application.options.CodeStyle;
import com.intellij.configurationStore.Property;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfxCodeStyleSettings extends CustomCodeStyleSettings {

    @Property(externalName = "afx_attribute_value")
    public AfxAttributeValuePresentation AFX_ATTRIBUTE_VALUE = AfxAttributeValuePresentation.BRACES;

    public AfxCodeStyleSettings(@NotNull CodeStyleSettings container) {
        super("AfxCodeStyleSettings", container);
    }

    public static @NotNull AfxAttributeValuePresentation getAfxAttributeValue(@NotNull PsiElement context) {
        AfxCodeStyleSettings settings = getCustomSettings(context.getProject(), context.getContainingFile());
        return settings.AFX_ATTRIBUTE_VALUE;
    }

    private static @NotNull AfxCodeStyleSettings getCustomSettings(@NotNull Project project, @Nullable PsiFile file) {
        CodeStyleSettings settings = file != null ? CodeStyle.getSettings(file) : CodeStyle.getSettings(project);
        return settings.getCustomSettings((Class<? extends AfxCodeStyleSettings>) AfxCodeStyleSettings.class);
    }
}
