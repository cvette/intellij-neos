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

import com.intellij.psi.PsiFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IStubFileElementType;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class FusionFileStub extends PsiFileStubImpl<FusionFile> {

    public FusionFileStub(FusionFile file) {
        super(file);
    }

    public static IStubFileElementType TYPE = new IStubFileElementType<FusionFileStub>(FusionLanguage.INSTANCE) {

        @Override
        public int getStubVersion() {
            return 1;
        }

        @NotNull
        @Override
        public String getExternalId() {
            return "Fusion.file";
        }

        @NotNull
        @Override
        public FusionFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
            return new FusionFileStub(null);
        }

        @Override
        public StubBuilder getBuilder() {
            return new DefaultStubBuilder() {
                @NotNull
                @Override
                protected StubElement createStubForFile(@NotNull PsiFile file) {
                    return new FusionFileStub((FusionFile) file);
                }
            };
        }
    };
}