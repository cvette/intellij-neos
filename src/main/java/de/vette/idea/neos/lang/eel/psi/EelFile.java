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

package de.vette.idea.neos.lang.eel.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import de.vette.idea.neos.lang.eel.EelFileType;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;

public class EelFile extends PsiFileBase {

    public EelFile(@NotNull FileViewProvider fileViewProvider) {
        super(fileViewProvider, EelLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return EelFileType.INSTANCE;
    }
}
