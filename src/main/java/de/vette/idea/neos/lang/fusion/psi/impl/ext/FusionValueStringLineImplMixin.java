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

package de.vette.idea.neos.lang.fusion.psi.impl.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import de.vette.idea.neos.lang.fusion.psi.FusionValueStringLine;
import de.vette.idea.neos.lang.fusion.psi.FusionValueStringLineContent;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionElementImpl;
import de.vette.idea.neos.lang.fusion.resolve.ref.FusionReference;
import de.vette.idea.neos.lang.fusion.resolve.ref.FusionResourceStringReference;
import de.vette.idea.neos.lang.fusion.resolve.ref.FusionStringReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionValueStringLineImplMixin extends FusionElementImpl implements FusionValueStringLine {

    public FusionValueStringLineImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    @Nullable
    @Override
    public FusionValueStringLineContent getValueStringLineContent() {
        return PsiTreeUtil.getChildOfType(this, FusionValueStringLineContent.class);
    }

    @Override
    public FusionReference getReference() {
        if (getValueStringLineContent() != null) {
            if (getValueStringLineContent().getText().startsWith("resource://")) {
                return new FusionResourceStringReference(this);
            } else {
                return new FusionStringReference(this);
            }
        }

        return null;
    }
}
