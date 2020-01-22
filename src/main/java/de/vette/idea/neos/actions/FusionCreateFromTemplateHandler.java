package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler;
import com.intellij.ide.fileTemplates.FileTemplate;
import de.vette.idea.neos.lang.fusion.FusionFileType;

import java.util.Map;

public class FusionCreateFromTemplateHandler extends DefaultCreateFromTemplateHandler {
    @Override
    public boolean handlesTemplate(FileTemplate template) {
        return template.isTemplateOfType(FusionFileType.INSTANCE);
    }

    @Override
    public void prepareProperties(Map<String, Object> props) {
        Object name = props.get(FileTemplate.ATTRIBUTE_NAME);
        Object prototypePath = props.get(FusionTemplatePropertiesProvider.FUSION_PROTOTYPE_PATH);

        String prototypeName = "";
        if (name instanceof String) {
            prototypeName = (String) name;
        }

        if (prototypePath instanceof String) {
            if (!((String) prototypePath).isEmpty()) {
                prototypeName = String.join(".", (String) prototypePath, prototypeName);
            }
        }

        props.put(FusionTemplatePropertiesProvider.FUSION_PROTOTYPE_NAME, prototypeName);
    }
}
