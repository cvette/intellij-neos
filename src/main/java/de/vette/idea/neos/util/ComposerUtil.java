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
package de.vette.idea.neos.util;

import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonValue;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ComposerUtil {

    @Nullable
    public static JsonFile getComposerManifest(PsiDirectory currentDirectory) {
        JsonFile composerFile;

        do {
            composerFile = getComposerManifestInDirectory(currentDirectory);
            currentDirectory = currentDirectory.getParentDirectory();
        } while (composerFile == null && currentDirectory != null && !currentDirectory.equals(currentDirectory.getProject().getBaseDir()) );

        return composerFile;
    }

    @Nullable
    public static JsonFile getComposerManifestInDirectory(@NotNull PsiDirectory packageDirContext) {
        for (PsiFile psiFile : packageDirContext.getFiles()) {
            if (psiFile instanceof JsonFile && psiFile.getName().equals("composer.json")) {
                return (JsonFile) psiFile;
            }
        }

        return null;
    }

    public static Map<String, String> getNamespaceMappings(JsonFile composerFile)
    {
        JsonObject object = (JsonObject) composerFile.getTopLevelValue();
        JsonProperty autoloadProperty = object.findProperty("autoload");
        Map<String,String> mappings = new HashMap<>();
        if (autoloadProperty != null) {
            JsonValue autoloadValue = autoloadProperty.getValue();
            if (autoloadValue instanceof JsonObject) {
                JsonObject autoloadObject = (JsonObject) autoloadValue;
                JsonProperty psr0 = autoloadObject.findProperty("psr-0");
                JsonProperty psr4 = autoloadObject.findProperty("psr-4");

                JsonObject psrObject = null;
                if (psr4 != null) {
                    psrObject = (JsonObject) psr4.getValue();
                } else if (psr0 != null) {
                    psrObject = (JsonObject) psr0.getValue();
                }

                if (psrObject != null) {
                    for (JsonProperty property : psrObject.getPropertyList()) {
                        mappings.put(property.getName(), property.getValue().getText());
                    }
                }
            }
        }

        return mappings;
    }
}