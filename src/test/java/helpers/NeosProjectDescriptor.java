package helpers;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;

import de.vette.idea.neos.Settings;

import org.jetbrains.annotations.NotNull;

/**
 * Enable the Neos Project plugin
 */
public class NeosProjectDescriptor extends DefaultLightProjectDescriptor {
    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
        super.configureModule(module, model, contentEntry);
        Settings.getInstance(module.getProject()).pluginEnabled = true;
    }
}
