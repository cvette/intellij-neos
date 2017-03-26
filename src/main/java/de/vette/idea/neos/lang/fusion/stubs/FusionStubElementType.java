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

package de.vette.idea.neos.lang.fusion.stubs;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.psi.FusionCompositeElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class FusionStubElementType<StubT extends StubElement<?>, PsiT extends FusionCompositeElement>
        extends IStubElementType<StubT, PsiT> {

    public FusionStubElementType(@NotNull @NonNls String debugName) {
        super(debugName, FusionLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "fusion." + super.toString();
    }

    public String getNameAsString(StringRef name) {
        if (name != null) {
            return name.getString();
        }

        return null;
    }
}
