package de.vette.idea.neos;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.frameworks.PhpFrameworkConfigurable;
import com.jetbrains.php.frameworks.PhpFrameworkConfigurableProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class FrameworkConfigurableProvider implements PhpFrameworkConfigurableProvider {
    public FrameworkConfigurableProvider() {
    }

    @Override
    public @Nls @NotNull String getName() {
        return "Neos";
    }

    @Override
    public @NotNull PhpFrameworkConfigurable createConfigurable(@NotNull Project project) {
        return new SettingsForm(project);
    }
}
