package de.vette.idea.neos.lang.fusion.livetemplates;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.lang.fusion.FusionFileType;
import org.jetbrains.annotations.NotNull;

public class FusionContext extends TemplateContextType {

    public FusionContext() {
        super("NEOSFUSION", "Neos Fusion");
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        return file.getFileType() instanceof FusionFileType;
    }

}
