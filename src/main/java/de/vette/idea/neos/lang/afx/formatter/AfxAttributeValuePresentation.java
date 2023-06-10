package de.vette.idea.neos.lang.afx.formatter;

import com.intellij.util.ui.PresentableEnum;
import de.vette.idea.neos.lang.afx.AfxBundle;

public enum AfxAttributeValuePresentation implements PresentableEnum {
    QUOTES("Quotes", "afx.code.style.attribute.default.value.quotes"),
    BRACES("Braces", "afx.code.style.attribute.default.value.braces"),
    NONE("None", "afx.code.style.attribute.default.value.none");

    private final String myStored;
    private final String myPresentableTextKey;

    private AfxAttributeValuePresentation(String stored, String presentableTextKey) {
        this.myStored = stored;
        this.myPresentableTextKey = presentableTextKey;
    }

    public String toString() {
        return this.myStored;
    }

    public String getPresentableText() {
        return AfxBundle.message(this.myPresentableTextKey);
    }
}