package de.vette.idea.neos.lang.fusion.livetemplates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import de.vette.idea.neos.lang.fusion.FusionFileType;
import org.jetbrains.annotations.NotNull;

public class FusionContext extends TemplateContextType {

    public FusionContext() {
        super("NEOSFUSION", "Neos Fusion");
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
        return templateActionContext.getFile().getFileType() instanceof FusionFileType;
    }
}
