package de.vette.idea.neos.lang.xliff;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

public interface TargetDomElement extends DomElement {
    String getValue();

    GenericAttributeValue<String> getState();
}
