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

package de.vette.idea.neos.indexes;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.eel.util.EelHelperUtil;
import de.vette.idea.neos.indexes.externalizer.StringDataExternalizer;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Map;

public class DefaultContextFileIndex extends FileBasedIndexExtension<String, String> {
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    public static final ID<String, String> KEY = ID.create("de.vette.idea.neos.default_context");
    private static final StringDataExternalizer EXTERNALIZER = new StringDataExternalizer();
    private static final int MAX_FILE_BYTE_SIZE = 5242880;

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile -> virtualFile.getFileType() == YAMLFileType.YML;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @NotNull
    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return fileContent -> {
            Map<String, String> map = new THashMap<>();
            PsiFile psiFile = fileContent.getPsiFile();
            if(!NeosProjectService.isEnabledForIndex(psiFile.getProject())
                    || !isValidForIndex(fileContent, psiFile)) {
                return map;
            }

            return EelHelperUtil.getHelpersInFile(psiFile);
        };
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        if (inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return psiFile.getName().matches("Settings(\\..+)?\\.yaml");
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<String> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 2;
    }
}
