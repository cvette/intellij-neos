package de.vette.idea.neos.lang.eel.psi;

import com.intellij.psi.tree.IElementType;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;

public class EelElementType extends IElementType {
    public EelElementType(@NotNull String debugName) {
        super(debugName, EelLanguage.INSTANCE);
    }
}
