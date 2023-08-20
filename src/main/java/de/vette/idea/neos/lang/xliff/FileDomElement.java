package de.vette.idea.neos.lang.xliff;

import com.intellij.util.xml.DomElement;

public interface FileDomElement extends DomElement {
    BodyDomElement getBody();
}