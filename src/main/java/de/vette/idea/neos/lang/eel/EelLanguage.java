package de.vette.idea.neos.lang.eel;

import com.intellij.lang.Language;

public class EelLanguage extends Language {
    public static final Language INSTANCE = new EelLanguage();

    public EelLanguage() {
        super("Neos EEL", "text/neos-eel");
    }
}
