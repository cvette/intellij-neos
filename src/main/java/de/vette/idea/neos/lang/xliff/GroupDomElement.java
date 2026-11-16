package de.vette.idea.neos.lang.xliff;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.List;

/**
 * A group of translation units. Plural forms are stored in a group with restype "x-gettext-plurals".
 */
public interface GroupDomElement extends DomElement {
    String PLURALS_RESTYPE = "x-gettext-plurals";

    GenericAttributeValue<String> getId();

    GenericAttributeValue<String> getRestype();

    List<TransUnitDomElement> getTransUnits();

    List<GroupDomElement> getGroups();
}
