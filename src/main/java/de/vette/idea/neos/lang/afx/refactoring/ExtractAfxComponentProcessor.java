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
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.util.ComposerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ExtractAfxComponentProcessor extends BaseRefactoringProcessor {
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

    /**
     * Retrieve a default value for the refactoring prompt.
     * Tries to find a name based on the surrounding prototype declaration, the package name or a fallback value otherwise.
     * @param element The element in the source fusion file containing the AFX
     */
    @NotNull
    public static String getSuggestedComponentName(@Nullable PsiElement element) {
        var closestPrototypeSignature = element != null ? getClosestPrototypeSignatureName(element) : null;
        if (closestPrototypeSignature != null) {
            return closestPrototypeSignature + ".Extracted";
        }

        var namespace = element != null ? getNamespaceFromFilePath(element.getContainingFile()) : null;
        if (namespace != null) {
            return namespace + ":Extracted";
        }

        return "Vendor.Package:Extracted";
    }

    @Nullable
    private static String getNamespaceFromFilePath(@NotNull PsiFile psiFile) {
        var composerFile = ComposerUtil.getComposerManifest(psiFile.getContainingDirectory());

        return composerFile == null ? null : ComposerUtil.getPackageKey(composerFile);
    }

    @Nullable
    private static String getClosestPrototypeSignatureName(@NotNull PsiElement element) {
        List<FusionPrototypeSignature> signatures = new ArrayList<>();
        var current = element;
        while (current != null && !(current instanceof FusionFile)) {
            FusionPath path = null;
            if (current instanceof FusionPropertyCopy copy) {
                path = copy.getPath();
            } else if (current instanceof FusionPropertyBlock block) {
                path = block.getPath();
            } else if (current instanceof FusionPropertyAssignment assignment) {
                path = assignment.getPath();
            }
            if (path != null) {
                List<FusionPrototypeSignature> subList = path.getPrototypeSignatureList().subList(0, path.getPrototypeSignatureList().size());
                if (!subList.isEmpty()) {
                    Collections.reverse(subList);
                    signatures.addAll(subList);
                }
            }
            current = current.getParent();
        }

        return signatures.isEmpty() ? null : signatures.get(0).getName();
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
