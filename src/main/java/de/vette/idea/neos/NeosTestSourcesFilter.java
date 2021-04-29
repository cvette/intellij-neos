package de.vette.idea.neos;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class NeosTestSourcesFilter extends TestSourcesFilter {

    @Override
    public boolean isTestSource(@NotNull VirtualFile file, @NotNull Project project) {
        return NeosProjectService.isEnabled(project) && (
                file.getPath().contains("/Tests/") || file.isDirectory() && file.getName().equals("Tests")
                        || ((file.getPath().contains("/Configuration/Testing/"))
                        || file.isDirectory()
                        && file.getName().equals("Testing")
                        && file.getParent().isDirectory()
                        && file.getParent().getName().equals("Configuration"))
        );
    }
}
