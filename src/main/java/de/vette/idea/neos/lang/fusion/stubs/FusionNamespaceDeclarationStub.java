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

import com.intellij.psi.stubs.*;
import de.vette.idea.neos.lang.fusion.psi.FusionNamespaceDeclaration;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionNamespaceDeclarationImpl;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionNamespaceDeclarationIndex;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class FusionNamespaceDeclarationStub extends StubBase<FusionNamespaceDeclaration> {

    protected String alias;
    protected String namespace;

    public FusionNamespaceDeclarationStub(StubElement parent, @NotNull IStubElementType elementType, @NotNull String alias, @NotNull String namespace) {
        super(parent, elementType);
        this.alias = alias;
        this.namespace = namespace;
    }

    public static FusionStubElementType TYPE = new FusionStubElementType<FusionNamespaceDeclarationStub , FusionNamespaceDeclaration>("FUSION_NAMESPACE_DECLARATION") {

        @Override
        public FusionNamespaceDeclaration createPsi(@NotNull FusionNamespaceDeclarationStub stub) {
            return new FusionNamespaceDeclarationImpl(stub, this);
        }

        @NotNull
        @Override
        public FusionNamespaceDeclarationStub createStub(@NotNull FusionNamespaceDeclaration psi, StubElement parentStub) {
            return new FusionNamespaceDeclarationStub(parentStub, this, psi.getAlias().getText(), psi.getNamespace().getText());
        }

        @Override
        public void serialize(@NotNull FusionNamespaceDeclarationStub stub, @NotNull StubOutputStream dataStream) throws IOException {
            dataStream.writeName(stub.alias);
            dataStream.writeName(stub.namespace);
        }

        @NotNull
        @Override
        public FusionNamespaceDeclarationStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
            String alias = getNameAsString(dataStream.readName());
            String namespace = getNameAsString(dataStream.readName());

            return new FusionNamespaceDeclarationStub(parentStub, this, alias, namespace);
        }

        @Override
        public void indexStub(@NotNull FusionNamespaceDeclarationStub stub, @NotNull IndexSink sink) {
            if (stub.alias != null) {
                sink.occurrence(FusionNamespaceDeclarationIndex.KEY, stub.alias);
            }
        }
    };
}
