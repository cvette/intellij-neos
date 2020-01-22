package de.vette.idea.neos.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import org.jetbrains.annotations.NotNull;

public class CreateFusionFile extends CreateFileFromTemplateAction implements DumbAware {

    private static final String NEW_FUSION_FILE = "Fusion File";

    public CreateFusionFile() {
        super(NEW_FUSION_FILE, "Create new Fusion file", FusionIcons.FILE);
    }


    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle(NEW_FUSION_FILE).addKind("Empty File", FusionIcons.FILE, "Fusion File");
        builder.setTitle(NEW_FUSION_FILE).addKind("Content Prototype", FusionIcons.PROTOTYPE, "Fusion Content Prototype");
        builder.setTitle(NEW_FUSION_FILE).addKind("Component Prototype", FusionIcons.PROTOTYPE, "Fusion Component Prototype");
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return NEW_FUSION_FILE;
    }
}
