package de.vette.idea.neos.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.NeosIcons;
import org.jetbrains.annotations.NotNull;

public class CreateNodeTypeDefinition extends CreateFileFromTemplateAction implements DumbAware {

    private static final String NEW_NODE_TYPE_DEFINITION = "Node Type Definition";

    protected static final String NODE_TYPE_TEMPLATE_NAME = "Neos Node Type";
    protected static final String DOCUMENT_NODE_TYPE_TEMPLATE_NAME = "Neos Document Node Type";
    protected static final String CONTENT_NODE_TYPE_TEMPLATE_NAME = "Neos Content Node Type";
    protected static final String COLLECTION_NODE_TYPE_TEMPLATE_NAME = "Neos Collection Node Type";
    protected static final String MIXIN_NODE_TYPE_TEMPLATE_NAME = "Neos Mixin Node Type";
    protected static final String CONSTRAINT_NODE_TYPE_TEMPLATE_NAME = "Neos Constraint Node Type";

    @Override
    protected PsiFile createFile(String name, String templateName, PsiDirectory dir) {
        String nameWithoutPrefix = name.replaceFirst("NodeTypes\\.", "");
        String prefix = "";
        switch (templateName) {
            case DOCUMENT_NODE_TYPE_TEMPLATE_NAME:
                prefix = "Document.";
                break;
            case CONTENT_NODE_TYPE_TEMPLATE_NAME:
                prefix = "Content.";
                break;
            case COLLECTION_NODE_TYPE_TEMPLATE_NAME:
                prefix = "Collection.";
                break;
            case MIXIN_NODE_TYPE_TEMPLATE_NAME:
                prefix = "Mixin.";
                break;
            case CONSTRAINT_NODE_TYPE_TEMPLATE_NAME:
                prefix = "Constraint.";
                break;
        }

        if (!nameWithoutPrefix.startsWith(prefix)) {
            nameWithoutPrefix = prefix + nameWithoutPrefix;
        }

        name = "NodeTypes." + nameWithoutPrefix;
        return super.createFile(name, templateName, dir);
    }

    public CreateNodeTypeDefinition() {
        super(NEW_NODE_TYPE_DEFINITION, "Create new node type definition", NeosIcons.NODE_TYPE);
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle(NEW_NODE_TYPE_DEFINITION).addKind("Empty File", NeosIcons.NODE_TYPE, NODE_TYPE_TEMPLATE_NAME);
        builder.setTitle(NEW_NODE_TYPE_DEFINITION).addKind("Document", NeosIcons.NODE_TYPE, DOCUMENT_NODE_TYPE_TEMPLATE_NAME);
        builder.setTitle(NEW_NODE_TYPE_DEFINITION).addKind("Content", NeosIcons.NODE_TYPE, CONTENT_NODE_TYPE_TEMPLATE_NAME);
        builder.setTitle(NEW_NODE_TYPE_DEFINITION).addKind("Collection", NeosIcons.NODE_TYPE, COLLECTION_NODE_TYPE_TEMPLATE_NAME);
        builder.setTitle(NEW_NODE_TYPE_DEFINITION).addKind("Mixin", NeosIcons.NODE_TYPE, MIXIN_NODE_TYPE_TEMPLATE_NAME);
        builder.setTitle(NEW_NODE_TYPE_DEFINITION).addKind("Constraint", NeosIcons.NODE_TYPE, CONSTRAINT_NODE_TYPE_TEMPLATE_NAME);
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return NEW_NODE_TYPE_DEFINITION;
    }
}
