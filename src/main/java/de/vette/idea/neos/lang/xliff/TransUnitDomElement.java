package de.vette.idea.neos.lang.xliff;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

public interface TransUnitDomElement extends DomElement {
    SourceDomElement getSource();

    TargetDomElement getTarget();

    GenericAttributeValue<String> getId();
}
