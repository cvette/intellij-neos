package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler;
import com.intellij.ide.fileTemplates.FileTemplate;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Map;

public class NodeTypeCreateFromTemplateHandler extends DefaultCreateFromTemplateHandler {
    @Override
    public boolean handlesTemplate(FileTemplate template) {
        return template.isTemplateOfType(YAMLFileType.YML) && (template.getName().equals(CreateNodeTypeDefinition.NODE_TYPE_TEMPLATE_NAME)
                || template.getName().equals(CreateNodeTypeDefinition.DOCUMENT_NODE_TYPE_TEMPLATE_NAME)
                || template.getName().equals(CreateNodeTypeDefinition.CONTENT_NODE_TYPE_TEMPLATE_NAME)
                || template.getName().equals(CreateNodeTypeDefinition.COLLECTION_NODE_TYPE_TEMPLATE_NAME)
                || template.getName().equals(CreateNodeTypeDefinition.MIXIN_NODE_TYPE_TEMPLATE_NAME)
                || template.getName().equals(CreateNodeTypeDefinition.CONSTRAINT_NODE_TYPE_TEMPLATE_NAME));
    }

    @Override
    public void prepareProperties(Map<String, Object> props) {
        String name = (String) props.get(FileTemplate.ATTRIBUTE_NAME);
        String nameWithoutPrefix = name.replaceFirst("NodeTypes\\.", "");

        props.put("NEOS_NODE_TYPE_NAME", nameWithoutPrefix);
    }
}
