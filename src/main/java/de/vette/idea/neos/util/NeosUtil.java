package de.vette.idea.neos.util;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;

import static com.intellij.openapi.project.ProjectUtil.guessProjectDir;

public class NeosUtil {

    /**
     * Collect all parent directories until a directory named "Fusion" is found.
     * If none is found, return an empty array.
     */
    public static ArrayList<PsiDirectory> getParentFusionDirectories(PsiDirectory currentDirectory) {
        if (currentDirectory == null) {
            return null;
        }

        PsiFile composerManifest = ComposerUtil.getComposerManifest(currentDirectory);
        PsiDirectory packageDirectory = null;
        if (composerManifest != null) {
            packageDirectory = composerManifest.getContainingDirectory();
        }

        ArrayList<PsiDirectory> parentDirectories = new ArrayList<>();
        boolean fusionDirectoryExists = false;

        do {
            parentDirectories.add(currentDirectory);
            currentDirectory = currentDirectory.getParentDirectory();

            if (currentDirectory != null && currentDirectory.getName().equals("Fusion")) {
                fusionDirectoryExists = true;
            }
        } while (currentDirectory != null
                && !currentDirectory.getName().equals("Fusion")
                && !currentDirectory.equals(packageDirectory)
                && !currentDirectory.equals(guessProjectDir(currentDirectory.getProject())));

        if (!fusionDirectoryExists) {
            return new ArrayList<>();
        }

        return parentDirectories;
    }
}
