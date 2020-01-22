package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider;
import com.intellij.json.psi.JsonFile;
import com.intellij.psi.PsiDirectory;
import de.vette.idea.neos.util.ComposerUtil;
import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public class FusionTemplatePropertiesProvider implements DefaultTemplatePropertiesProvider {
    public static final String NEOS_PACKAGE_NAME = "NEOS_PACKAGE_NAME";
    public static final String FUSION_PROTOTYPE_PATH = "FUSION_PROTOTYPE_PATH";
    public static final String FUSION_PROTOTYPE_NAME = "FUSION_PROTOTYPE_NAME";

    @Override
    public void fillProperties(@NotNull PsiDirectory directory, @NotNull Properties props) {
        JsonFile composerManifest = ComposerUtil.getComposerManifest(directory);
        if (composerManifest == null) {
            return;
        }

        ArrayList<PsiDirectory> directories = NeosUtil.getParentFusionDirectories(directory);
        ArrayList<String> directoryNames = new ArrayList<>();

        for (PsiDirectory dir: directories) {
            directoryNames.add(dir.getName());
        }

        Collections.reverse(directoryNames);
        String fusionPrototypePath = String.join(".", directoryNames);

        PsiDirectory packageDir = composerManifest.getContainingDirectory();

        props.setProperty(FUSION_PROTOTYPE_PATH, fusionPrototypePath);
        props.setProperty(NEOS_PACKAGE_NAME, packageDir.getName());
    }
}
