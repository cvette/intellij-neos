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
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionPropertyAssignmentImpl;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPropertyAssignmentIndex;
import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class FusionPropertyAssignmentStub extends StubBase<FusionPropertyAssignment> {
    protected String path;
    protected boolean isSimpleProperty;
    protected String containingPrototype;

    public FusionPropertyAssignmentStub(StubElement parent, IStubElementType<FusionPropertyAssignmentStub, FusionPropertyAssignment> elementType, String property, Boolean isSimpleProperty, String containingPrototype) {
        super(parent, elementType);
        this.path = property;
        this.isSimpleProperty = isSimpleProperty;
        this.containingPrototype = containingPrototype;
    }

    public static FusionStubElementType<FusionPropertyAssignmentStub, FusionPropertyAssignment> TYPE = new FusionStubElementType<>("FUSION_PROPERTY_ASSIGNMENT") {

        @Override
        public FusionPropertyAssignment createPsi(@NotNull FusionPropertyAssignmentStub stub) {
            return new FusionPropertyAssignmentImpl(stub, this);
        }

        @NotNull
        @Override
        public FusionPropertyAssignmentStub createStub(@NotNull FusionPropertyAssignment psi, StubElement parentStub) {
            List<String> prototypeNames = NeosUtil.getPrototypeNames(psi);
            String prototype = prototypeNames.isEmpty() ? "" : prototypeNames.get(0);
            return new FusionPropertyAssignmentStub(parentStub, this, psi.getPath().getText(), psi.isSimpleProperty(), prototype);
        }

        @Override
        public void serialize(@NotNull FusionPropertyAssignmentStub stub, @NotNull StubOutputStream dataStream) throws IOException {
            dataStream.writeName(stub.path);
            dataStream.writeBoolean(stub.isSimpleProperty);
            dataStream.writeName(stub.containingPrototype);
        }

        @NotNull
        @Override
        public FusionPropertyAssignmentStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
            String property = getNameAsString(dataStream.readName());
            Boolean isSimpleProperty = dataStream.readBoolean();
            String containingPrototype = getNameAsString(dataStream.readName());
            return new FusionPropertyAssignmentStub(parentStub, this, property, isSimpleProperty, containingPrototype);
        }

        @Override
        public void indexStub(@NotNull FusionPropertyAssignmentStub stub, @NotNull IndexSink sink) {
            if (stub.path == null || !stub.isSimpleProperty || stub.containingPrototype.equals("")) {
                return;
            }

            //TODO: Check nesting level

            sink.occurrence(FusionPropertyAssignmentIndex.KEY, stub.containingPrototype);
        }
    };
}
