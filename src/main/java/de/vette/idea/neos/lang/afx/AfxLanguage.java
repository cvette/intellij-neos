package de.vette.idea.neos.lang.afx;

import com.intellij.lang.Language;

public class AfxLanguage extends Language {
    public static final Language INSTANCE = new AfxLanguage();

    public AfxLanguage() {
        super("Neos AFX", "text/neos-afx");
    }
}
