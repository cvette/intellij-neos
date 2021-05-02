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

package de.vette.idea.neos.lang.eel.stubs;

import com.intellij.psi.PsiFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.DefaultStubBuilder;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.tree.IStubFileElementType;
import de.vette.idea.neos.lang.eel.EelLanguage;
import de.vette.idea.neos.lang.eel.psi.EelFile;
import org.jetbrains.annotations.NotNull;

public class EelFileStub extends PsiFileStubImpl<EelFile> {

    public EelFileStub(EelFile file) {
        super(file);
    }

    public static IStubFileElementType<EelFileStub> TYPE = new IStubFileElementType<>(EelLanguage.INSTANCE) {

        @Override
        public int getStubVersion() {
            return 1;
        }

        @NotNull
        @Override
        public String getExternalId() {
            return "Eel.file";
        }

        @NotNull
        @Override
        public EelFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) {
            return new EelFileStub(null);
        }

        @Override
        public StubBuilder getBuilder() {
            return new DefaultStubBuilder() {
                @Override
                protected @NotNull EelFileStub createStubForFile(@NotNull PsiFile file) {
                    return new EelFileStub((EelFile) file);
                }
            };
        }
    };
}