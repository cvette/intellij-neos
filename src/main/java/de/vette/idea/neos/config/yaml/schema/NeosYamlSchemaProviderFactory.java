package de.vette.idea.neos.config.yaml.schema;

import com.intellij.openapi.project.Project;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NeosYamlSchemaProviderFactory implements JsonSchemaProviderFactory {
    @Override
    public @NotNull List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
        return List.of(
                new NodeTypeYamlSchemaProvider(project),
                new NodeMigrationYamlSchemaProvider(project),
                new CacheConfigYamlSchemaProvider(project)
        );
    }
}
