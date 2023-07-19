import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("idea")
    // Java support
    id("java")
    id("java-library")
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.14.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.1.0"
    // gradle-grammar-kit-plugin - read more: https://github.com/JetBrains/gradle-grammar-kit-plugin
    id("org.jetbrains.grammarkit") version "2022.3.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()


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
    implementation("io.sentry:sentry:6.26.0")
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(file(".qodana").canonicalPath)
    reportPath.set(file("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

grammarKit {
    jflexRelease.set(properties("jFlexVersion"))
    grammarKitRelease.set(properties("grammarKitVersion"))
}

val generateEelLexer = task<GenerateLexerTask>("GenerateEelLexer") {
    sourceFile.set(file("src/main/grammars/EelLexer.flex"))
    targetDir.set("src/gen/de/vette/idea/neos/lang/eel/parser")
    targetClass.set("EelLexer")
    purgeOldFiles.set(true)
}

val generateAfxLexer = task<GenerateLexerTask>("GenerateAfxLexer") {
    sourceFile.set(file("src/main/grammars/AfxLexer.flex"))
    targetDir.set("src/gen/de/vette/idea/neos/lang/afx/parser")
    targetClass.set("AfxLexer")
    purgeOldFiles.set(true)
}

val generateFusionLexer = task<GenerateLexerTask>("GenerateFusionLexer") {
    sourceFile.set(file("src/main/grammars/FusionLexer.flex"))
    targetDir.set("src/gen/de/vette/idea/neos/lang/fusion/parser")
    targetClass.set("FusionLexer")
    purgeOldFiles.set(true)
}

val generateEelParser = task<GenerateParserTask>("GenerateEelParser") {
    sourceFile.set(file("src/main/grammars/EelParser.bnf"))
    targetRoot.set("src/gen")
    pathToParser.set("/de/vette/idea/neos/lang/eel/parser/EelParser.java")
    pathToPsiRoot.set("/de/vette/idea/neos/lang/core/psi")
    purgeOldFiles.set(true)
}

val generateFusionParser = task<GenerateParserTask>("GenerateFusionParser") {
    sourceFile.set(file("src/main/grammars/FusionParser.bnf"))
    targetRoot.set("src/gen")
    pathToParser.set("/de/vette/idea/neos/lang/fusion/parser/FusionParser.java")
    pathToPsiRoot.set("/de/vette/idea/neos/lang/core/psi")
    purgeOldFiles.set(true)
}

tasks {
    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it.get()
            targetCompatibility = it.get()
            dependsOn(generateEelLexer, generateAfxLexer, generateFusionLexer, generateEelParser, generateFusionParser)
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
                projectDir.resolve("README.md").readText().lines().run {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                                .withHeader(false)
                                .withEmptySections(false),
                        Changelog.OutputType.HTML,
                )
            }
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(environment("CERTIFICATE_CHAIN"))
        privateKey.set(environment("PRIVATE_KEY"))
        password.set(environment("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(environment("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://jetbrains.org/intellij/sdk/docs/tutorials/build_system/deployment.html#specifying-a-release-channel
        channels.set(properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) })    }
}
