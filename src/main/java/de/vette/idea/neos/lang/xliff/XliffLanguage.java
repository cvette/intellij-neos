package de.vette.idea.neos.lang.xliff;

import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xml.XMLLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class XliffLanguage extends XMLLanguage {
    public static final XliffLanguage INSTANCE = new XliffLanguage();

    private XliffLanguage() {
        super(XMLLanguage.INSTANCE, "XLIFF", "application/xliff+xml", "application/x-xliff+xml");
    }

    protected XliffLanguage(@NotNull Language baseLanguage, @NonNls @NotNull String name, @NonNls @NotNull String @NotNull ... mime) {
        super(baseLanguage, name, mime);
    }
}
