package de.vette.idea.neos.lang.eel.psi;

import de.vette.idea.neos.lang.eel.resolve.ref.EelReference;

public interface EelReferenceElement extends EelElement {

    @Override
    EelReference getReference();
}
