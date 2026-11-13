package de.vette.idea.neos.lang.afx.psi;

import com.intellij.psi.impl.source.tree.CompositePsiElement;

/**
 * An AFX spread attribute: {@code {...expression}}.
 */
public class AfxSpread extends CompositePsiElement {
    public AfxSpread() {
        super(AfxElementTypes.AFX_SPREAD);
    }

    @Override
    public String toString() {
        return "AfxSpread";
    }
}
