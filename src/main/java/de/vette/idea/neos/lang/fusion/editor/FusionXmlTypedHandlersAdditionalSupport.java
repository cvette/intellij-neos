package de.vette.idea.neos.lang.fusion.editor;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.XmlTypedHandlersAdditionalSupport;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import org.jetbrains.annotations.NotNull;

public class FusionXmlTypedHandlersAdditionalSupport implements XmlTypedHandlersAdditionalSupport {
    @Override
    public boolean isAvailable(@NotNull PsiFile psiFile, @NotNull Language lang) {
        return lang.isKindOf(FusionLanguage.INSTANCE);
    }
}
