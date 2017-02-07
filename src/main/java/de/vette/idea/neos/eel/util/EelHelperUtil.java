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

package de.vette.idea.neos.eel.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.NeosProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.HashMap;

public class EelHelperUtil {

    public static HashMap<String, String> getHelpersInFile(@NotNull PsiFile psiFile) {
        YAMLKeyValue defaultContext = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, "TYPO3", "TypoScript", "defaultContext");

        HashMap<String, String> result = new HashMap<String, String>();
        if (defaultContext != null) {
            PsiElement mapping = defaultContext.getLastChild();
            if (mapping instanceof YAMLMapping) {
                for (PsiElement mappingElement : mapping.getChildren()) {
                    if (mappingElement instanceof YAMLKeyValue) {
                        YAMLKeyValue keyValue = (YAMLKeyValue) mappingElement;
                        result.put(keyValue.getKeyText(), keyValue.getValueText());
                        NeosProjectComponent.getLogger().info(keyValue.getKeyText() + ": " + keyValue.getValueText());
                    }
                }
            }
        }

        return result;
    }
}
