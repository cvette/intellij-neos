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

import com.intellij.json.psi.*;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.project.ProjectUtil.guessProjectDir;

public class ComposerUtil {

    @Nullable
    public static JsonFile getComposerManifest(PsiDirectory currentDirectory) {
        if (currentDirectory == null) {
            return null;
        }

        JsonFile composerFile;
        do {
            composerFile = getComposerManifestInDirectory(currentDirectory);
            currentDirectory = currentDirectory.getParentDirectory();
        } while (composerFile == null && currentDirectory != null && !currentDirectory.equals(guessProjectDir(currentDirectory.getProject())));

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

    /**
     * As defined in Neos\Flow\Package\PackageManager::getPackageKeyFromManifest
     */
    @Nullable
    public static String getPackageKey(JsonFile composerFile)
    {
        JsonObject object = (JsonObject) composerFile.getTopLevelValue();
        if (object == null) {
            return null;
        }

        // test extra.neos.package-key
        var packageKey = getFromPath(object, "extra", "neos", "package-key");
        if (packageKey instanceof JsonStringLiteral) {
            if (isPackageKeyValid(((JsonStringLiteral) packageKey).getValue())) {
                return ((JsonStringLiteral) packageKey).getValue();
            }
        }

        // get from path
        var packageType = getFromPath(object, "type");
        if (packageType instanceof JsonStringLiteral && isFlowPackageType(((JsonStringLiteral) packageType).getValue())) {
            var packageKeyFromDirectory = composerFile.getContainingDirectory().getName();
            if (packageKeyFromDirectory.contains(".") && isPackageKeyValid(packageKeyFromDirectory)) {
                return packageKeyFromDirectory;
            }
        }

        // test autoload configuration
        var namespaceMapping = getNamespaceMappings(composerFile);
        if (!namespaceMapping.isEmpty()) {
            var firstNamespacePath = namespaceMapping.keySet().stream().findFirst();
            var packageKeyFromNamespace = firstNamespacePath.get()
                .replace('\\', '.')
                .replaceAll("\\.*$", "");
            if (isPackageKeyValid(packageKeyFromNamespace)) {
                return packageKeyFromNamespace;
            }
        }

        // build from package-name
        var packageName = getFromPath(object, "name");
        if (packageName != null) {
            var packageKeyFromName = ((JsonStringLiteral) packageName).getValue().replace('/', '.');
            if (isPackageKeyValid(packageKeyFromName)) {
                return packageKeyFromName;
            }
        }

        return null;
    }

    /**
     * As defined in Neos\Flow\Composer\ComposerUtility::isFlowPackageType
     */
    public static boolean isFlowPackageType(String packageType)
    {
        return packageType.startsWith("typo3-flow-") || packageType.startsWith("neos-");
    }

    /**
     * As defined in Neos\Flow\Package\PackageManager::isPackageKeyValid and
     * Neos\Flow\Package\PackageInterface::PATTERN_MATCH_PACKAGEKEY
     */
    public static boolean isPackageKeyValid(String packageKey)
    {
        return packageKey.matches("^[a-zA-Z\\d]+\\.(?:[a-zA-Z\\d][.a-zA-Z\\d]*)+$");
    }

    /**
     * Retrieve the nested JsonValue at the path in an object, e.g. the object
     * {"foo": {"bar": {"baz": "string"}}}
     * with path ["foo", "bar", "baz"] will return a JsonValue for "string".
     * @return null if there was no object at some point in the path
     */
    @Nullable
    protected static JsonValue getFromPath(JsonObject object, String ...path)
    {
        JsonValue pointer = object;
        var i = 0;
        for (var key : path) {
            i++;
            if (!(pointer instanceof JsonObject)) {
                return null;
            }
            var property = ((JsonObject) pointer).findProperty(key);
            if (property == null) {
                return null;
            }
            pointer = property.getValue();
            if (i == path.length) {
                break;
            }
        }
        return pointer;
    }
}