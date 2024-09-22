package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import de.vette.idea.neos.lang.fusion.psi.FusionElementFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ExtractAfxComponentSnippetGenerator {
    private final Project myProject;
    private final List<PsiElement> myOriginalSource;

    private String myBaseComponentName = "Neos.Fusion:Component";

    public ExtractAfxComponentSnippetGenerator(Project project, List<PsiElement> originalSource) {
        myProject = project;
        myOriginalSource = originalSource;
    }

    public String generateComponentCode(@NotNull String componentName, @NotNull Map<String, AfxExtractor.ExtractedProperty> dynamicProperties, boolean includeChildrenInComponent) {
        FileTemplate template = FileTemplateManager.getInstance(myProject).getInternalTemplate("Afx Extract Component Template");
        Properties properties = new Properties();

        String fusionPropertiesCode = "";

        StringBuilder fusionPropertiesBuilder = new StringBuilder();
        for (Map.Entry<String, AfxExtractor.ExtractedProperty> entry : dynamicProperties.entrySet()) {
            // child props are not exposed, as children are in the component with all their props
            if (entry.getValue().getDepth() > 0 && includeChildrenInComponent) {
                continue;
            }

            // would be nice to infer types from the values and initialize as string/null/boolean
            fusionPropertiesBuilder.append("\n").append(entry.getKey()).append(" = ").append("null");
        }
        fusionPropertiesBuilder.append("\n");
        fusionPropertiesCode = fusionPropertiesBuilder.toString();

        String afxCode = AfxExtractor
                .replaceDynamicProperties(
                        myProject,
                        myOriginalSource,
                        dynamicProperties,
                        includeChildrenInComponent/* && !dynamicProperties.containsKey("content")*/ ? null : "props.content"
                )
                .stream().map(PsiElement::getText).collect(Collectors.joining());
        afxCode = formatAfxCode(afxCode);
        properties.setProperty("FUSION_PROTOTYPE_NAME", componentName);
        properties.setProperty("FUSION_BASE_PROTOTYPE_NAME", myBaseComponentName);
        properties.setProperty("FUSION_AFX_CONTENT", afxCode.isBlank() ? "" : afxCode);
        properties.setProperty("FUSION_PROPERTIES", fusionPropertiesCode);

        try {
            return formatFusionCode(template.getText(properties));
        } catch (IOException e) {
            return "";
        }
    }

    public String generateComponentUsageCode(@NotNull String componentName, @NotNull Map<String, AfxExtractor.ExtractedProperty> dynamicProperties, boolean includeChildrenInComponent) {
        StringBuilder code = new StringBuilder();
        code.append("<").append(componentName);

        String childrenText = null;
        for (Map.Entry<String, AfxExtractor.ExtractedProperty> entry : dynamicProperties.entrySet()) {
            var valueText = entry.getValue().getTextValue();

            if (entry.getKey().equals("content")/* && entry.getValue().getDepth() == 0*/) {
                childrenText = valueText;
                continue;
            }

            // if children are within the component, we don't need to expose their props
            if (includeChildrenInComponent && entry.getValue().getDepth() > 0) {
                continue;
            }

            // value-text will come as eel content with braces, so we remove them before adding new ones..
            if (valueText != null && valueText.startsWith("{") && valueText.endsWith("}")) {
                return valueText.substring(1, valueText.length() - 1);
            }

            code.append(" ").append(entry.getKey()).append("={").append(valueText).append("}");
        }

        if (dynamicProperties.containsKey("content") && dynamicProperties.get("content").getDepth() == 0) {
            code.append(">").append(childrenText).append("</").append(componentName).append(">");
        } else if (includeChildrenInComponent) {
            code.append("/>");
        } else {
            code.append(">");

            for (PsiElement child : AfxExtractor.getChildren(myOriginalSource.get(0))) {
                code.append(child.getText());
            }

            code.append("</").append(componentName).append(">");
        }

        return formatAfxCode(code.toString());
    }

    public void setBaseComponentName(@NotNull String baseComponentName) {
        myBaseComponentName = baseComponentName;
    }

    private String formatFusionCode(String fusionCode) {
        var fusionFile = FusionElementFactory.createFusionFile(myProject, fusionCode);
        return CodeStyleManager.getInstance(myProject).reformat(fusionFile).getText();
    }

    private String formatAfxCode(String afxCode) {
        // createFileFromText(AfxLanguage.INSTANCE, afxCode) threw an exception about modifying files outside a write action
        // we use XML here, as AFX formatting does not work properly
        var afxFile = PsiFileFactory.getInstance(myProject)
                .createFileFromText("dummy." + XmlFileType.INSTANCE.getDefaultExtension(), XmlFileType.INSTANCE, afxCode);
        return CodeStyleManager.getInstance(myProject).reformat(afxFile).getText();
    }
}
