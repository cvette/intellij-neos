package de.vette.idea.neos.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.yaml.YAMLFileType;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import static com.intellij.openapi.project.ProjectUtil.guessProjectDir;

public class NeosUtil {
    public static final Pattern NODE_TYPES_PATH_PATTERN = Pattern.compile("(^|/)(NodeTypes/|Configuration/((Development|Production|Testing)/([^/]+/)*)?NodeTypes\\.)");
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

    /**
     * Gets the name of the prototype definition and the copied prototype
     * for a given PSI element.
     *
     * @param psi PsiElement
     * @return List
     */
    public static List<String> getPrototypeNames(PsiElement psi) {
        List<String> prototypes = new Vector<>();

        PsiElement parent = psi.getParent();
        while(!(parent instanceof FusionFile) && parent != null) {
            if (parent instanceof FusionPrototypeInstance) {
                prototypes.add(((FusionPrototypeInstance) parent).getType().getText());
                return prototypes;
            }

            if (parent instanceof FusionPropertyCopy) {
                FusionPropertyCopy copy = (FusionPropertyCopy) parent;
                FusionPath path = copy.getPath();

                if (path.isPrototypeSignature()) {
                    FusionPrototypeSignature signature = path.getPrototypeSignatureList().get(0);

                    if (signature.getType() != null) {
                        prototypes.add(signature.getType().getText());
                    }

                    if (copy.getCopiedPrototypeSignature() != null && copy.getCopiedPrototypeSignature().getType() != null) {
                        String type = copy.getCopiedPrototypeSignature().getType().getText();
                        prototypes.add(type);
                    }
                }
            }

            parent = parent.getParent();
        }

        return prototypes;
    }

    public static String getDefiningPrototypeName(FusionPropertyAssignment assignment) {
        PsiElement parentBlock = assignment.getParent();
        if (!(parentBlock instanceof FusionBlock)) {
            return null;
        }

        PsiElement parent = parentBlock.getParent();
        if (parent instanceof FusionPropertyCopy) {
            FusionPropertyCopy copy = (FusionPropertyCopy) parent;
            if (!copy.isPrototypeInheritance()) {
                return null;
            }

            FusionPrototypeSignature signature = copy.getPath().getPrototypeSignatureList().get(0);
            if (signature.getType() == null) {
                return null;
            }

            return signature.getType().getText();
        }

        return null;
    }
}
