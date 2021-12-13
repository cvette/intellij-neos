package de.vette.idea.neos.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLFileType;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.intellij.openapi.project.ProjectUtil.guessProjectDir;

public class NeosUtil {
    public static final Pattern NODE_TYPES_PATH_PATTERN = Pattern.compile("(^|/)(NodeTypes/|Configuration/NodeTypes\\.)");
    public static final Pattern NODE_MIGRATION_PATH_PATTERN = Pattern.compile("(^|/)Migrations/(ContentRepository|TYPO3CR)/");

    /**
     * Check if the given virtual file is a node type definition
     */
    public static Boolean isNodeTypeDefinition(VirtualFile virtualFile)
    {
        if (virtualFile.getFileType() != YAMLFileType.YML) {
            return false;
        }

        return NODE_TYPES_PATH_PATTERN.matcher(virtualFile.getPath()).find();
    }

    /**
     * Check if the given virtual file is a node migration
     */
    public static Boolean isNodeMigration(VirtualFile virtualFile)
    {
        if (virtualFile.getFileType() != YAMLFileType.YML) {
            return false;
        }

        return NODE_MIGRATION_PATH_PATTERN.matcher(virtualFile.getPath()).find();
    }

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
