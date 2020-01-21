package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider;
import com.intellij.json.psi.JsonFile;
import com.intellij.psi.PsiDirectory;
import de.vette.idea.neos.util.ComposerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class FusionTemplatePropertiesProvider implements DefaultTemplatePropertiesProvider {

    @Override
    public void fillProperties(@NotNull PsiDirectory directory, @NotNull Properties props) {
        JsonFile composerManifest = ComposerUtil.getComposerManifest(directory);
        if (composerManifest == null) {
            return;
        }

        PsiDirectory dir = composerManifest.getContainingDirectory();
        String dirName = dir.getName();

        props.setProperty("FUSION_PACKAGE_NAME", dirName);
    }
}
