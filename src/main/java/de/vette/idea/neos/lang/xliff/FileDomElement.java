package de.vette.idea.neos.lang.xliff;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

public interface FileDomElement extends DomElement {
    BodyDomElement getBody();

    GenericAttributeValue<String> getOriginal();

    GenericAttributeValue<String> getProductName();

    GenericAttributeValue<String> getSourceLanguage();

    GenericAttributeValue<String> getTargetLanguage();
}
