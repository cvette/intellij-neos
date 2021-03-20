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

package de.vette.idea.neos.lang.fusion.structure;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import org.jetbrains.annotations.NotNull;

public class FusionStructureViewModel extends TextEditorBasedStructureViewModel implements StructureViewModel.ElementInfoProvider {
    private final FusionFile fusionFile;
    private final Sorter[] mySorters;

    public FusionStructureViewModel(Editor editor, PsiFile file) {
        super(editor, file);
        this.fusionFile = (FusionFile) file;
        this.mySorters = new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @NotNull
    @Override
    public StructureViewTreeElement getRoot() {
        return new FusionTreeElement(fusionFile);
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return false;
    }

    @NotNull
    @Override
    public Sorter @NotNull [] getSorters() {
        return this.mySorters;
    }
}
