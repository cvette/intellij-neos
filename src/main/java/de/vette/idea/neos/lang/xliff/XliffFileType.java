package de.vette.idea.neos.lang.xliff;

import com.intellij.ide.highlighter.DomSupportEnabled;
import com.intellij.ide.highlighter.XmlLikeFileType;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class XliffFileType extends XmlLikeFileType implements DomSupportEnabled {
    @NonNls public static final String DOT_DEFAULT_EXTENSION = ".xlf";

    public static final XliffFileType INSTANCE = new XliffFileType();

    private XliffFileType() {
        super(XliffLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "Neos XLIFF";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "XML Localization Interchange File Format";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "xlf";
    }

    @Override
    public Icon getIcon() {
        return XliffIcons.XLIFF_FILE;
    }
}
