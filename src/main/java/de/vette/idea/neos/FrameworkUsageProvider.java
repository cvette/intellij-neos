package de.vette.idea.neos;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.frameworks.PhpFrameworkUsageProvider;
import org.jetbrains.annotations.NotNull;

public class FrameworkUsageProvider implements PhpFrameworkUsageProvider {
    public FrameworkUsageProvider() {
    }

    @Override
    public @NotNull String getName() {
        return "Neos";
    }

    @Override
    public boolean isEnabled(@NotNull Project project) {
        return Settings.getInstance(project).pluginEnabled;
    }
}
