package de.vette.idea.neos.config.yaml.schema;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion;
import com.jetbrains.jsonSchema.remote.JsonFileResolver;
import de.vette.idea.neos.NeosProjectService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NodeMigrationYamlSchemaProvider implements JsonSchemaFileProvider {
    protected Project project;


    public NodeMigrationYamlSchemaProvider(Project project) {
        this.project = project;
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
        return !project.isDisposed()
                && NeosProjectService.isEnabled(project)
                && file.getParent() != null
                && file.getParent().isDirectory()
                && (file.getParent().getParent().getName().equals("Migrations")
                && file.getParent().getName().equals("ContentRepository") || file.getParent().getName().equals("TYPO3CR"));
    }

    @Override
    public @NotNull @Nls String getName() {
        return "Neos Node Migration";
    }

    @Override
    public @Nullable VirtualFile getSchemaFile() {
        return JsonFileResolver.urlToFile(Objects.requireNonNull(this.getRemoteSource()));
    }

    @Override
    public @NotNull SchemaType getSchemaType() {
        return SchemaType.remoteSchema;
    }

    @Override
    public JsonSchemaVersion getSchemaVersion() {
        return JsonSchemaVersion.SCHEMA_7;
    }

    @Override
    public @Nullable
    @NonNls String getRemoteSource() {
        return "https://raw.githubusercontent.com/Sebobo/Shel.Neos.Schema/main/NodeMigration.Schema.json";
    }
}
