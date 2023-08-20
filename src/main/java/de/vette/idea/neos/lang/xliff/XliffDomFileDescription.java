package de.vette.idea.neos.lang.xliff;

import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XliffDomFileDescription extends DomFileDescription<XliffDomElement> {
    public XliffDomFileDescription() {
        super(XliffDomElement.class, "xliff");
    }

    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return file.getFileType() == XliffFileType.INSTANCE;
    }
}
