package de.vette.idea.neos.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import de.vette.idea.neos.NeosIcons;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.xliff.XliffIcons;
import org.jetbrains.annotations.NotNull;

public class CreateXliffFile extends CreateFileFromTemplateAction implements DumbAware {

    private static final String NEW_XLIFF_FILE = "XLIFF File";

    public CreateXliffFile() {
        super(NEW_XLIFF_FILE, "Create new XLIFF file", XliffIcons.XLIFF_FILE);
    }


    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle(NEW_XLIFF_FILE).addKind("Empty File", XliffIcons.XLIFF_FILE, "XLIFF File");
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return NEW_XLIFF_FILE;
    }
}
