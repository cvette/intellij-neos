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
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;

import de.vette.idea.neos.NeosProjectService;

import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Index which maps the Node Type name to the file name in NodeTypes.yaml. We do not store any "Value" in the index, as we are only interested in
 * which files have the NodeType defined.
 */
public class NodeTypesYamlFileIndex extends ScalarIndexExtension<String> {

    public static final ID<String, Void> KEY = ID.create("de.vette.idea.neos.NodeTypesYamlFileIndex");

    private final KeyDescriptor<String> keyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return NeosUtil::isNodeTypeDefinition;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return new NodeTypesYamlIndexer();
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return keyDescriptor;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    private static class NodeTypesYamlIndexer implements DataIndexer<String, Void, FileContent> {
        private static final int MAX_FILE_BYTE_SIZE = 5242880;

        @NotNull
        @Override
        public Map<String, Void> map(@NotNull FileContent fileContent) {
            PsiFile psiFile = fileContent.getPsiFile();

            if (NeosProjectService.isEnabledForIndex(psiFile.getProject()) && isValidForIndex(fileContent)) {
                return doIndex(fileContent);
            }

            return Collections.emptyMap();
        }

        @NotNull
        private Map<String, Void> doIndex(FileContent fileContent) {
            Map<String, Void> result = new HashMap<>();
            YAMLUtil.getTopLevelKeys((YAMLFile)fileContent.getPsiFile())
                    .forEach(yamlKeyValue -> result.put(yamlKeyValue.getKeyText(), null));
            return result;
        }

        private static boolean isValidForIndex(FileContent inputData) {
            return inputData.getFile().getLength() <= MAX_FILE_BYTE_SIZE;
        }
    }
}
