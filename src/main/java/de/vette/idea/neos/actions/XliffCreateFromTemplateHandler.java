package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler;
import com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider;
import com.intellij.ide.fileTemplates.FileTemplate;
import de.vette.idea.neos.lang.fusion.FusionFileType;
import de.vette.idea.neos.lang.xliff.XliffFileType;

import java.util.Map;

public class XliffCreateFromTemplateHandler extends DefaultCreateFromTemplateHandler {
    @Override
    public boolean handlesTemplate(FileTemplate template) {
        return template.isTemplateOfType(XliffFileType.INSTANCE);
    }
}
