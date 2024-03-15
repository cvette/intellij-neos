package de.vette.idea.neos.lang.xliff;

import com.intellij.util.xml.DomElement;

import java.util.List;

public interface BodyDomElement extends DomElement {
    List<TransUnitDomElement> getTransUnits();
}
