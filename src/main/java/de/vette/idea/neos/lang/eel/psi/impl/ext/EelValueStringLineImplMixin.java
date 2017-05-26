/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.lang.eel.psi.impl.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine;
import de.vette.idea.neos.lang.eel.psi.EelValueStringLineContent;
import de.vette.idea.neos.lang.eel.psi.impl.EelNamedElementImpl;
import de.vette.idea.neos.lang.eel.resolve.ref.EelReference;
import de.vette.idea.neos.lang.eel.resolve.ref.EelResourceStringReference;
import de.vette.idea.neos.lang.eel.resolve.ref.EelStringReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EelValueStringLineImplMixin extends EelNamedElementImpl implements EelValueStringLine {

    public EelValueStringLineImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    @Nullable
    @Override
    public EelValueStringLineContent getValueStringLineContent() {
        return PsiTreeUtil.getChildOfType(this, EelValueStringLineContent.class);
    }

    @Override
    public EelReference getReference() {
        if (getValueStringLineContent() != null) {
            if (getValueStringLineContent().getText().startsWith("resource://")) {
                return new EelResourceStringReference(this);
            } else {
                return new EelStringReference(this);
            }
        }

        return null;
    }
}
