import org.jetbrains.changelog.closure
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexer
import org.jetbrains.grammarkit.tasks.GenerateParser

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("idea")
    // Java support
    id("java")
    id("java-library")
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.7.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.1.2"
    // gradle-grammar-kit-plugin - read more: https://github.com/JetBrains/gradle-grammar-kit-plugin
    id("org.jetbrains.grammarkit") version "2021.1.2"
}

group = properties("pluginGroup")
version = properties("pluginVersion")


// Configure project's dependencies
repositories {
    mavenCentral()
}

idea {
    module {
        generatedSourceDirs.add(file("src/gen"))
    }
}

dependencies {
    implementation("io.sentry:sentry:4.3.0")
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")
    downloadSources = properties("platformDownloadSources").toBoolean()
    updateSinceUntilBuild = true

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    setPlugins(*properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version = properties("pluginVersion")
}

grammarKit {
    jflexRelease = properties("jFlexVersion")
    grammarKitRelease = properties("grammarKitVersion")
}

val generateEelLexer = task<GenerateLexer>("GenerateEelLexer") {
    source = "src/main/grammars/EelLexer.flex"
    targetDir = "src/gen/de/vette/idea/neos/lang/eel/parser"
    targetClass = "EelLexer"
    purgeOldFiles = true
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

val generateEelParser = task<GenerateParser>("GenerateEelParser") {
    source = "src/main/grammars/EelParser.bnf"
    targetRoot = "src/gen"
    pathToParser = "/de/vette/idea/neos/lang/eel/parser/EelParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

val generateFusionParser = task<GenerateParser>("GenerateFusionParser") {
    source = "src/main/grammars/FusionParser.bnf"
    targetRoot = "src/gen"
    pathToParser = "/de/vette/idea/neos/lang/fusion/parser/FusionParser.java"
    pathToPsiRoot = "/de/vette/idea/neos/lang/core/psi"
    purgeOldFiles = true
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = properties("javaVersion")
        targetCompatibility = properties("javaVersion")
        dependsOn(generateEelLexer, generateAfxLexer, generateFusionLexer, generateEelParser, generateFusionParser)
    }

    patchPluginXml {
        version(properties("pluginVersion"))
        sinceBuild(properties("pluginSinceBuild"))
        untilBuild(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription(
                closure {
                    File(projectDir, "README.md").readText().lines().run {
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
        ideVersions(properties("pluginVerifierIdeVersions"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://jetbrains.org/intellij/sdk/docs/tutorials/build_system/deployment.html#specifying-a-release-channel
        channels(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
    }
}