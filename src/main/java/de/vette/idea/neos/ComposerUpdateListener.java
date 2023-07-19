package de.vette.idea.neos;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.jetbrains.php.composer.ComposerDataService;
import com.jetbrains.php.composer.actions.update.ComposerInstalledPackagesService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ComposerUpdateListener implements ComposerInstalledPackagesService.ComposerUpdateListener {
    static final String PACKAGES_DIRECTORY = "Packages";

    public void packageRefreshed(@NotNull Project project) {
        if (!Settings.getInstance(project).excludePackageSymlinks) {
            return;
        }

        VirtualFile config = ComposerDataService.getInstance(project).getCurrentConfigFile();

        if (config == null) {
            return;
        }

        VirtualFile packagesDir = config.getParent().findChild(PACKAGES_DIRECTORY);
        if (packagesDir == null) {
            return;
        }

        VfsUtilCore.visitChildrenRecursively(packagesDir, new VirtualFileVisitor<>() {
            public boolean visitFile(@NotNull VirtualFile file) {
                // Limit to the first two levels
                if (file.getPath().split("/").length - packagesDir.getPath().split("/").length > 2) {
                    return false;
                }

                // Only process directories
                if (!file.isDirectory()) {
                    return false;
                }

                // We need to get the native file to check for symlinks
                java.io.File ioFile = VfsUtilCore.virtualToIoFile(file);
                Path path = Paths.get(ioFile.getPath());
                if (!Files.isSymbolicLink(path)) {
                    return true;
                }

                Module module = ModuleUtil.findModuleForFile(file, project);
                if (module == null) {
                    return false;
                }

                // Exclude directories
                String fileUrl = file.getUrl();
                Collection<String> excludeFolders = new ArrayList<>();
                excludeFolders.add(fileUrl);
                ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                for (VirtualFile sourceRoot : moduleRootManager.getContentRoots()) {
                    ModuleRootModificationUtil.updateExcludedFolders(
                            module,
                            sourceRoot,
                            Collections.emptyList(),
                            excludeFolders
                    );
                }

                return true;
            }
        });
    }
}
