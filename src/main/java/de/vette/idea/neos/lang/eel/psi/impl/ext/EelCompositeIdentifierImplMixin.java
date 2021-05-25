package de.vette.idea.neos.lang.eel.psi.impl.ext;

import com.intellij.lang.ASTNode;
import de.vette.idea.neos.lang.eel.psi.EelCompositeIdentifier;
import de.vette.idea.neos.lang.eel.psi.impl.EelElementImpl;
import de.vette.idea.neos.lang.eel.resolve.ref.EelCompositeIdentifierReference;
import de.vette.idea.neos.lang.eel.resolve.ref.EelReference;
import org.jetbrains.annotations.NotNull;

public abstract class EelCompositeIdentifierImplMixin extends EelElementImpl implements EelCompositeIdentifier {

    public EelCompositeIdentifierImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    @Override
    public EelReference getReference() {
        return new EelCompositeIdentifierReference(this);
    }
}
