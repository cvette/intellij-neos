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
package de.vette.idea.neos.lang.fusion.stubs.index;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import de.vette.idea.neos.lang.fusion.psi.FusionNamespaceDeclaration;
import org.jetbrains.annotations.NotNull;

public class FusionNamespaceDeclarationIndex extends StringStubIndexExtension<FusionNamespaceDeclaration> {

    public static StubIndexKey<String, FusionNamespaceDeclaration> KEY = StubIndexKey.createIndexKey("de.vette.idea.neos.lang.fusion.stubs.index.FusionNamespaceDeclarationIndex");

    @NotNull
    @Override
    public StubIndexKey<String, FusionNamespaceDeclaration> getKey() {
        return KEY;
    }
}