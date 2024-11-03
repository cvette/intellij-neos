package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.BaseUsageViewDescriptor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import de.vette.idea.neos.lang.afx.AfxBundle;
import de.vette.idea.neos.lang.fusion.psi.FusionElementFactory;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Properties;

class ExtractAfxComponentProcessor extends BaseRefactoringProcessor {
    private static final String BASE_PROTOTYPE_NAME = "Neos.Fusion:Component";

    private final PsiLanguageInjectionHost mySourceElement;
    private final PsiFile myTargetFile;
    private final PsiElement myElementToExtract;
    private final String myNewName;

    protected ExtractAfxComponentProcessor(@NotNull Project project, PsiLanguageInjectionHost sourceElement, PsiElement elementToExtract, String newName) {
        super(project);
        this.mySourceElement = sourceElement;
        this.myTargetFile = sourceElement.getContainingFile();
        this.myElementToExtract = elementToExtract;
        this.myNewName = newName;
    }

    @Override
    protected @NotNull UsageViewDescriptor createUsageViewDescriptor(UsageInfo @NotNull [] usageInfos) {
        return new BaseUsageViewDescriptor();
    }

    @Override
    protected UsageInfo @NotNull [] findUsages() {
        return new UsageInfo[0];
    }

    @Override
    protected void performRefactoring(UsageInfo @NotNull [] usageInfos) {
        var usageCode = "<" + myNewName + "/>";
        var componentCode = generateComponentCode(myNewName, myElementToExtract.getText());
        var newPrototype = formatFusionCode(componentCode);

        var relativeSelectionStart = myElementToExtract.getTextOffset();
        var relativeSelectionEnd = myElementToExtract.getTextOffset() + myElementToExtract.getTextLength();
        var currentContent = mySourceElement.getText();
        var newContent = currentContent.substring(0, relativeSelectionStart) + usageCode + currentContent.substring(relativeSelectionEnd);
        var updatedHost = mySourceElement.updateText(newContent);
        mySourceElement.replace(updatedHost);

        var anchor = SmartPointerManager.createPointer(myTargetFile.getFirstChild());
        for (PsiElement newChild : newPrototype.getChildren()) {
            myTargetFile.addBefore(newChild, anchor.getElement());
        }
    }

    protected FusionFile formatFusionCode(String fusionCode) {
        var fusionFile = FusionElementFactory.createFusionFile(myProject, fusionCode);
        return (FusionFile) CodeStyleManager.getInstance(myProject).reformat(fusionFile);
    }

    protected String generateComponentCode(String componentName, String afxCode) {
        FileTemplate template = FileTemplateManager.getInstance(myProject).getInternalTemplate("Afx Extract Component Template");

        Properties properties = new Properties();
        properties.setProperty("FUSION_PROTOTYPE_NAME", componentName);
        properties.setProperty("FUSION_BASE_PROTOTYPE_NAME", BASE_PROTOTYPE_NAME);
        properties.setProperty("FUSION_AFX_CONTENT", afxCode.isBlank() ? "" : afxCode);
        properties.setProperty("FUSION_PROPERTIES", "");

        try {
            return template.getText(properties);
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    protected @NotNull @NlsContexts.Command String getCommandName() {
        return AfxBundle.message("afx.refactoring.extract.component.title");
    }
}
