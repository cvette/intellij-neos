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

package de.vette.idea.neos;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.plugins.newui.SearchQueryParser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.composer.ComposerConfigUtils;
import com.jetbrains.php.composer.ComposerDataService;
import com.jetbrains.php.composer.ComposerUtils;
import com.jetbrains.php.composer.InstalledPackageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static com.intellij.openapi.project.ProjectUtil.guessProjectDir;

public class NeosProjectService {
    final private static Logger LOG = Logger.getInstance("Neos");
    protected Project project;

    public NeosProjectService(Project project) {
        this.project = project;
    }

    public static NeosProjectService getInstance(@NotNull Project project) {
        return project.getService(NeosProjectService.class);
    }

    public static Logger getLogger() {
        return LOG;
    }

    /**
     * If plugin is not enabled on first project start/indexing we will never get a filled
     * index until a forced cache rebuild, we check also for vendor path
     */
    public static boolean isEnabledForIndex(Project project) {
        return (isEnabled(project) || isNeosProject(project));
    }

    public boolean isEnabled() {
        return NeosProjectService.isEnabled(this.project);
    }

    public static boolean isEnabled(@Nullable Project project) {
        return project != null && Settings.getInstance(project) != null && Settings.getInstance(project).pluginEnabled;
    }

    public static boolean isEnabled(@Nullable PsiElement element) {
        return element != null && isEnabled(element.getProject());
    }

    public boolean isNeosProject() {
        return NeosProjectService.isNeosProject(this.project);
    }

    public static boolean isNeosProject(Project project) {
        VirtualFile configFile = ComposerDataService.getInstance(project).getCurrentConfigFile();
        if (configFile == null) {
            return false;
        }

        try {
            JsonObject root = ComposerConfigUtils.parseJson(configFile).getAsJsonObject();

            if (!root.has("require")) {
                return false;
            }

            JsonObject requireObject = root.getAsJsonObject("require");
            return requireObject.has("neos/neos") || requireObject.has("neos/flow");
        } catch (IllegalStateException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
