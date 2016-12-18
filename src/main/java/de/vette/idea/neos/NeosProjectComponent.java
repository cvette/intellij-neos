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

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.util.IdeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeosProjectComponent implements ProjectComponent {

    final private static Logger LOG = Logger.getInstance("Neos-Plugin");
    private Project project;

    public NeosProjectComponent(Project project) {
        this.project = project;
    }

    public static Logger getLogger() {
        return LOG;
    }

    public boolean isEnabled() {
        return Settings.getInstance(project).pluginEnabled;
    }

    /**
     * If plugin is not enabled on first project start/indexing we will never get a filled
     * index until a forced cache rebuild, we check also for vendor path
     */
    public static boolean isEnabledForIndex(Project project) {
        return (NeosProjectComponent.isEnabled(project) || NeosProjectComponent.isNeosProject(project));
    }

    public static boolean isEnabled(@Nullable Project project) {
        return project != null && Settings.getInstance(project).pluginEnabled;
    }

    @Override
    public void projectOpened() {
        if (!this.isEnabled() && !Settings.getInstance(project).dismissEnableNotification) {
            if (NeosProjectComponent.isNeosProject(this.project)) {
                IdeHelper.notifyEnableMessage(project);
            }
        }
    }

    @Override
    public void projectClosed() {

    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "NeosProjectComponent";
    }

    public static boolean isNeosProject(Project project) {
        return (VfsUtil.findRelativeFile(project.getBaseDir(), "Packages") != null
                && VfsUtil.findRelativeFile(project.getBaseDir(), "Configuration") != null
                && VfsUtil.findRelativeFile(project.getBaseDir(), "Packages", "Application", "TYPO3.Neos") != null);
    }
}
