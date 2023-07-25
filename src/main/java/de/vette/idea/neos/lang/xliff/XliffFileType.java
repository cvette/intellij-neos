package de.vette.idea.neos.lang.xliff;

import com.intellij.ide.highlighter.XmlLikeFileType;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import de.vette.idea.neos.NeosIcons;
import de.vette.idea.neos.lang.eel.EelFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class XliffFileType extends XmlLikeFileType {
    @NonNls public static final String DOT_DEFAULT_EXTENSION = ".xlf";

    public static final XliffFileType INSTANCE = new XliffFileType();

    private XliffFileType() {
        super(XliffLanguage.INSTANCE);
    }

    protected XliffFileType(Language language) {
        super(language);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "Neos XLIFF";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Neos XLIFF";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "xlf";
    }

    @Override
    public Icon getIcon() {
        return NeosIcons.NODE_TYPE;
    }
}
