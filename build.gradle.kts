import org.jetbrains.changelog.closure
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser

plugins {
    idea
    // Java support
    java
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.7.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.1.1"
    // gradle-grammar-kit-plugin - read more: https://github.com/JetBrains/gradle-grammar-kit-plugin
    id("org.jetbrains.grammarkit") version "2020.3.2"
}

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project

val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

val grammarKitVersion: String by project
val jFlexVersion: String by project

val javaVersion: String by project

group = pluginGroup
version = pluginVersion


// Configure project's dependencies
repositories {
    mavenCentral()
    jcenter()
}

idea {
    module {
        generatedSourceDirs.add(file("src/gen"))
    }
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = pluginName_
    version = platformVersion
    type = platformType
    downloadSources = platformDownloadSources.toBoolean()
    updateSinceUntilBuild = true

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    setPlugins(*platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version = pluginVersion
}

grammarKit {
    jflexRelease = jFlexVersion
    grammarKitRelease = grammarKitVersion
}

val generateAfxLexer = task<GenerateLexer>("GenerateAfxLexer") {
    source = "src/main/grammars/AfxLexer.flex"
    targetDir = "src/gen/de/vette/idea/neos/lang/afx/parser"
    targetClass = "AfxLexer"
    purgeOldFiles = true
}

val generateFusionLexer = task<GenerateLexer>("GenerateFusionLexer") {
    source = "src/main/grammars/FusionLexer.flex"
    targetDir = "src/gen/de/vette/idea/neos/lang/fusion/parser"
    targetClass = "FusionLexer"
    purgeOldFiles = true
}

val generateFusionParser = task<GenerateParser>("GenerateParser") {
    source = "src/main/grammars/FusionParser.bnf"
    targetRoot = "src/gen"
    pathToParser = "/de/vette/idea/neos/lang/fusion/parser/FusionParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        dependsOn(generateAfxLexer, generateFusionLexer, generateFusionParser)
    }

    patchPluginXml {
        version(pluginVersion)
        sinceBuild(pluginSinceBuild)
        untilBuild(pluginUntilBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription(
                closure {
                    File("./README.md").readText().lines().run {
                        val start = "<!-- Plugin description -->"
                        val end = "<!-- Plugin description end -->"

                        if (!containsAll(listOf(start, end))) {
                            throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                        }
                        subList(indexOf(start) + 1, indexOf(end))
                    }.joinToString("\n").run { markdownToHTML(this) }
                }
        )

        // Get the latest available change notes from the changelog file
        changeNotes(
                closure {
                    changelog.getLatest().toHTML()
                }
        )
    }

    runPluginVerifier {
        ideVersions(pluginVerifierIdeVersions)
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://jetbrains.org/intellij/sdk/docs/tutorials/build_system/deployment.html#specifying-a-release-channel
        channels(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first())
    }
}