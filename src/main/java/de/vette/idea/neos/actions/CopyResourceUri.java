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

package de.vette.idea.neos.actions;

import com.intellij.ide.actions.CopyPathProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import de.vette.idea.neos.NeosIcons;
import de.vette.idea.neos.util.ComposerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class CopyResourceUri extends CopyPathProvider {

    @Override
    public void update(@NotNull AnActionEvent e)
    {
        super.update(e);
        e.getPresentation().setIcon(NeosIcons.NODE_TYPE);
    }

    @Nullable
    @Override
    public String getPathToElement(@NotNull Project project, @Nullable VirtualFile virtualFile, @Nullable Editor editor)
    {
        if (virtualFile == null) {
            return null;
        }
        var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return null;
        }

        var packageKey = getClosestPackageKey(psiFile);
        if (packageKey == null) {
            return null;
        }

        return getResourceUri(virtualFile, packageKey);
    }

    private String getResourceUri(VirtualFile file, String packageName)
    {
        var pathParts = file.getPath().split("Resources/", 2);
        if (pathParts.length < 2) {
            return String.format("resource://%s/", packageName);
        }
        return String.format("resource://%s/%s", packageName, pathParts[1]);
    }

    @Nullable
    private String getClosestPackageKey(PsiFile file)
    {
        var parent = file.getParent();
        String packageName = null;
        var contentRoots = Arrays.asList(ProjectRootManager.getInstance(file.getProject()).getContentRoots());

        var resourcesDirectoryFound = false;
        while (parent != null && !contentRoots.contains(parent.getVirtualFile())) {
            if (parent.getName().equals("Resources")) {
                resourcesDirectoryFound = true;
            } else if (resourcesDirectoryFound) {
                var composerManifest = ComposerUtil.getComposerManifest(parent);
                if (composerManifest != null && composerManifest.isValid()) {
                    packageName = ComposerUtil.getPackageKey(composerManifest);
                    break;
                }
            }
            parent = parent.getParent();
        }

        if (!resourcesDirectoryFound) {
            return null;
        }
        return packageName;
    }
}
